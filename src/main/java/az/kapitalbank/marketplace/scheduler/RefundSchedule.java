package az.kapitalbank.marketplace.scheduler;

import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.service.OrderService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundSchedule {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(cron = "0 22 * * * ?", zone = "Asia/Baku")
    public void refundOrdersOver20Day() {
        log.info("Auto refund process started at {}", LocalDateTime.now());
        var purchaseDate = LocalDateTime.now().minusDays(21);
        var orders =
                orderRepository.findByTransactionDateBeforeAndTransactionStatusIn(purchaseDate,
                        List.of(TransactionStatus.PRE_PURCHASE,
                                TransactionStatus.FAIL_IN_COMPLETE_PRE_PURCHASE));
        for (var order : orders) {
            try {
                orderService.autoRefund(order);
                order.setTransactionStatus(TransactionStatus.AUTO_REFUND);
                order.setTransactionDate(LocalDateTime.now());
            } catch (Exception ex) {
                log.error("Auto refund order failed : orderNo - {}", order.getOrderNo());
                order.setTransactionStatus(TransactionStatus.FAIL_IN_AUTO_REFUND);
            }
        }
        orderRepository.saveAll(orders);
        log.info("Auto refund process finished at {}", LocalDateTime.now());
    }
}
