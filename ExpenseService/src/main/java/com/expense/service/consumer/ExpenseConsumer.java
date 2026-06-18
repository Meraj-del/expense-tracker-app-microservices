package com.expense.service.consumer;

import com.expense.service.dto.ExpenseDto;
import com.expense.service.service.ExpenseService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseConsumer {

    private final ExpenseService expenseService;

    @PostConstruct
    public void init() {
        log.info("ExpenseConsumer initialized. Listening for expense events.");
    }

    @PreDestroy
    public void cleanup() {
        log.info("ExpenseConsumer shutting down cleanly.");
    }

    @KafkaListener(topics = "${spring.kafka.topic-json.name}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void listen(ExpenseDto expenseDto) {
        try {
            expenseService.createExpense(expenseDto);
            log.info("Expense created successfully: {}", expenseDto);
        } catch (Exception e) {
            log.error("Failed to process expense event: {}", expenseDto, e);
        }
    }
}
