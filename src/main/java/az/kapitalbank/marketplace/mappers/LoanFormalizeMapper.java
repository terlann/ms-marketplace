package az.kapitalbank.marketplace.mappers;

import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderRequest;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.constant.DvsConstant;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.mappers.qualifier.LoanFormalizationQualifier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = LoanFormalizationQualifier.class)
public interface LoanFormalizeMapper {

    @Mapping(source = "operationEntity.fullName", target = "fullName")
    @Mapping(source = "operationEntity.email", target = "email")
    @Mapping(source = "operationEntity.mobileNumber", target = "mobile")
    @Mapping(source = "operationEntity.pin", target = "pin")
    @Mapping(source = "processResponse.variables.cif", target = "cif")
    @Mapping(source = "processResponse.variables.cashCreditContractNumber", target = "cashContract")
    @Mapping(source = "processResponse.taskId", target = "processId")
    @Mapping(constant = DvsConstant.SALES_CHANNEL, target = "salesChannel")
    @Mapping(constant = DvsConstant.SALES_SOURCE, target = "salesSource")
    @Mapping(constant = DvsConstant.BRANCH_CODE, target = "branchCode")
    @Mapping(constant = DvsConstant.PRODUCT, target = "product")
    @Mapping(constant = DvsConstant.AGENT, target = "agent")
    @Mapping(constant = "false", target = "confirmed")
    @Mapping(source = "processResponse.variables.period", target = "tenure")
    @Mapping(source = "processResponse.variables.creditDocumentsInfo.path", target = "documents",
            qualifiedByName = "asList")
    @Mapping(source = "processResponse.variables.selectedOffer.cashOffer.interestRate", target = "loanPercentage",
            qualifiedByName = "toString")
    @Mapping(source = "processResponse.processCreateTime", target = "creditCreatedAt",
            qualifiedByName = "toLocalDateTime")
    @Mapping(constant = "processResponse.variables.selectedOffer.cashOffer.availableLoanAmount", target = "cashAmount")
    DvsCreateOrderRequest toDvsCreateOrderRequest(OperationEntity operationEntity, ProcessResponse processResponse);

}
