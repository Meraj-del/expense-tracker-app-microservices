package com.expense.service.consumer;

import com.expense.service.dto.ExpenseDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Slf4j
public class Expensedeserializer implements Deserializer<ExpenseDto> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public ExpenseDto deserialize(String arg0, byte[] arg1) {
        try {
            return mapper.readValue(arg1, ExpenseDto.class);
        } catch (Exception e) {
            log.error("Failed to deserialize expense event: {}", e.getMessage());
            return null;
        }
    }

}
