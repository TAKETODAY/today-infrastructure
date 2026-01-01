/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.condition;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.context.annotation.Conditional;
import infra.stereotype.Component;

/**
 * {@link Conditional @Conditional} that only matches when no beans meeting the specified
 * requirements are already contained in the {@link BeanFactory}. None of the requirements
 * must be met for the condition to match and the requirements do not have to be met by
 * the same bean.
 * <p>
 * When placed on a {@code @Component} method, the bean class defaults to the return type of
 * the factory method:
 *
 * <pre>{@code
 * @Configuration
 * public class MyConfiguration {
 *
 *     @Component
 *     @ConditionalOnMissingBean
 *     public MyService myService() {
 *         ...
 *     }
 *
 * }}</pre>
 * <p>
 * In the sample above the condition will match if no bean of type {@code MyService} is
 * already contained in the {@link BeanFactory}.
 * <p>
 * The condition can only match the bean definitions that have been processed by the
 * application context so far and, as such, it is strongly recommended to use this
 * condition on auto-configuration classes only. If a candidate bean may be created by
 * another auto-configuration, make sure that the one using this condition runs after.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/25 21:32</a>
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnBeanCondition.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalOnMissingBean {

  /**
   * The class types of beans that should be checked. The condition matches when no bean
   * of each class specified is contained in the {@link BeanFactory}. Beans that are not
   * autowire candidates or that are not default candidates are ignored.
   * <p>
   * Since this annotation is parsed by loading class bytecode, it is safe to specify
   * classes here that may ultimately not be on the classpath, but only if this
   * annotation is directly on the affected component and <b>not</b> if this annotation
   * is used as a composed, meta-annotation. In order to use this annotation as a
   * meta-annotation, only use the {@link #type} attribute.
   *
   * @return the class types of beans to check
   * @see Component#autowireCandidate()
   * @see BeanDefinition#isAutowireCandidate
   * @see Component#defaultCandidate()
   * @see AbstractBeanDefinition#isDefaultCandidate
   */
  Class<?>[] value() default {};

  /**
   * The class type names of beans that should be checked. The condition matches when no
   * bean of each class specified is contained in the {@link BeanFactory}. Beans that
   * are not autowire candidates or that are not default candidates are ignored.
   *
   * @return the class type names of beans to check
   * @see Component#autowireCandidate()
   * @see BeanDefinition#isAutowireCandidate
   * @see Component#defaultCandidate()
   * @see AbstractBeanDefinition#isDefaultCandidate
   */
  String[] type() default {};

  /**
   * The class types of beans that should be ignored when identifying matching beans.
   * <p>
   * Since this annotation is parsed by loading class bytecode, it is safe to specify
   * classes here that may ultimately not be on the classpath, but only if this
   * annotation is directly on the affected component and <b>not</b> if this annotation
   * is used as a composed, meta-annotation. In order to use this annotation as a
   * meta-annotation, only use the {@link #ignoredType} attribute.
   *
   * @return the class types of beans to ignore
   */
  Class<?>[] ignored() default {};

  /**
   * The class type names of beans that should be ignored when identifying matching
   * beans.
   *
   * @return the class type names of beans to ignore
   */
  String[] ignoredType() default {};

  /**
   * The annotation type decorating a bean that should be checked. The condition matches
   * when each annotation specified is missing from all beans in the
   * {@link BeanFactory}. Beans that are not autowire candidates or that are not default
   * candidates are ignored.
   * <p>
   * Since this annotation is parsed by loading class bytecode, it is safe to specify
   * classes here that may ultimately not be on the classpath, but only if this
   * annotation is directly on the affected component and <b>not</b> if this annotation
   * is used as a composed, meta-annotation.
   *
   * @return the class-level annotation types to check
   * @see Component#autowireCandidate()
   * @see BeanDefinition#isAutowireCandidate
   * @see Component#defaultCandidate()
   * @see AbstractBeanDefinition#isDefaultCandidate
   */
  Class<? extends Annotation>[] annotation() default {};

  /**
   * The names of beans to check. The condition matches when each bean name specified is
   * missing in the {@link BeanFactory}.
   *
   * @return the names of beans to check
   */
  String[] name() default {};

  /**
   * Strategy to decide if the application context hierarchy (parent contexts) should be
   * considered.
   *
   * @return the search strategy
   */
  SearchStrategy search() default SearchStrategy.ALL;

  /**
   * Additional classes that may contain the specified bean types within their generic
   * parameters. For example, an annotation declaring {@code value=Name.class} and
   * {@code parameterizedContainer=NameRegistration.class} would detect both
   * {@code Name} and {@code NameRegistration<Name>}.
   * <p>
   * Since this annotation is parsed by loading class bytecode, it is safe to specify
   * classes here that may ultimately not be on the classpath, but only if this
   * annotation is directly on the affected component and <b>not</b> if this annotation
   * is used as a composed, meta-annotation.
   *
   * @return the container types
   */
  Class<?>[] parameterizedContainer() default {};

}
