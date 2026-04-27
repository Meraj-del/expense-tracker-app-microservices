package com.expense.service.controller;

import com.expense.service.dto.ExpenseDto;
import com.expense.service.service.ExpenseService;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expense/v1")
public class ExpenseController {

    private final ExpenseService expenseService;

    @Autowired
    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/get")
    public ResponseEntity<?> getExpenses(
            @RequestParam("user_id") String userId) {
        try {
            List<ExpenseDto> expenseDtoList = expenseService.getExpense(userId);
            if (expenseDtoList.isEmpty()) {
                return new ResponseEntity<>("No expenses found for this user", HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(expenseDtoList, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get/range")
    public ResponseEntity<?> getExpenseByDateRange(
            @RequestParam("user_id") String userId,
            @RequestParam("from") String from,
            @RequestParam("to") String to) {
        try {
            List<ExpenseDto> expenses = expenseService.getExpenseByDateRange(userId, from, to);
            if (expenses.isEmpty()) {
                return new ResponseEntity<>("No expenses found in this date range", HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(expenses, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<String> createExpense(
            @RequestBody ExpenseDto expenseDto) {
        try {
            boolean created = expenseService.createExpense(expenseDto);
            if (created) {
                return new ResponseEntity<>("Expense created successfully", HttpStatus.CREATED);
            }
            return new ResponseEntity<>("Expense creation failed", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateExpense(
            @RequestBody ExpenseDto expenseDto) {
        try {
            boolean updated = expenseService.updateExpense(expenseDto);
            if (updated) {
                return new ResponseEntity<>("Expense updated successfully", HttpStatus.OK);
            }
            return new ResponseEntity<>("Expense not found", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteExpense(
            @RequestParam("user_id") String userId,
            @RequestParam("external_id") String externalId) {
        try {
            boolean deleted = expenseService.deleteExpense(userId, externalId);
            if (deleted) {
                return new ResponseEntity<>("Expense deleted successfully", HttpStatus.OK);
            }
            return new ResponseEntity<>("Expense not found", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/addExpense")
    public ResponseEntity<Boolean> addExpense(@RequestHeader(value = "X-User-Id") @NonNull String userId, @RequestHeader(value = "X-External-Id") @NonNull String externalId,ExpenseDto expenseDto) {
        try{
            expenseDto.setUserId(userId);
            return new ResponseEntity<>(expenseService.createExpense(expenseDto), HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(false,HttpStatus.BAD_REQUEST);
        }
    }
}