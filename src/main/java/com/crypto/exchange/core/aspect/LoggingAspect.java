package com.crypto.exchange.core.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Точка среза: все методы во всех контроллерах и сервисах
     */
    @Pointcut("within(com.crypto.exchange.web.controller..*) || within(com.crypto.exchange.core.service..*)")
    public void applicationPackagePointcut() {
    }

    /**
     * Around-совет: перехватывает вызов метода, замеряет время работы и логирует параметры
     */
    @Around("applicationPackagePointcut()")
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