package az.kapitalbank.marketplace.constants;

import static az.kapitalbank.marketplace.constants.TestConstants.BUSINESS_KEY;
import static az.kapitalbank.marketplace.constants.TestConstants.CARD_UID;
import static az.kapitalbank.marketplace.constants.TestConstants.CUSTOMER_ID;
import static az.kapitalbank.marketplace.constants.TestConstants.TASK_ID;
import static az.kapitalbank.marketplace.constants.TestConstants.TRACK_ID;
import static az.kapitalbank.marketplace.constants.TestConstants.UMICO_USER_ID;

import az.kapitalbank.marketplace.client.atlas.model.response.AccountResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.CardDetailResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.CreateCardCreditRequest;
import az.kapitalbank.marketplace.client.optimus.model.process.Offer;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessData;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.client.optimus.model.process.SelectedOffer;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.ProductEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ConstantObject {

    public static CustomerEntity getCustomerEntity() {
        return CustomerEntity.builder()
                .umicoUserId(UMICO_USER_ID.getValue())
                .cardId(CARD_UID.getValue()).build();
    }

    public static CustomerEntity getCustomerEntity2() {
        return CustomerEntity.builder()
                .id(UUID.fromString(CUSTOMER_ID.getValue()))
                .build();
    }

    public static OperationEntity getOperationEntity() {
        return OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .orders(List.of(getOrderEntity()))
                .commission(BigDecimal.valueOf(12))
                .customer(getCustomerEntity())
                .totalAmount(BigDecimal.ONE)
                .dvsOrderId(12345L)
                .taskId(TASK_ID.getValue())
                .businessKey(BUSINESS_KEY.getValue())
                .scoredAmount(BigDecimal.ONE)
                .build();
    }

    public static OperationEntity getOperationEntityAutoRefund() {
        return OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .commission(BigDecimal.valueOf(12))
                .customer(getCustomerEntity())
                .totalAmount(BigDecimal.ONE)
                .dvsOrderId(12345L)
                .taskId(TASK_ID.getValue())
                .businessKey(BUSINESS_KEY.getValue())
                .scoredAmount(BigDecimal.ONE)
                .build();
    }

    public static OperationEntity getOperationEntityForMonthlyPayment() {
        return OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .orders(List.of(getOrderEntity()))
                .commission(BigDecimal.valueOf(12))
                .customer(getCustomerEntity())
                .totalAmount(BigDecimal.ONE)
                .loanTerm(3)
                .build();
    }

    public static ProcessResponse getProcessResponse() {
        return ProcessResponse.builder()
                .variables(ProcessData.builder()
                        .dvsOrderId(12345L)
                        .createCardCreditRequest(CreateCardCreditRequest.builder()
                                .startDate(LocalDate.parse("2020-02-02"))
                                .endDate(LocalDate.parse("2020-02-02"))
                                .build())
                        .selectedOffer(SelectedOffer.builder()
                                .cardOffer(Offer.builder()
                                        .availableLoanAmount(BigDecimal.ONE)
                                        .build())
                                .build()).build()).build();
    }

    public static OperationEntity getOperationEntityFirstCustomer() {
        return OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .orders(List.of(getOrderEntity()))
                .commission(BigDecimal.valueOf(12))
                .customer(getCustomerEntity2())
                .totalAmount(BigDecimal.ONE)
                .dvsOrderId(12345L)
                .taskId(TASK_ID.getValue())
                .businessKey(BUSINESS_KEY.getValue())
                .build();
    }

    public static OrderEntity getOrderEntity() {
        return OrderEntity.builder()
                .products(List.of(getProductEntity()))
                .totalAmount(BigDecimal.ONE)
                .commission(BigDecimal.ONE)
                .build();
    }

    public static OrderEntity getOrderEntityAutoRefund() {
        return OrderEntity.builder()
                .totalAmount(BigDecimal.ONE)
                .commission(BigDecimal.ONE)
                .transactionId("123456")
                .operation(getOperationEntityAutoRefund())
                .build();
    }

    public static ProductEntity getProductEntity() {
        return ProductEntity.builder()
                .productNo("p1")
                .amount(BigDecimal.ONE)
                .build();
    }

    public static ProductEntity getProductEntity2() {
        return ProductEntity.builder()
                .productNo("p2")
                .amount(BigDecimal.ONE)
                .build();
    }

    public static CardDetailResponse getCardDetailResponse() {
        return CardDetailResponse.builder()
                .accounts(List.of(getAccountResponse()))
                .build();
    }

    public static AccountResponse getAccountResponse() {
        return AccountResponse.builder()
                .status(AccountStatus.OPEN_PRIMARY)
                .availableBalance(BigDecimal.valueOf(10000L))
                .overdraftLimit(BigDecimal.valueOf(10000L)).build();
    }
}
