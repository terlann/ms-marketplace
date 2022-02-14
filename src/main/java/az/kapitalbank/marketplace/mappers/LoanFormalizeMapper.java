package az.kapitalbank.marketplace.mappers;

import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderRequest;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.constants.DvsConstants;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.mappers.qualifier.LoanFormalizationQualifier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = LoanFormalizationQualifier.class)
public interface LoanFormalizeMapper {

    @Mapping(source = "customerEntity.fullName", target = "fullName")
    @Mapping(source = "customerEntity.email", target = "email")
    @Mapping(source = "mobileNumber", target = "mobile")
    @Mapping(source = "pin", target = "pin")
    @Mapping(source = "processResponse.variables.cif", target = "cif")
    @Mapping(source = "processResponse.variables.cashCreditContractNumber", target = "cashContract")
    @Mapping(source = "processResponse.taskId", target = "processId")
    @Mapping(constant = DvsConstants.SALES_CHANNEL, target = "salesChannel")
    @Mapping(constant = DvsConstants.SALES_SOURCE, target = "salesSource")
    @Mapping(constant = DvsConstants.BRANCH_CODE, target = "branchCode")
    @Mapping(constant = DvsConstants.PRODUCT, target = "product")
    @Mapping(constant = DvsConstants.AGENT, target = "agent")
    @Mapping(constant = "false", target = "confirmed")
    @Mapping(source = "processResponse.variables.period", target = "tenure")
    @Mapping(source = "processResponse.variables.creditDocumentsInfo.path", target = "documents",
            qualifiedByName = "asList")
    @Mapping(source = "processResponse.variables.selectedOffer.cashOffer.interestRate", target = "loanPercentage",
            qualifiedByName = "toString")
    @Mapping(source = "processResponse.processCreateTime", target = "creditCreatedAt",
            qualifiedByName = "toLocalDateTime")
    @Mapping(constant = "processResponse.variables.selectedOffer.cashOffer.availableLoanAmount", target = "cashAmount")
    DvsCreateOrderRequest toDvsCreateOrderRequest(CustomerEntity customerEntity, ProcessResponse processResponse,
                                                  String pin, String mobileNumber);

}
