package az.kapitalbank.marketplace.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.client.telesales.exception.TelesalesClientException;
import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderRequest;
import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderResponse;
import az.kapitalbank.marketplace.constant.FraudResultStatus;
import az.kapitalbank.marketplace.constant.FraudType;
import az.kapitalbank.marketplace.dto.LeadDto;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.mapper.TelesalesMapper;
import az.kapitalbank.marketplace.repository.OperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

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
        when(telesalesMapper.toTelesalesOrder(operationEntity, fraudTypes)).thenReturn(createTelesalesOrderRequest);
        when(telesalesClient.sendLead(createTelesalesOrderRequest)).thenReturn(createTelesalesOrderResponse);

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
        when(telesalesMapper.toTelesalesOrder(operationEntity, fraudTypes)).thenReturn(createTelesalesOrderRequest);
        when(telesalesClient.sendLead(createTelesalesOrderRequest))
                .thenThrow(new TelesalesClientException("0", "Telesales Error"));

        var actual = telesalesService.sendLead(leadDto);
        var expected = Optional.empty();

        assertEquals(expected, actual);

    }
}
