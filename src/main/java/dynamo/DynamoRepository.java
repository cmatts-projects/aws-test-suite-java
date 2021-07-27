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

        Set<Person> allSiblings = new HashSet<>();
        allSiblings.addAll(findPersonByFather(person.getFatherId()));
        allSiblings.addAll(findPersonByMother(person.getMotherId()));

        return Siblings.builder()
                .fullSiblings(extractFullSiblings(person, allSiblings))
                .stepByFather(extractStepSiblingsByFather(person, allSiblings))
                .stepByMother(extractStepSiblingsByMother(person, allSiblings))
                .parents(extractParents(allSiblings))
                .build();
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

    private List<Person> extractStepSiblingsByMother(Person person, Set<Person> allSiblings) {
        return allSiblings.stream()
                .filter(s -> !Objects.equals(s.getFatherId(), person.getFatherId()) &&
                        Objects.equals(s.getMotherId(), person.getMotherId()))
                .sorted((a, b) -> sortByParentYear(a, b, a.getFatherId(), b.getFatherId()))
                .collect(toList());
    }

    private List<Person> extractStepSiblingsByFather(Person person, Set<Person> allSiblings) {
        return allSiblings.stream()
                .filter(s -> Objects.equals(s.getFatherId(), person.getFatherId()) &&
                        !Objects.equals(s.getMotherId(), person.getMotherId()))
                .sorted((a, b) -> sortByParentYear(a, b, a.getMotherId(), b.getMotherId()))
                .collect(toList());
    }

    private List<Person> extractFullSiblings(Person person, Set<Person> allSiblings) {
        return allSiblings.stream()
                .filter(s -> Objects.equals(s.getFatherId(), person.getFatherId()) &&
                        Objects.equals(s.getMotherId(), person.getMotherId()))
                .sorted((a, b) -> (a.getYearOfBirth() > b.getYearOfBirth() ||
                        (Objects.equals(a.getYearOfBirth(), b.getYearOfBirth()) && a.getId() > b.getId())) ? 1 : -1)
                .collect(toList());
    }

    private int sortByParentYear(Person a, Person b, Integer aParentId, Integer bParentId) {
        return
            (Objects.equals(aParentId, bParentId) &&
                (b.getYearOfBirth() == null ||
                    (a.getYearOfBirth() != null && (a.getYearOfBirth() > b.getYearOfBirth() ||
                        (Objects.equals(a.getYearOfBirth(), b.getYearOfBirth()) && a.getId() > b.getId())))))
            ||
            (!Objects.equals(aParentId, bParentId) &&
                ((aParentId != null && bParentId == null) || (aParentId != null && aParentId > bParentId)))
            ? 1 : -1;
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
