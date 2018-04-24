package com.yhj.web.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface ActionProcessor {

	String prefix() default "/WEB-INF/view";

	String suffix() default ".jsp";

}
