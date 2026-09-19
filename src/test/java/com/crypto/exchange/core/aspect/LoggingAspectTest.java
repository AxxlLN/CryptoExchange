package com.crypto.exchange.core.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @InjectMocks
    private LoggingAspect loggingAspect;

    @BeforeEach
    void setUp() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.crypto.exchange.TestService");
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1"});
    }

    @Test
    @DisplayName("Should successfully log method execution and return result")
    void logExecutionDetails_Success() throws Throwable {
        when(joinPoint.proceed()).thenReturn("successResult");

        Object result = loggingAspect.logExecutionDetails(joinPoint);

        assertThat(result).isEqualTo("successResult");
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    @DisplayName("Should rethrow IllegalArgumentException when target method throws it")
    void logExecutionDetails_IllegalArgumentException() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("Invalid arg"));

        assertThatThrownBy(() -> loggingAspect.logExecutionDetails(joinPoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid arg");
    }

    @Test
    @DisplayName("Should rethrow generic Throwable when target method fails")
    void logExecutionDetails_GenericException() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> loggingAspect.logExecutionDetails(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
    }
}