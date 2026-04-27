package com.expense.service.service;

import com.expense.service.dto.ExpenseDto;
import com.expense.service.entities.Expense;
import com.expense.service.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public boolean createExpense(ExpenseDto expenseDto) {
        try {
            setCurrency(expenseDto);
            Expense expense = new Expense();
            expense.setUserId(expenseDto.getUserId());
            expense.setAmount(expenseDto.getAmount());
            expense.setMerchant(expenseDto.getMerchant());
            expense.setCurrency(expenseDto.getCurrency());
            expense.setCreatedAt(expenseDto.getCreatedAt());
            expenseRepository.save(expense);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateExpense(ExpenseDto expenseDto) {
        Optional<Expense> expenseFoundOpt = expenseRepository
                .findByUserIdAndExternalId(expenseDto.getUserId(), expenseDto.getExternalId());
        if (expenseFoundOpt.isEmpty()) {
            return false;
        }
        Expense expense = expenseFoundOpt.get();
        expense.setCurrency(Strings.isNotBlank(expenseDto.getCurrency())
                ? expenseDto.getCurrency() : expense.getCurrency());
        expense.setMerchant(Strings.isNotBlank(expenseDto.getMerchant())
                ? expenseDto.getMerchant() : expense.getMerchant());
        if (expenseDto.getAmount() != null) {
            expense.setAmount(expenseDto.getAmount());
        }
        expenseRepository.save(expense);
        return true;
    }

    public List<ExpenseDto> getExpenseByDateRange(String userId, String from, String to) {
        try {
            Timestamp startDate = Timestamp.valueOf(from + " 00:00:00");
            Timestamp endDate = Timestamp.valueOf(to + " 23:59:59");
            List<Expense> expenses = expenseRepository
                    .findByUserIdAndCreatedAtBetween(userId, startDate, endDate);
            return expenses.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Invalid date format. Use yyyy-MM-dd example: 2024-01-01");
        }
    }

    public boolean deleteExpense(String userId, String externalId) {
        Optional<Expense> expenseOpt = expenseRepository
                .findByUserIdAndExternalId(userId, externalId);
        if (expenseOpt.isEmpty()) {
            return false;
        }
        expenseRepository.delete(expenseOpt.get());
        return true;
    }

    public List<ExpenseDto> getExpense(String userId) {
        List<Expense> expensesList = expenseRepository.findByUserId(userId);
        return expensesList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ExpenseDto toDto(Expense expense) {
        ExpenseDto dto = new ExpenseDto();
        dto.setUserId(expense.getUserId());
        dto.setAmount(expense.getAmount());
        dto.setMerchant(expense.getMerchant());
        dto.setCurrency(expense.getCurrency());
        dto.setExternalId(expense.getExternalId());
        dto.setCreatedAt(expense.getCreatedAt());
        return dto;
    }

    private void setCurrency(ExpenseDto expenseDto) {
        if (Objects.isNull(expenseDto.getCurrency())) {
            expenseDto.setCurrency("INR");
        }
    }
}