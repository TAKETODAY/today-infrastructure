package infra.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;

/**
 * Annotation to declare that other beans depend on the annotated bean.
 * <p>This annotation can be used on types or methods to specify bean names or types
 * that should be initialized after the annotated bean, ensuring proper dependency order.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/24 11:19
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface RequiredBy {

  /**
   * Bean names that depend on the annotated bean.
   */
  @AliasFor(annotation = RequiredBy.class, attribute = "names")
  String[] value() default {};

  /**
   * Bean names that depend on the annotated bean.
   */
  @AliasFor(annotation = RequiredBy.class, attribute = "value")
  String[] names() default {};

  /**
   * Bean types that depend on the annotated bean.
   * All beans matching these types will be made to depend on the annotated bean.
   */
  Class<?>[] types() default {};

}