package com.zrs.spring.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface MyRequestParameter {

    String value() default "";
}
