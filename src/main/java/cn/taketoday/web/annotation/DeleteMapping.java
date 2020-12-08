package cn.taketoday.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.Constant;
import cn.taketoday.web.RequestMethod;

/**
 * @author TODAY
 * @date 2020/12/8 21:47
 */
@Retention(RetentionPolicy.RUNTIME)
@ActionMapping(method = RequestMethod.DELETE)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface DeleteMapping {

  /** urls */
  String[] value() default Constant.BLANK;

  /** Exclude url on class */
  boolean exclude() default false;

}
