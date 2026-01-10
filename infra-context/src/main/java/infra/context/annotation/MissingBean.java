/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.context.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.factory.BeanFactory;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.SearchStrategy;
import infra.core.annotation.AliasFor;
import infra.stereotype.Component;

/**
 * Context will create a bean definition when current context were missing
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-01-31 14:36
 */
@Component
@ConditionalOnMissingBean
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface MissingBean {

  /**
   * The class types of beans that should be checked. The condition matches when no bean
   * of each class specified is contained in the {@link BeanFactory}.
   *
   * @return the class types of beans to check
   */
  @AliasFor(annotation = ConditionalOnMissingBean.class, attribute = "value")
  Class<?>[] value() default {};

  /**
   * Missing bean name
   * <p>
   * this attr determine the bean definition
   * </p>
   *
   * <p>
   * when its declare on a method default bean name is method-name
   * </p>
   */
  @AliasFor(annotation = ConditionalOnMissingBean.class, attribute = "name")
  String[] name() default {};

  /**
   * The class type names of beans that should be checked. The condition matches when no
   * bean of each class specified is contained in the {@link BeanFactory}.
   *
   * @return the class type names of beans to check
   */
  @AliasFor(annotation = ConditionalOnMissingBean.class, attribute = "type")
  String[] type() default {};

  /**
   * The class types of beans that should be ignored when identifying matching beans.
   *
   * @return the class types of beans to ignore
   */
  @AliasFor(annotation = ConditionalOnMissingBean.class, attribute = "ignored")
  Class<?>[] ignored() default {};

  /**
   * The class type names of beans that should be ignored when identifying matching
   * beans.
   *
   * @return the class type names of beans to ignore
   */
  @AliasFor(annotation = ConditionalOnMissingBean.class, attribute = "ignoredType")
  String[] ignoredType() default {};

  /**
   * The annotation type decorating a bean that should be checked. The condition matches
   * when each annotation specified is missing from all beans in the
   * {@link BeanFactory}.
   *
   * @return the class-level annotation types to check
   */
  @AliasFor(annotation = ConditionalOnMissingBean.class, attribute = "annotation")
  Class<? extends Annotation>[] annotation() default {};

  /**
   * Strategy to decide if the application context hierarchy (parent contexts) should be
   * considered.
   *
   * @return the search strategy
   */
  @AliasFor(annotation = ConditionalOnMissingBean.class, attribute = "search")
  SearchStrategy search() default SearchStrategy.ALL;

  /**
   * Additional classes that may contain the specified bean types within their generic
   * parameters. For example, an annotation declaring {@code value=Name.class} and
   * {@code parameterizedContainer=NameRegistration.class} would detect both
   * {@code Name} and {@code NameRegistration<Name>}.
   *
   * @return the container types
   */
  @AliasFor(annotation = ConditionalOnMissingBean.class, attribute = "parameterizedContainer")
  Class<?>[] parameterizedContainer() default {};

}
