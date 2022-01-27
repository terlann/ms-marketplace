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

    @Mapping(source = "pinCode", target = "pin")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    @Mapping(target = "scoreCash", constant = "true")
    @Mapping(target = "phoneNumberVerified", constant = "true")
    @Mapping(target = "processProductType", source = "productType")
    @Mapping(target = "isMarketPlaceOperation", constant = "true")
    StartScoringVariable toCustomerScoringVariable(String pinCode, String phoneNumber, String productType);


}
