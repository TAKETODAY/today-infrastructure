/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.context.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.SearchStrategy;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.stereotype.Component;

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
