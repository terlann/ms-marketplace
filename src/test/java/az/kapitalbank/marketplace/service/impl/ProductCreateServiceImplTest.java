package az.kapitalbank.marketplace.service.impl;

import az.kapitalbank.marketplace.client.optimus.model.process.ProcessData;
import az.kapitalbank.marketplace.client.optimus.model.process.ProcessResponse;
import az.kapitalbank.marketplace.constants.FraudResultStatus;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static az.kapitalbank.marketplace.property.CustomerConstants.FULLNAME;
import static az.kapitalbank.marketplace.property.CustomerConstants.PINCODE;
import static az.kapitalbank.marketplace.property.CustomerConstants.PHONE_NUMBER;
import static az.kapitalbank.marketplace.property.CustomerConstants.CIF;
import static az.kapitalbank.marketplace.property.CustomerConstants.UMICO_USER_ID;
import static az.kapitalbank.marketplace.property.OrderConstants.LOAN_AMOUNT;
import static az.kapitalbank.marketplace.property.OrderConstants.TRACK_ID;
import static az.kapitalbank.marketplace.property.ScoringConstants.TASK_ID;
import static az.kapitalbank.marketplace.property.ScoringConstants.BUSINESS_KEY;
import static az.kapitalbank.marketplace.property.ScoringConstants.USER_TASK_SCORING;

@ExtendWith(MockitoExtension.class)
class ProductCreateServiceImplTest {

    @InjectMocks
    ScoringServiceImpl scoringService;

    @Test
    void whenStarScoringCall_ThenShouldBeSuccess() {
        /*CheckFraudResultEvent checkFraudResultEvent = Mockito.mock(CheckFraudResultEvent.class);

        Mockito.when(customerRepository.findById(trackId)).thenReturn(Optional.of(generateCustomerEntity()));
        Mockito.when(orderRepository.findById(trackId)).thenReturn(Optional.of(generateOrderEntity()));
        Mockito.when(scoringService.startScoring(trackId, pincode, phoneNumber)).thenReturn(trackId);
        Mockito.doNothing().when(orderRepository).save(generateOrderEntity());

        loanFormalizeService.startScoring(checkFraudResultEvent);

        Mockito.verify(customerRepository).findById(trackId);
        Mockito.verify(orderRepository).findById(trackId);
        Mockito.verify(scoringService).startScoring(trackId, pincode, phoneNumber);
        Mockito.verify(orderRepository).save(generateOrderEntity());*/
    }

    @Test
    void whenCreateScoringCall_ThenShouldBeSuccess() {
        Mockito.when(scoringService.getProcess(TRACK_ID, BUSINESS_KEY))
                .thenReturn(Optional.of(generateProcessResponse()));
    }

    public FraudCheckResultEvent generateCheckFraudResultEventBlackList() {
        FraudCheckResultEvent fraudCheckResultEvent = new FraudCheckResultEvent();
        fraudCheckResultEvent.setTrackId(TRACK_ID);
        fraudCheckResultEvent.setFraudResultStatus(FraudResultStatus.BLACKLIST);
        return fraudCheckResultEvent;
    }

    public CustomerEntity generateCustomerEntity() {
        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setFullName(FULLNAME);
        customerEntity.setUmicoUserId(UMICO_USER_ID);
        customerEntity.setMobileNumber(PHONE_NUMBER);
        customerEntity.setIdentityNumber(PINCODE);
        return customerEntity;
    }

    public OrderEntity generateOrderEntity() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(TRACK_ID);
        orderEntity.setTotalAmount(new BigDecimal(LOAN_AMOUNT));
        orderEntity.setCustomer(generateCustomerEntity());
        return orderEntity;
    }

    public ScoringResultEvent scoringResultEvent() {
        ScoringResultEvent scoringResultEvent = new ScoringResultEvent();
        scoringResultEvent.setProcessStatus(USER_TASK_SCORING);
        return scoringResultEvent;
    }

    public ProcessResponse generateProcessResponse() {
        return ProcessResponse.builder()
                .processCreateTime(new Date())
                .taskId(TASK_ID)
                .variables(ProcessData.builder()
                        .cif(CIF)
                        .pin(PINCODE)
                        .build())
                .build();

    }

}