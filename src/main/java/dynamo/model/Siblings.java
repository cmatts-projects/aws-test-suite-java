package dynamo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Siblings {
    private List<Person> fullSiblings;
    private List<Person> stepByFather;
    private List<Person> stepByMother;
    private List<Person> parents;
}
