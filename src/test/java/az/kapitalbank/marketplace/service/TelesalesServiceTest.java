package az.kapitalbank.marketplace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.loan.LoanClient;
import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.client.telesales.exception.TelesalesClientException;
import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderRequest;
import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderResponse;
import az.kapitalbank.marketplace.constant.FraudResultStatus;
import az.kapitalbank.marketplace.constant.FraudType;
import az.kapitalbank.marketplace.constant.ProductType;
import az.kapitalbank.marketplace.constant.SubProductType;
import az.kapitalbank.marketplace.dto.LeadDto;
import az.kapitalbank.marketplace.dto.request.LoanRequest;
import az.kapitalbank.marketplace.dto.response.LeadResponse;
import az.kapitalbank.marketplace.dto.response.LoanResponse;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.mapper.TelesalesMapper;
import az.kapitalbank.marketplace.repository.OperationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelesalesServiceTest {

    @Mock
    TelesalesClient telesalesClient;
    @Mock
    TelesalesMapper telesalesMapper;
    @Mock
    OperationRepository operationRepository;
    @InjectMocks
    TelesalesService telesalesService;
    @Mock
    LoanClient loanClient;

    @Test
    void sendLead_Success() {
        var trackId = UUID.fromString("6f032496-9a2c-11ec-b909-0242ac120002");
        var operationEntity = OperationEntity.builder()
                .totalAmount(BigDecimal.valueOf(1))
                .commission(BigDecimal.valueOf(0.14))
                .build();
        var createTelesalesOrderRequest = CreateTelesalesOrderRequest.builder().build();
        var fraudTypes = List.of(FraudType.PIN);
        BigDecimal amountWithCommission = BigDecimal.valueOf(1.14);
        var createTelesalesOrderResponse = CreateTelesalesOrderResponse.builder()
                .operationId("7572ef38-9ab2-11ec-b909-0242ac120002")
                .build();
        var leadDto = LeadDto.builder()
                .trackId(trackId)
                .fraudResultStatus(FraudResultStatus.SUSPICIOUS)
                .types(List.of(FraudType.PIN))
                .build();
        when(operationRepository.findById(trackId)).thenReturn(Optional.of(operationEntity));
        when(telesalesMapper.toTelesalesOrder(operationEntity, fraudTypes)).thenReturn(
                createTelesalesOrderRequest);
        when(telesalesClient.sendLead(createTelesalesOrderRequest)).thenReturn(
                createTelesalesOrderResponse);

        var actual = telesalesService.sendLead(leadDto);
        var expected = Optional.of("7572ef38-9ab2-11ec-b909-0242ac120002");

        assertEquals(expected, actual);

    }

    @Test
    void sendLead_when_throwException() {
        var trackId = UUID.fromString("6f032496-9a2c-11ec-b909-0242ac120002");
        var operationEntity = OperationEntity.builder()
                .totalAmount(BigDecimal.valueOf(1))
                .commission(BigDecimal.valueOf(0.14))
                .build();
        var createTelesalesOrderRequest = CreateTelesalesOrderRequest.builder().build();
        var fraudTypes = List.of(FraudType.PIN);
        var leadDto = LeadDto.builder()
                .trackId(trackId)
                .fraudResultStatus(FraudResultStatus.SUSPICIOUS)
                .types(List.of(FraudType.PIN))
                .build();
        when(operationRepository.findById(trackId)).thenReturn(Optional.of(operationEntity));
        when(telesalesMapper.toTelesalesOrder(operationEntity, fraudTypes)).thenReturn(
                createTelesalesOrderRequest);
        when(telesalesClient.sendLead(createTelesalesOrderRequest))
                .thenThrow(new TelesalesClientException("0", "Telesales Error"));

        var actual = telesalesService.sendLead(leadDto);
        var expected = Optional.empty();

        assertEquals(expected, actual);

    }

    @Test
    void sendLead_To_Loan_Success() {
        var source = "0007";
        var trackId = UUID.fromString("6f032496-9a2c-11ec-b909-0242ac120002");
        var operationEntity = OperationEntity.builder()
                .mobileNumber("994552844590")
                .totalAmount(BigDecimal.valueOf(1))
                .commission(BigDecimal.valueOf(0.14))
                .build();
        BigDecimal amountWithCommission = BigDecimal.valueOf(1.14);
        LoanRequest loanRequest = LoanRequest.builder()
                .productType(ProductType.BIRKART)
                .subProductType(SubProductType.UMICO)
                .phoneNumber(operationEntity.getMobileNumber())
                .fullName(operationEntity.getFullName())
                .pinCode(operationEntity.getPin())
                .productAmount(amountWithCommission)
                .build();
        var loanResponse = LoanResponse.builder()
                .data(LeadResponse.builder()
                        .leadId("12345")
                        .build()).build();
        when(operationRepository.findById(trackId)).thenReturn(Optional.of(operationEntity));
        when(loanClient.sendLead(source, loanRequest)).thenReturn(loanResponse);
        var actual = telesalesService.sendLeadToLoanService(trackId);
        var expected = Optional.of(LoanResponse.builder()
                .data(LeadResponse.builder()
                        .leadId("12345")
                        .build()).build());
        assertEquals(expected, actual);
    }

    @Test
    void sendLead_To_Loan_when_throwException() {
        var source = "0007";
        var trackId = UUID.fromString("6f032496-9a2c-11ec-b909-0242ac120002");
        var operationEntity = OperationEntity.builder()
                .totalAmount(BigDecimal.valueOf(1))
                .commission(BigDecimal.valueOf(0.14))
                .build();

        BigDecimal amountWithCommission = BigDecimal.valueOf(1.14);

        LoanRequest loanRequest = LoanRequest.builder()
                .productType(ProductType.BIRKART)
                .subProductType(SubProductType.UMICO)
                .phoneNumber(operationEntity.getMobileNumber())
                .fullName(operationEntity.getFullName())
                .pinCode(operationEntity.getPin())
                .productAmount(amountWithCommission)
                .build();

        when(operationRepository.findById(trackId)).thenReturn(Optional.of(operationEntity));
        when(loanClient.sendLead(source,loanRequest))
                .thenThrow(new RuntimeException("Send error to loan service !"));

        var actual = telesalesService.sendLeadToLoanService(trackId);
        var expected = Optional.empty();

        assertEquals(expected, actual);

    }

}
