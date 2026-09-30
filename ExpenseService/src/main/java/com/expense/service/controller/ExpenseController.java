package com.expense.service.controller;

import com.expense.service.dto.ExpenseDto;
import com.expense.service.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expense/v1")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;


    // =========================================================
    // GET ALL EXPENSES FOR AUTHENTICATED USER
    // =========================================================

    @GetMapping("/get")
    public ResponseEntity<?> getExpenses(
            @RequestHeader("X-User-Id") String userId) {

        try {

            List<ExpenseDto> expenseDtoList =
                    expenseService.getExpense(userId);

            if (expenseDtoList.isEmpty()) {
                return new ResponseEntity<>(
                        "No expenses found for this user",
                        HttpStatus.NO_CONTENT
                );
            }

            return new ResponseEntity<>(
                    expenseDtoList,
                    HttpStatus.OK
            );

        } catch (Exception e) {

            return new ResponseEntity<>(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    // =========================================================
    // GET EXPENSES BY DATE RANGE
    // =========================================================

    @GetMapping("/get/range")
    public ResponseEntity<?> getExpenseByDateRange(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("from") String from,
            @RequestParam("to") String to) {

        try {

            List<ExpenseDto> expenses =
                    expenseService.getExpenseByDateRange(
                            userId,
                            from,
                            to
                    );

            if (expenses.isEmpty()) {
                return new ResponseEntity<>(
                        "No expenses found in this date range",
                        HttpStatus.NO_CONTENT
                );
            }

            return new ResponseEntity<>(
                    expenses,
                    HttpStatus.OK
            );

        } catch (RuntimeException e) {

            return new ResponseEntity<>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );

        } catch (Exception e) {

            return new ResponseEntity<>(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    // =========================================================
    // CREATE EXPENSE
    // =========================================================

    @PostMapping("/create")
    public ResponseEntity<String> createExpense(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ExpenseDto expenseDto) {

        try {

            // Never trust user_id coming from request body.
            // Use authenticated user ID injected by Kong.
            expenseDto.setUserId(userId);

            boolean created =
                    expenseService.createExpense(expenseDto);

            if (created) {

                return new ResponseEntity<>(
                        "Expense created successfully",
                        HttpStatus.CREATED
                );
            }

            return new ResponseEntity<>(
                    "Expense creation failed",
                    HttpStatus.BAD_REQUEST
            );

        } catch (Exception e) {

            return new ResponseEntity<>(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    // =========================================================
    // UPDATE EXPENSE
    // =========================================================

    @PutMapping("/update")
    public ResponseEntity<String> updateExpense(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ExpenseDto expenseDto) {

        try {

            // Never trust user_id from client.
            // Force the authenticated user's ID.
            expenseDto.setUserId(userId);

            boolean updated =
                    expenseService.updateExpense(expenseDto);

            if (updated) {

                return new ResponseEntity<>(
                        "Expense updated successfully",
                        HttpStatus.OK
                );
            }

            return new ResponseEntity<>(
                    "Expense not found",
                    HttpStatus.NOT_FOUND
            );

        } catch (Exception e) {

            return new ResponseEntity<>(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    // =========================================================
    // DELETE EXPENSE
    // =========================================================

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteExpense(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("external_id") String externalId) {

        try {

            // userId comes ONLY from authenticated request.
            boolean deleted =
                    expenseService.deleteExpense(
                            userId,
                            externalId
                    );

            if (deleted) {

                return new ResponseEntity<>(
                        "Expense deleted successfully",
                        HttpStatus.OK
                );
            }

            return new ResponseEntity<>(
                    "Expense not found",
                    HttpStatus.NOT_FOUND
            );

        } catch (Exception e) {

            return new ResponseEntity<>(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    // =========================================================
    // ADD EXPENSE
    // =========================================================

    @PostMapping("/addExpense")
    public ResponseEntity<Boolean> addExpense(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-External-Id") String externalId,
            @RequestBody ExpenseDto expenseDto) {

        try {

            // Identity comes from Kong.
            expenseDto.setUserId(userId);

            // External ID comes from trusted request header.
            expenseDto.setExternalId(externalId);

            boolean created =
                    expenseService.createExpense(expenseDto);

            return new ResponseEntity<>(
                    created,
                    HttpStatus.OK
            );

        } catch (Exception e) {

            return new ResponseEntity<>(
                    false,
                    HttpStatus.BAD_REQUEST
            );
        }
    }


    // =========================================================
    // HEALTH CHECK
    // =========================================================

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkHealth() {

        return new ResponseEntity<>(
                true,
                HttpStatus.OK
        );
    }
}