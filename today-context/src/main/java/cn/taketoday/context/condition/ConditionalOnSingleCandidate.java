/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that only matches when a bean of the specified class
 * is already contained in the {@link BeanFactory} and a single candidate can be
 * determined.
 * <p>
 * The condition will also match if multiple matching bean instances are already contained
 * in the {@link BeanFactory} but a primary candidate has been defined; essentially, the
 * condition match if auto-wiring a bean with the defined type will succeed.
 * <p>
 * The condition can only match the bean definitions that have been processed by the
 * application context so far and, as such, it is strongly recommended to use this
 * condition on auto-configuration classes only. If a candidate bean may be created by
 * another auto-configuration, make sure that the one using this condition runs after.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBeanCondition.class)
public @interface ConditionalOnSingleCandidate {

  /**
   * The class type of bean that should be checked. The condition matches if a bean of
   * the class specified is contained in the {@link BeanFactory} and a primary candidate
   * exists in case of multiple instances.
   * <p>
   * This attribute may <strong>not</strong> be used in conjunction with
   * {@link #type()}, but it may be used instead of {@link #type()}.
   *
   * @return the class type of the bean to check
   */
  Class<?> value() default Object.class;

  /**
   * The class type name of bean that should be checked. The condition matches if a bean
   * of the class specified is contained in the {@link BeanFactory} and a primary
   * candidate exists in case of multiple instances.
   * <p>
   * This attribute may <strong>not</strong> be used in conjunction with
   * {@link #value()}, but it may be used instead of {@link #value()}.
   *
   * @return the class type name of the bean to check
   */
  String type() default "";

  /**
   * Strategy to decide if the application context hierarchy (parent contexts) should be
   * considered.
   *
   * @return the search strategy
   */
  SearchStrategy search() default SearchStrategy.ALL;

}
