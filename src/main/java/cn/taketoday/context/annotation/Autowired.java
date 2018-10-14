package cn.taketoday.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {

	/**
	 * bean name
	 * 
	 * @return
	 */
	String value() default "";

	/**
	 * @return class
	 */
	Class<?> class_() default Class.class;

	/**
	 * is required ?
	 * 
	 * @return
	 */
	boolean required() default true;

}
