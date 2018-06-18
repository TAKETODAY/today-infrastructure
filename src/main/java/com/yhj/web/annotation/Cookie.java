package com.yhj.web.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cookie {
	
    boolean required() default true;

    String value() default "";

    
}