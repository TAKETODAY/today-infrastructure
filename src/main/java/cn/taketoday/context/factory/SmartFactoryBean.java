/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.factory;

/**
 * Extension of the {@link FactoryBean} interface. Implementations may
 * indicate whether they always return independent instances, for the
 * case where their is singleton implementation returning {@code false}
 * does not clearly indicate independent instances.
 *
 * <p>Plain {@link FactoryBean} implementations which do not implement
 * this extended interface are simply assumed to always return independent
 * instances if their is singleton implementation returns {@code false};
 * the exposed object is only accessed on demand.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework and within collaborating frameworks.
 * In general, application-provided FactoryBeans should simply implement
 * the plain {@link FactoryBean} interface. New methods might be added
 * to this extended interface even in point releases.
 *
 * @param <T>
 *         the bean type
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/3/9 14:06
 * @since 3.0
 */
public interface SmartFactoryBean<T> extends FactoryBean<T> {

  /**
   * Does this FactoryBean expect eager initialization, that is,
   * eagerly initialize itself as well as expect eager initialization
   * of its singleton object (if any)?
   * <p>A standard FactoryBean is not expected to initialize eagerly:
   * Its {@link #getBean()} will only be called for actual access, even
   * in case of a singleton object. Returning {@code true} from this
   * method suggests that {@link #getBean()} should be called eagerly,
   * also applying post-processors eagerly. This may make sense in case
   * of a is singleton object, in particular if post-processors expect
   * to be applied on startup.
   * <p>The default implementation returns {@code false}.
   *
   * @return whether eager initialization applies
   *
   * @see AbstractBeanFactory#initializeSingletons()
   */
  default boolean isEagerInit() {
    return false;
  }

}
