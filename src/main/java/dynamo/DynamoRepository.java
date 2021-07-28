package dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.TransactionWriteRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import dynamo.model.Fact;
import dynamo.model.Person;
import dynamo.model.Siblings;
import org.apache.commons.collections4.ListUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dynamo.DynamoClient.getDynamoMapper;
import static dynamo.DynamoClient.getDynamoMapperConfig;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class DynamoRepository {
    public Optional<Person> findPerson(Integer id) {
        if (id == null) {
            return Optional.empty();
        }

        DynamoDBQueryExpression<Person> queryExpression = new DynamoDBQueryExpression<Person>()
                .withConsistentRead(true)
                .withKeyConditionExpression("id = :id")
                .withExpressionAttributeValues(Map.of(
                        ":id", new AttributeValue().withN(Integer.toString(id))
                ));

        List<Person> results = getDynamoMapper().query(Person.class, queryExpression, getDynamoMapperConfig());
        return results.stream().findFirst();
    }

    public List<Person> findPersonByFather(Integer id) {
        if (id == null) {
            return emptyList();
        }

        DynamoDBQueryExpression<Person> queryExpression = new DynamoDBQueryExpression<Person>()
                .withConsistentRead(false)
                .withIndexName("fatherIndex")
                .withKeyConditionExpression("fatherId = :id")
                .withExpressionAttributeValues(Map.of(
                        ":id", new AttributeValue().withN(Integer.toString(id))
                ));

        return getDynamoMapper().query(Person.class, queryExpression, getDynamoMapperConfig());
    }

    public List<Person> findPersonByMother(Integer id) {
        if (id == null) {
            return emptyList();
        }

        DynamoDBQueryExpression<Person> queryExpression = new DynamoDBQueryExpression<Person>()
                .withConsistentRead(false)
                .withIndexName("motherIndex")
                .withKeyConditionExpression("motherId = :id")
                .withExpressionAttributeValues(Map.of(
                        ":id", new AttributeValue().withN(Integer.toString(id))
                ));

        return getDynamoMapper().query(Person.class, queryExpression, getDynamoMapperConfig());
    }

    public List<Fact> findFacts(Integer id) {
        if (id == null) {
            return emptyList();
        }

        DynamoDBQueryExpression<Fact> queryExpression = new DynamoDBQueryExpression<Fact>()
                .withIndexName("personIndex")
                .withConsistentRead(false)
                .withKeyConditionExpression("personId = :id")
                .withExpressionAttributeValues(Map.of(
                        ":id", new AttributeValue().withN(Integer.toString(id))
                ));

        return getDynamoMapper().query(Fact.class, queryExpression, getDynamoMapperConfig());
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
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();

        return getDynamoMapper().scan(Person.class, scanExpression, getDynamoMapperConfig())
                .stream()
                .sorted(Comparator.comparing(Person::getName))
                .collect(toList());
    }

    public void load(List<Person> peopleDataList, List<Fact> factDataList) {
        List<Object> allData = Stream.concat(peopleDataList.stream(), factDataList.stream())
                .collect(toList());

        ListUtils.partition(allData, 25)
                .forEach(this::addWithTransaction);
    }

    private void addWithTransaction(List<Object> batch) {
        DynamoDBMapper mapper = DynamoClient.getDynamoMapper();
        TransactionWriteRequest transactionWriteRequest = new TransactionWriteRequest();
        batch.forEach(transactionWriteRequest::addPut);
        mapper.transactionWrite(transactionWriteRequest, DynamoClient.getDynamoMapperConfig());
    }
}
