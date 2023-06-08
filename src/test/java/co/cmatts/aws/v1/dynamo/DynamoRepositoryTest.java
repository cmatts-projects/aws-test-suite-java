package co.cmatts.aws.v1.dynamo;

import co.cmatts.aws.v1.cloudformation.CloudFormation;
import co.cmatts.aws.v1.dynamo.model.Fact;
import co.cmatts.aws.v1.dynamo.model.Person;
import co.cmatts.aws.v1.dynamo.model.Siblings;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.List;
import java.util.Optional;

import static co.cmatts.aws.v1.dynamo.DynamoDbTestDataFactory.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDFORMATION;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

@Testcontainers
@ExtendWith(SystemStubsExtension.class)
class DynamoRepositoryTest {

    private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack").withTag("0.12.15");
    private static final String DYNAMO_TABLES_YML = "dynamo-tables.yml";

    @SystemStub
    private static EnvironmentVariables environmentVariables;

    @Container
    private static final LocalStackContainer LOCAL_STACK_CONTAINER = new LocalStackContainer(IMAGE)
            .withServices(DYNAMODB, CLOUDFORMATION);

    private static DynamoRepository repo;

    @BeforeAll
    static void beforeAll() throws Exception {
        environmentVariables
            .set("AWS_ACCESS_KEY", LOCAL_STACK_CONTAINER.getAccessKey())
            .set("AWS_SECRET_KEY", LOCAL_STACK_CONTAINER.getSecretKey())
            .set("LOCAL_STACK_ENDPOINT", LOCAL_STACK_CONTAINER.getEndpointOverride(null).toString())
            .set("AWS_REGION", LOCAL_STACK_CONTAINER.getRegion());

        CloudFormation.createStack("DynamoDB", DYNAMO_TABLES_YML);

        repo = new DynamoRepository();
        repo.load(peopleDataList(), factDataList());
    }

    @Test
    void shouldFindPerson() {
        Optional<Person> result = repo.findPerson(1);
        assertThat(result.isPresent()).isTrue();

        Person p = result.get();
        assertThat(p.getId()).isEqualTo(1);
        assertThat(p.getYearOfBirth()).isEqualTo(1900);
        assertThat(p.getYearOfDeath()).isEqualTo(1990);
        assertThat(p.getFatherId()).isEqualTo(16);
        assertThat(p.getMotherId()).isEqualTo(17);
        assertThat(p.toString())
                .isEqualTo(person(1).toString());
    }
    
    @Test
	void shouldNotFindPerson() {
        Optional<Person> result = repo.findPerson(99);
        assertThat(result.isPresent()).isFalse();
    }

    @Test
	void shouldFindFactsForPerson() {
        List<Fact> facts = repo.findFacts(1);
        assertThat(facts).hasSize(3);
        assertThat(facts).containsExactlyInAnyOrder(fact(1), fact(2), fact(3));
        assertThat(facts.stream().filter(f -> f.getId() == 2).findAny().get().toString())
                .isEqualTo(fact(2).toString());
    }

    @Test
	void shouldNotFindFacts() {
        List<Fact> facts = repo.findFacts(99);
        assertThat(facts).hasSize(0);
    }

    @Test
	void shouldFindAllSiblingsGroupedByParentsAndInOrderOfYearOfBirth() {
        Siblings siblings = repo.findSiblings(1);
        assertThat(siblings).isEqualTo(PERSON_1_SIBLINGS);
    }

    @Test
	void shouldFindSiblingsWithNoMotherAssumingTheSameMother() {
        Siblings siblings = repo.findSiblings(3);

        assertThat(siblings).isEqualTo(PERSON_3_SIBLINGS);
    }

    @Test
	void shouldFindSiblingsWithNoFatherAssumingTheSameFather() {
        Siblings siblings = repo.findSiblings(8);

        assertThat(siblings).isEqualTo(PERSON_8_SIBLINGS);
    }

    @Test
	void shouldFindAllPeopleSorted() {
        List<Person> people = repo.findPeople();
        assertThat(people).hasSize(peopleCount());
        assertThat(people.get(0).getName()).isEqualTo("First Person");
        assertThat(people.get(1).getName()).isEqualTo("Mr Test");
    }

    @Test
    void shouldUpdateEntities() {
        Person person = repo.findPerson(21).get();
        person.setYearOfBirth(1799);
        person.setYearOfDeath(1888);

        Fact fact = repo.findFacts(21).get(0);
        fact.setImage("Updated");
        fact.setDescription("A changed description");

        repo.updateEntities(asList(person, fact));

        Person updatedPerson = repo.findPerson(21).get();
        Fact updatedFact = repo.findFacts(21).get(0);

        assertThat(updatedPerson.toString()).isEqualTo(person.toString());
        assertThat(updatedFact.toString()).isEqualTo(fact.toString());
    }

    @Test
    void shouldHandleOptimisticLockingOfEntities() {
        Person person = repo.findPerson(21).get();
        person.setYearOfBirth(1699);

        Fact fact = repo.findFacts(21).get(0);
        fact.setDescription("This has an older version");
        fact.setVersion(fact.getVersion() - 1);

        assertThatThrownBy(() -> repo.updateEntities(asList(person, fact)))
            .isInstanceOf(TransactionCanceledException.class);

        Person updatedPerson = repo.findPerson(21).get();
        Fact updatedFact = repo.findFacts(21).get(0);

        assertThat(updatedPerson.toString()).isNotEqualTo(person.toString());
        assertThat(updatedFact.toString()).isNotEqualTo(fact.toString());
    }
}
