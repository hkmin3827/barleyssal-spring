package com.hakyung.barleyssal_spring.global.ratelimit;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int maxRequests() default 5;
    int windowSeconds() default 1;
}