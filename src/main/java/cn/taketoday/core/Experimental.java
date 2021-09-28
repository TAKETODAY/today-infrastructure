package cn.taketoday.core;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A common annotation to declare that annotated elements is <b>Experimental</b>
 * May be deleted in the future
 *
 * @author TODAY 2021/9/28 11:24
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({
				ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD,
				ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR
})
public @interface Experimental {
}
