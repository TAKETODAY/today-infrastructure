
package com.yhj.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.yhj.web.interceptor.InterceptProcessor;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Interceptor {

	
	/**	配置拦截器 */
    Class<? extends InterceptProcessor>[] value() default {};
    
    
    
}


