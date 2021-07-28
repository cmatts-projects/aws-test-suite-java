package dynamo;

import dynamo.model.Fact;
import dynamo.model.Person;
import dynamo.model.Siblings;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class DynamoDbTestDataFactory {

    private static final Object[][] PEOPLE_DATA = {
                {"Mr Test",16,17,1900,1990},
                {"Mr Test1",16,17,1890,1990},
                {"Mr Test2",16,null,1900,1990},
                {"Mr Test3",16,20,1900,1990},
                {"Mr Test4",16,21,1900,1990},
                {"Mr Test5",16,21,1890,1990},
                {"Mr Test6",16,null,1900,1990},
                {"Mr Test10",null,17,1900,1990},
                {"Mr Test11",18,17,1900,1990},
                {"Mr Test12",19,17,1900,1990},
                {"Mr Test13",19,17,1890,1990},
                {"Mr Test14",null,17,1900,1990},
                {"Mr Test15",14,15,1900,1990},
                {"Mr Test16",null,null,1880,null},
                {"Mr Test17",null,null,null,1920},
                {"Mr Test20",null,null,null,null},
                {"Mr Test21",null,null,null,null},
                {"Mr Test22",null,null,null,null},
                {"Mr Test23",null,null,null,null},
                {"Mr Test24",null,null,null,null},
                {"Mr Test25",null,null,null,null},
                {"First Person",null,null,null,null}
            };

    private static final Object[][] FACT_DATA = {
                {1, 1901, "resource2", null, "fact1"},
                {1, 1902, "resource1", null, "fact2"},
                {1, 1892, null, "source1", "fact3"},
                {2, 1852, "resource3", null, "fact4"},
                {3, 1872, "resource4", null, "fact5"},
                {21, 1872, "Original", null, "some facts"}
            };

    public static final Siblings PERSON_1_SIBLINGS = Siblings.builder()
                .fullSiblings(asList(person(2), person(1)))
                .stepByFather(asList(person(3), person(7), person(4), person(6), person(5)))
                .stepByMother(asList(person(8), person(12), person(9), person(11), person(10)))
                .parents(asList(person(16), person(17), person(18),person(19), person(20), person(21)))
                .build();

    public static final Siblings PERSON_3_SIBLINGS = Siblings.builder()
                .fullSiblings(asList(person(3), person(7)))
                .stepByFather(asList(person(2), person(1), person(4), person(6), person(5)))
                .stepByMother(emptyList())
                .parents(asList(person(16), person(17), person(20), person(21)))
                .build();

    public static final Siblings PERSON_8_SIBLINGS = Siblings.builder()
                .fullSiblings(asList(person(8), person(12)))
                .stepByFather(emptyList())
                .stepByMother(asList(person(2), person(1), person(9), person(11), person(10)))
                .parents(asList(person(16), person(17), person(18), person(19)))
                .build();

    public static List<Person> peopleDataList() {
        return IntStream.range(0, PEOPLE_DATA.length)
                .mapToObj(i -> person(i + 1))
                .collect(Collectors.toList());
    }

    public static Person person(int i) {
        int index = i - 1;
        return Person.builder()
                .id(i)
                .name((String) PEOPLE_DATA[index][0])
                .fatherId((Integer) PEOPLE_DATA[index][1])
                .motherId((Integer) PEOPLE_DATA[index][2])
                .yearOfBirth((Integer) PEOPLE_DATA[index][3])
                .yearOfDeath((Integer) PEOPLE_DATA[index][4])
                .version(1L)
                .build();
    }

    public static List<Fact> factDataList() {
        return IntStream.range(0, FACT_DATA.length)
                .mapToObj(i -> fact(i + 1))
                .collect(Collectors.toList());
    }

    public static Fact fact(int i) {
        int index = i - 1;
        return Fact.builder()
                .id(i)
                .personId((Integer) FACT_DATA[index][0])
                .year((Integer) FACT_DATA[index][1])
                .image((String) FACT_DATA[index][2])
                .source((String) FACT_DATA[index][3])
                .description((String) FACT_DATA[index][4])
                .version(1L)
                .build();
    }

    public static int peopleCount() {
        return PEOPLE_DATA.length;
    }

    public static int factCount() {
        return FACT_DATA.length;
    }
}
