package com.crypto.exchange.core.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Перехватывает вызовы методов во всех контроллерах и сервисах,
     * замеряет время выполнения и логирует параметры.
     */
    @Around("within(com.crypto.exchange.web.controller..*) || within(com.crypto.exchange.core.service..*)")
    public Object logExecutionDetails(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("Enter: {}.{}() with argument(s) = {}", className, methodName, Arrays.toString(args));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;

            log.info("Exit: {}.{}() executed in {} ms with result = {}", className, methodName, executionTime, result);
            return result;
        } catch (IllegalArgumentException e) {
            log.error("Illegal argument: {} in {}.{}()", Arrays.toString(args), className, methodName);
            throw e;
        } catch (Throwable e) {
            log.error("Exception in {}.{}() with cause = '{}' and exception = '{}'",
                    className, methodName, e.getCause() != null ? e.getCause() : "NULL", e.getMessage());
            throw e;
        }
    }
}