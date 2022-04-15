package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.ConstantObject.getOperationEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.loan.LoanClient;
import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderRequest;
import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderResponse;
import az.kapitalbank.marketplace.constant.FraudType;
import az.kapitalbank.marketplace.dto.request.LoanRequest;
import az.kapitalbank.marketplace.dto.response.LeadResponse;
import az.kapitalbank.marketplace.dto.response.LoanResponse;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.mapper.TelesalesMapper;
import java.util.List;
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
    UmicoService umicoService;
    @Mock
    TelesalesClient telesalesClient;
    @Mock
    TelesalesMapper telesalesMapper;
    @InjectMocks
    private LeadService leadService;


    @Test
    void sendLead_Success() {
        var createTelesalesOrderRequest = CreateTelesalesOrderRequest.builder().build();
        var createTelesalesOrderResponse = CreateTelesalesOrderResponse.builder().build();
        when(telesalesMapper.toTelesalesOrder(any(OperationEntity.class),
                eq(List.of(FraudType.PIN)))).thenReturn(createTelesalesOrderRequest);
        when(telesalesClient.sendLead(any(CreateTelesalesOrderRequest.class)))
                .thenReturn(createTelesalesOrderResponse);

        leadService.sendLead(getOperationEntity(), List.of(FraudType.PIN));
        verify(telesalesMapper).toTelesalesOrder(any(OperationEntity.class),
                eq(List.of(FraudType.PIN)));
    }

    @Test
    void testSendLead_UmicoServiceReturnsAbsent() {
        var createTelesalesOrderRequest = CreateTelesalesOrderRequest.builder().build();
        var createTelesalesOrderResponse = CreateTelesalesOrderResponse.builder().build();
        var loanResponse = new LoanResponse(new LeadResponse("leadId"));

        when(telesalesMapper.toTelesalesOrder(any(OperationEntity.class),
                eq(List.of(FraudType.PIN)))).thenReturn(createTelesalesOrderRequest);
        when(telesalesClient.sendLead(any(CreateTelesalesOrderRequest.class))).thenReturn(
                createTelesalesOrderResponse);
        when(loanClient.sendLead(eq("0007"), any(LoanRequest.class)))
                .thenReturn(loanResponse);

        leadService.sendLead(getOperationEntity(), List.of(FraudType.PIN));
        verify(loanClient).sendLead(eq("0007"), any(LoanRequest.class));
    }
}
