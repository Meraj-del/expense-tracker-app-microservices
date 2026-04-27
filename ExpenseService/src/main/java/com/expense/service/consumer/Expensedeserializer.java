package com.expense.service.consumer;

import com.expense.service.dto.ExpenseDto;
import org.apache.kafka.common.serialization.Deserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class Expensedeserializer implements Deserializer<ExpenseDto> {

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public ExpenseDto deserialize(String arg0, byte[] arg1) {
        ObjectMapper mapper = new ObjectMapper();
        ExpenseDto expenseDto = null;
        try {
            expenseDto = mapper.readValue(arg1, ExpenseDto.class);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return expenseDto;
    }

}
