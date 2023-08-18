package io.github.merite.training.basic;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ApiAspect {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Pointcut("target(javax.sql.DataSource)")
    public void datasourcePointcut() {
        // datasource pointcut
    }

    @Around("datasourcePointcut()")
    public Object traceAdviceDataSource(ProceedingJoinPoint invocation) throws Throwable {
        return invokeDataSource(invocation);
    }

    private Object invokeDataSource(ProceedingJoinPoint invocation) throws Throwable {
        logger.debug("{}.{}", invocation.getTarget().getClass().getName(), invocation.getSignature().getName());
        return invocation.proceed();
    }
}
