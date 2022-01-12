package az.kapitalbank.marketplace.client.optimus.model.process;

import lombok.Data;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(level = PRIVATE)
public class MainIncome {
    boolean salaryProject;
    String companyName;
    Salary salary;
    String cardType;
}
