package com.expense.service.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* com.expense.service.service.ExpenseService.*(..))")
    public void expenseServiceMethods() {}


    @Pointcut("execution(* com.expense.service.controller.ExpenseController.*(..))")
    public void expenseControllerMethods() {}

    @Around("expenseServiceMethods() || expenseControllerMethods()")
    public Object logAndTime(ProceedingJoinPoint joinPoint) throws Throwable {

        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("→ ENTER: {} | args: {}", method, args);
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("EXIT: {} | duration: {}ms | result: {}", method, duration, result);
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("ERROR: {} | duration: {}ms | error: {}", method, duration, e.getMessage());
            throw e;
        }
    }
}