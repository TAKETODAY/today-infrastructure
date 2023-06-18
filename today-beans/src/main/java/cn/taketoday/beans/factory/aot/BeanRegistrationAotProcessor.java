/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.aot;

import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.lang.Nullable;

/**
 * AOT processor that makes bean registration contributions by processing
 * {@link RegisteredBean} instances.
 *
 * <p>{@code BeanRegistrationAotProcessor} implementations may be registered in
 * a {@value AotServices#FACTORIES_RESOURCE_LOCATION} resource or as a bean.
 *
 * <p>Using this interface on a registered bean will cause the bean <em>and</em>
 * all of its dependencies to be initialized during AOT processing. We generally
 * recommend that this interface is only used with infrastructure beans such as
 * {@link BeanPostProcessor} which have limited dependencies and are already
 * initialized early in the bean factory lifecycle. If such a bean is registered
 * using a factory method, make sure to make it {@code static} so that its
 * enclosing class does not have to be initialized.
 *
 * <p>An AOT processor replaces its usual runtime behavior by an optimized
 * arrangement, usually in generated code. For that reason, a component that
 * implements this interface is not contributed by default. If a component that
 * implements this interface still needs to be invoked at runtime,
 * {@link #isBeanExcludedFromAotProcessing} can be overridden.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see BeanRegistrationAotContribution
 * @since 4.0
 */
@FunctionalInterface
public interface BeanRegistrationAotProcessor {

  /**
   * Process the given {@link RegisteredBean} instance ahead-of-time and
   * return a contribution or {@code null}.
   * <p>
   * Processors are free to use any techniques they like to analyze the given
   * instance. Most typically use reflection to find fields or methods to use
   * in the contribution. Contributions typically generate source code or
   * resource files that can be used when the AOT optimized application runs.
   * <p>
   * If the given instance isn't relevant to the processor, it should return a
   * {@code null} contribution.
   *
   * @param registeredBean the registered bean to process
   * @return a {@link BeanRegistrationAotContribution} or {@code null}
   */
  @Nullable
  BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean);

  /**
   * Return if the bean instance associated with this processor should be
   * excluded from AOT processing itself. By default, this method returns
   * {@code true} to automatically exclude the bean, if the definition should
   * be written then this method may be overridden to return {@code true}.
   *
   * @return if the bean should be excluded from AOT processing
   * @see BeanRegistrationExcludeFilter
   */
  default boolean isBeanExcludedFromAotProcessing() {
    return true;
  }

}
