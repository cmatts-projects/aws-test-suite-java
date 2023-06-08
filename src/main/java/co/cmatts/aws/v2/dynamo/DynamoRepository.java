package co.cmatts.aws.v2.dynamo;

import co.cmatts.aws.v2.dynamo.model.DynamoDbMappedBean;
import co.cmatts.aws.v2.dynamo.model.Fact;
import co.cmatts.aws.v2.dynamo.model.Person;
import co.cmatts.aws.v2.dynamo.model.Siblings;
import org.apache.commons.collections4.ListUtils;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static co.cmatts.aws.v2.dynamo.Dynamo.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

public class DynamoRepository {

    private static final String FATHER_INDEX = "fatherIndex";
    private static final String MOTHER_INDEX = "motherIndex";
    private static final String PERSON_INDEX = "personIndex";
    private static final int BATCH_SIZE = 25;

    public Optional<Person> findPerson(Integer id) {
        if (id == null) {
            return Optional.empty();
        }

        Key idKey = Key.builder().partitionValue(id).build();
        CompletableFuture<Person> result = getDynamoTable(Person.class).getItem(r -> r.key(idKey));

        return Optional.ofNullable(result.join());
    }

    public List<Person> findPersonByFather(Integer id) {
        return findEntitiesByIndex(id, Person.class, FATHER_INDEX);
    }

    public List<Person> findPersonByMother(Integer id) {
        return findEntitiesByIndex(id, Person.class, MOTHER_INDEX);
    }

    public List<Fact> findFacts(Integer id) {
        return findEntitiesByIndex(id, Fact.class, PERSON_INDEX);
    }

    private <T extends DynamoDbMappedBean> List<T> findEntitiesByIndex(Integer id, Class<T> beanClass, String index) {
        if (id == null) {
            return emptyList();
        }

        DynamoDbAsyncIndex<T> parentIndex = getDynamoTable(beanClass).index(index);

        SdkPublisher<Page<T>> publisher =
                parentIndex.query(r -> r.consistentRead(false).queryConditional(keyEqualTo(k -> k.partitionValue(id))));

        return collectFromPublisher(publisher);
    }

    private <T> List<T> collectFromPublisher(SdkPublisher<Page<T>> publisher) {
        CollectingSubscriber<Page<T>> subscriber = new CollectingSubscriber<>();
        publisher.subscribe(subscriber);
        subscriber.waitForCompletion();
        if (!isNull(subscriber.error())) {
            throw new IllegalStateException("DynamoDb query failed with an error", subscriber.error());
        }
        if (!subscriber.isCompleted()) {
            throw new IllegalStateException("DynamoDb query failed to get all content");
        }

        return subscriber.collectedItems().stream()
                .map(Page::items)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public Siblings findSiblings(Integer id) {
        Optional<Person> p = findPerson(id);
        if (p.isEmpty()) {
            return new Siblings();
        }
        Person person = p.get();

        Set<Person> allSiblings = Stream
                .concat(findPersonByFather(person.getFatherId()).stream(),
                        findPersonByMother(person.getMotherId()).stream())
                .collect(Collectors.toSet());

        return new Siblings(person, allSiblings, extractParents(allSiblings));
    }

    private List<Person> extractParents(Set<Person> allSiblings) {
        return allSiblings.stream()
                .map(s -> asList(s.getFatherId(), s.getMotherId()))
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .distinct()
                .map(this::findPerson)
                .map(Optional::get)
                .sorted(Comparator.comparing(Person::getId))
                .collect(toList());
    }

    public List<Person> findPeople() {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                .consistentRead(true)
                .build();

        PagePublisher<Person> publisher = getDynamoTable(Person.class).scan(request);

        return collectFromPublisher(publisher).stream().sorted(Comparator.comparing(Person::getName)).collect(toList());
    }

    @SafeVarargs
    public final void load(List<? extends DynamoDbMappedBean>... dataLists) {
        List<DynamoDbMappedBean> allData = Stream.of(dataLists).flatMap(List::stream).collect(toList());
        ListUtils.partition(allData, BATCH_SIZE).forEach(this::loadBatch);
    }

    private void loadBatch(List<DynamoDbMappedBean> data) {
        Map<Class<DynamoDbMappedBean>, List<DynamoDbMappedBean>> entities = data
                .stream()
                .collect(groupingBy(g -> (Class<DynamoDbMappedBean>) g.getClass()));
        List<WriteBatch> batchWrites = entities
                .entrySet()
                .stream()
                .map(e -> loadBatchForEntity(e.getValue(), e.getKey())).collect(toList());

        CompletableFuture<BatchWriteResult> asyncResult = getEnhancedUnversionedDynamoClient()
                .batchWriteItem(b -> b.writeBatches(batchWrites));

        BatchWriteResult result = asyncResult.join();

        entities.keySet().forEach(beanClass -> validateBatchWrite(result, beanClass));
    }

    private <T extends DynamoDbMappedBean> void validateBatchWrite(BatchWriteResult result, Class<T> beanClass) {
        DynamoDbAsyncTable<T> table = getDynamoTable(getEnhancedUnversionedDynamoClient(), beanClass);
        if (result.unprocessedPutItemsForTable(table).size() > 0) {
            throw new IllegalStateException("DynamoDb query failed to write all content");
        }
    }

    private <T extends DynamoDbMappedBean> WriteBatch loadBatchForEntity(List<T> entities, Class<T> beanClass) {
        DynamoDbAsyncTable<T> table = getDynamoTable(getEnhancedUnversionedDynamoClient(), beanClass);

        WriteBatch.Builder<T> batch = WriteBatch.builder(beanClass).mappedTableResource(table);
        entities.forEach(e -> batch.addPutItem((T) e));
        return batch.build();
    }

    public void updateEntities(List<? extends DynamoDbMappedBean> entities) {
        CompletableFuture<Void> update = getEnhancedDynamoClient().transactWriteItems(t -> entities
                .forEach(e -> addUpdateForEntity(t, e)));
        update.join();
        entities.forEach(e -> e.setVersion(e.getVersion() + 1));
    }

    private <T extends DynamoDbMappedBean> void addUpdateForEntity(TransactWriteItemsEnhancedRequest.Builder builder, T entity) {
        Class<T> beanClass = (Class<T>) entity.getClass();
        UpdateItemEnhancedRequest<T> updateRequest = UpdateItemEnhancedRequest.builder(beanClass).item(entity).build();
        builder.addUpdateItem(getDynamoTable(beanClass), updateRequest);
    }
}
