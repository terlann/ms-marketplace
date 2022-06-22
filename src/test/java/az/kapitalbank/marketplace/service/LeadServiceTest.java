package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.FAIL_IN_PREAPPROVED;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.PENDING;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.PREAPPROVED;
import static az.kapitalbank.marketplace.constants.ConstantObject.getOperationEntity;
import static az.kapitalbank.marketplace.constants.ConstantObject.getOperationEntityForMonthlyPayment;
import static az.kapitalbank.marketplace.constants.TestConstants.TRACK_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.loan.LoanClient;
import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.constant.SendLeadReason;
import az.kapitalbank.marketplace.constant.SendLeadType;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.mapper.OrderMapper;
import az.kapitalbank.marketplace.mapper.TelesalesMapper;
import az.kapitalbank.marketplace.messaging.publisher.FraudCheckPublisher;
import az.kapitalbank.marketplace.repository.OperationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    SmsService smsService;
    @Mock
    LoanClient loanClient;
    @Mock
    OrderMapper orderMapper;
    @Mock
    UmicoService umicoService;
    @Mock
    TelesalesClient telesalesClient;
    @Mock
    TelesalesMapper telesalesMapper;
    @Mock
    OperationRepository operationRepository;
    @Mock
    FraudCheckPublisher fraudCheckPublisher;
    @InjectMocks
    private LeadService leadService;


    @Test
    void sendLeadSchedule_Success() {
        when(operationRepository.findByUpdatedAtBeforeAndUmicoDecisionStatusIn(
                any(OffsetDateTime.class),
                eq(Set.of(PREAPPROVED, FAIL_IN_PREAPPROVED)))).thenReturn(
                List.of(getOperationEntity()));
        leadService.sendLeadNoActionDvs();
        verify(operationRepository).findByUpdatedAtBeforeAndUmicoDecisionStatusIn(
                any(OffsetDateTime.class), eq(Set.of(PREAPPROVED, FAIL_IN_PREAPPROVED)));
    }

    @Test
    void retrySendLead_Fraud() {
        OperationEntity operationEntity =
                OperationEntity.builder().id(UUID.fromString(TRACK_ID.getValue()))
                        .sendLeadReason(SendLeadReason.FRAUD_LIST).build();

        when(operationRepository.findByUmicoDecisionStatusAndIsSendLeadIsFalse(
                UmicoDecisionStatus.PENDING)).thenReturn(List.of(operationEntity));
        leadService.retrySendLead();
        verify(operationRepository).findByUmicoDecisionStatusAndIsSendLeadIsFalse(
                UmicoDecisionStatus.PENDING);
    }

    @Test
    void retrySendLead_Success() {
        OperationEntity operationEntity =
                OperationEntity.builder().id(UUID.fromString(TRACK_ID.getValue()))
                        .sendLeadReason(SendLeadReason.OPTIMUS_FAIL_GET_PROCESS).build();

        when(operationRepository.findByUmicoDecisionStatusAndIsSendLeadIsFalse(
                UmicoDecisionStatus.PENDING)).thenReturn(
                List.of(getOperationEntityForMonthlyPayment()));
        leadService.retrySendLead();
        verify(operationRepository).findByUmicoDecisionStatusAndIsSendLeadIsFalse(
                UmicoDecisionStatus.PENDING);
    }

    @Test
    void sendLeadManual_send_lead_failed() {
        leadService.sendLeadManual(SendLeadType.SEND_LEAD_FAILED);
        verify(operationRepository).findByUmicoDecisionStatusAndIsSendLeadIsFalse(PENDING);
    }

    @Test
    void sendLeadManual_no_action_dvs() {
        leadService.sendLeadManual(SendLeadType.NO_ACTION_DVS);
        verify(operationRepository).findByUpdatedAtBeforeAndUmicoDecisionStatusIn(any(), any());
    }
}
