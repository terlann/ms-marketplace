package az.kapitalbank.marketplace.mappers;

import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringVariable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ScoringMapper {

    @Mapping(target = "cardProductCode", constant = "BUMM")
    @Mapping(target = "isMarketPlaceOperation", constant = "true")
    @Mapping(target = "phoneNumber", source = "mobileNumber")
    @Mapping(target = "phoneNumberVerified", constant = "true")
    @Mapping(source = "pin", target = "pin")
    @Mapping(target = "preApproval", constant = "false")
    @Mapping(target = "processProductType", source = "productType")
    @Mapping(target = "scoreCard", constant = "true")
    @Mapping(target = "scoreCash", constant = "false")
    StartScoringVariable toStartScoringVariable(String pin, String mobileNumber, String productType);


}
