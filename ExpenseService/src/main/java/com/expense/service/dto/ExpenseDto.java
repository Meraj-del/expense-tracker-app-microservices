package com.expense.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExpenseDto {

    private String externalId;

    @JsonProperty(value = "amount")
    private Double amount;

    @JsonProperty(value = "user_id")
    private String userId;

    @JsonProperty(value = "merchant")
    private String merchant;

    @JsonProperty(value = "currency")
    private String currency;

    @JsonProperty(value = "created_at")
    private Timestamp createdAt;

    public ExpenseDto(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ExpenseDto expenseDto = mapper.readValue(json, ExpenseDto.class);
            this.externalId = expenseDto.getExternalId();
            this.amount = expenseDto.getAmount();
            this.userId = expenseDto.getUserId();
            this.merchant = expenseDto.getMerchant();
            this.currency = expenseDto.getCurrency();
            this.createdAt = expenseDto.getCreatedAt();
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize ExpenseDto from json", e);
        }
    }
}