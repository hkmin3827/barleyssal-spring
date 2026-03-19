package com.hakyung.barleyssal_spring.global.lock;

import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private static final String REDISSON_LOCK_PREFIX = "LOCK:";
    private final RedissonClient redissonClient;

    @Around("@annotation(com.hakyung.barleyssal_spring.global.lock.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        String dynamicKey = (String) CustomSpringELParser.getDynamicValue(
                signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key());
        String lockKey = REDISSON_LOCK_PREFIX + dynamicKey;

        RLock rLock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            if (!isLocked) {
                log.warn("[DistributedLock] 락 획득 실패: {}", lockKey);
                throw new CustomException(ErrorCode.CONCURRENT_REQUEST_DENIED);
            }

            log.debug("[DistributedLock] 락 획득 성공: {}", lockKey);
            
            return joinPoint.proceed();
            
        } catch (InterruptedException e) {
            log.error("[DistributedLock] 락 획득 대기 중 인터럽트 발생", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (rLock.isLocked() && rLock.isHeldByCurrentThread()) {
                    rLock.unlock();
                    log.debug("[DistributedLock] 락 해제 성공: {}", lockKey);
                }
            } catch (IllegalMonitorStateException e) {
                log.error("[DistributedLock] 이미 해제된 락입니다. lockKey: {}", lockKey, e);
            }
        }
    }
}