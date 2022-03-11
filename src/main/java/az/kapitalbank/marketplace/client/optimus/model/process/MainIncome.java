package az.kapitalbank.marketplace.client.optimus.model.process;

import static lombok.AccessLevel.PRIVATE;

import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = PRIVATE)
public class MainIncome {
    boolean salaryProject;
    String companyName;
    Salary salary;
    String cardType;
}
