
package example.gh24375;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnclosingAnnotation {

  @AliasFor("nested2")
  NestedAnnotation nested1() default @NestedAnnotation;

  @AliasFor("nested1")
  NestedAnnotation nested2() default @NestedAnnotation;

}
