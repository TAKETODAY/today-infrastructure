/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.condition;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that only matches when no beans meeting the specified
 * requirements are already contained in the {@link BeanFactory}. None of the requirements
 * must be met for the condition to match and the requirements do not have to be met by
 * the same bean.
 * <p>
 * When placed on a {@code @Component} method, the bean class defaults to the return type of
 * the factory method:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class MyConfiguration {
 *
 *     &#064;Component
 *     &#064;ConditionalOnMissingBean
 *     public MyService myService() {
 *         ...
 *     }
 *
 * }</pre>
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
   * of each class specified is contained in the {@link BeanFactory}.
   *
   * @return the class types of beans to check
   */
  Class<?>[] value() default {};

  /**
   * The class type names of beans that should be checked. The condition matches when no
   * bean of each class specified is contained in the {@link BeanFactory}.
   *
   * @return the class type names of beans to check
   */
  String[] type() default {};

  /**
   * The class types of beans that should be ignored when identifying matching beans.
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
   * {@link BeanFactory}.
   *
   * @return the class-level annotation types to check
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
   *
   * @return the container types
   */
  Class<?>[] parameterizedContainer() default {};

}
