package az.kapitalbank.marketplace.mappers;

import az.kapitalbank.marketplace.client.optimus.model.scoring.StartScoringVariable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ScoringMapper {

    ScoringMapper INSTANCE = Mappers.getMapper(ScoringMapper.class);

    @Mapping(target = "cardProductCode",constant = "BUMM")
    @Mapping(target = "isMarketPlaceOperation", constant = "true")
    @Mapping(target = "phoneNumber",source = "phoneNumber")
    @Mapping(target = "phoneNumberVerified", constant = "true")
    @Mapping(source = "pinCode", target = "pin")
    @Mapping(target = "preApproval", constant = "false")
    @Mapping(target = "processProductType", source = "productType")
    @Mapping(target = "scoreCard", constant = "true")
    @Mapping(target = "scoreCash", constant = "false")
    StartScoringVariable toCustomerScoringVariable(String pinCode, String phoneNumber, String productType);


}
