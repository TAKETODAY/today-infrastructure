
package com.yhj.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.yhj.web.enums.RequestMethod;


@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ActionMapping {

	/***
	 * 请求路径
	 * @return
	 */
	String[] value() default {};

	/**
	 * 请求方法	大写
	 * @return
	 */
	RequestMethod[] method() default {
		RequestMethod.GET,
		RequestMethod.PUT,
		RequestMethod.POST,
		RequestMethod.DELETE
	};
	
}
