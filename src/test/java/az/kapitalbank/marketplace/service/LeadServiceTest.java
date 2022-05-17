package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.FAIL_IN_PREAPPROVED;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.PREAPPROVED;
import static az.kapitalbank.marketplace.constants.ConstantObject.getOperationEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.loan.LoanClient;
import az.kapitalbank.marketplace.client.loan.model.LeadResponse;
import az.kapitalbank.marketplace.client.loan.model.LoanRequest;
import az.kapitalbank.marketplace.client.loan.model.LoanResponse;
import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderRequest;
import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderResponse;
import az.kapitalbank.marketplace.constant.FraudType;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.mapper.TelesalesMapper;
import az.kapitalbank.marketplace.repository.OperationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    LoanClient loanClient;
    @Mock
    SmsService smsService;
    @Mock
    UmicoService umicoService;
    @Mock
    TelesalesMapper telesalesMapper;
    @Mock
    TelesalesClient telesalesClient;
    @Mock
    OperationRepository operationRepository;
    @InjectMocks
    private LeadService leadService;


    @Test
    void sendLead_Success() {
        var createTelesalesOrderRequest = CreateTelesalesOrderRequest.builder().build();
        var createTelesalesOrderResponse = CreateTelesalesOrderResponse.builder().build();
        when(telesalesMapper.toTelesalesOrder(any(OperationEntity.class),
                eq(List.of(FraudType.PIN)))).thenReturn(createTelesalesOrderRequest);
        when(telesalesClient.sendLead(any(CreateTelesalesOrderRequest.class))).thenReturn(
                createTelesalesOrderResponse);

        leadService.sendLead(getOperationEntity(), List.of(FraudType.PIN));
        verify(telesalesMapper).toTelesalesOrder(any(OperationEntity.class),
                eq(List.of(FraudType.PIN)));
    }

    @Test
    void testSendLead_UmicoServiceReturnsAbsent() {
        var loanResponse = new LoanResponse(new LeadResponse("leadId"));

        when(loanClient.sendLead(eq("0007"), any(LoanRequest.class))).thenReturn(loanResponse);

        leadService.sendLeadLoan(getOperationEntity());
        verify(loanClient).sendLead(eq("0007"), any(LoanRequest.class));
    }

    @Test
    void sendLead_Exception_Request_Not_readable() {

        var createTelesalesOrderRequest = CreateTelesalesOrderRequest.builder().build();
        var createTelesalesOrderResponse = CreateTelesalesOrderResponse.builder()
                .response(new CreateTelesalesOrderResponse.Response("", "Request not readable"))
                .build();
        when(telesalesMapper.toTelesalesOrder(any(OperationEntity.class),
                eq(List.of(FraudType.PIN)))).thenReturn(createTelesalesOrderRequest);
        when(telesalesClient.sendLead(any(CreateTelesalesOrderRequest.class))).thenReturn(
                createTelesalesOrderResponse);

        leadService.sendLead(getOperationEntity(), List.of(FraudType.PIN));
        verify(telesalesMapper).toTelesalesOrder(any(OperationEntity.class),
                eq(List.of(FraudType.PIN)));

    }

    @Test
    void sendLead_Exception_Internal_Server_Error() {

        var createTelesalesOrderRequest = CreateTelesalesOrderRequest.builder().build();
        var createTelesalesOrderResponse = CreateTelesalesOrderResponse.builder()
                .response(new CreateTelesalesOrderResponse.Response("5", ""))
                .build();
        when(telesalesMapper.toTelesalesOrder(any(OperationEntity.class),
                eq(List.of(FraudType.PIN)))).thenReturn(createTelesalesOrderRequest);
        when(telesalesClient.sendLead(any(CreateTelesalesOrderRequest.class))).thenReturn(
                createTelesalesOrderResponse);

        leadService.sendLead(getOperationEntity(), List.of(FraudType.PIN));
        verify(telesalesMapper).toTelesalesOrder(any(OperationEntity.class),
                eq(List.of(FraudType.PIN)));

    }

    @Test
    void sendLeadSchedule_Success() {
        when(operationRepository.findByUpdatedAtBeforeAndUmicoDecisionStatusIn(
                any(OffsetDateTime.class),
                eq(Set.of(PREAPPROVED, FAIL_IN_PREAPPROVED)))).thenReturn(
                List.of(getOperationEntity()));
        leadService.sendLeadSchedule();
        verify(operationRepository).findByUpdatedAtBeforeAndUmicoDecisionStatusIn(
                any(OffsetDateTime.class),
                eq(Set.of(PREAPPROVED, FAIL_IN_PREAPPROVED)));
    }
}
