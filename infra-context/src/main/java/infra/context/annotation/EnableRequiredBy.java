package infra.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables support for processing {@code @RequiredBy} annotations on beans.
 * <p>
 * This annotation should be declared on a configuration class to import the
 * {@link RequiredByDependencyConfigurer}, which registers the necessary infrastructure
 * beans to handle dependency requirements defined by {@code @RequiredBy}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see RequiredBy
 * @since 5.0 2026/3/24 23:27
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RequiredByDependencyConfigurer.class)
public @interface EnableRequiredBy {

}
