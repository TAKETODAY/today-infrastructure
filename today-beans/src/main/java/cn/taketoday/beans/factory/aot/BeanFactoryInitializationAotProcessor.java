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

import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.lang.Nullable;

/**
 * AOT processor that makes bean factory initialization contributions by
 * processing {@link ConfigurableBeanFactory} instances.
 *
 * <p>{@code BeanFactoryInitializationAotProcessor} implementations may be
 * registered in a {@value AotServices#FACTORIES_RESOURCE_LOCATION} resource or
 * as a bean.
 *
 * <p>Using this interface on a registered bean will cause the bean <em>and</em>
 * all of its dependencies to be initialized during AOT processing. We generally
 * recommend that this interface is only used with infrastructure beans such as
 * {@link BeanFactoryPostProcessor} which have limited dependencies and are
 * already initialized early in the bean factory lifecycle. If such a bean is
 * registered using a factory method, make sure to make it {@code static} so
 * that its enclosing class does not have to be initialized.
 *
 * <p>A component that implements this interface is not contributed.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanFactoryInitializationAotContribution
 * @since 4.0
 */
@FunctionalInterface
public interface BeanFactoryInitializationAotProcessor {

  /**
   * Process the given {@link ConfigurableBeanFactory} instance
   * ahead-of-time and return a contribution or {@code null}.
   * <p>Processors are free to use any techniques they like to analyze the given
   * bean factory. Most typically use reflection to find fields or methods to
   * use in the contribution. Contributions typically generate source code or
   * resource files that can be used when the AOT optimized application runs.
   * <p>If the given bean factory does not contain anything that is relevant to
   * the processor, this method should return a {@code null} contribution.
   *
   * @param beanFactory the bean factory to process
   * @return a {@link BeanFactoryInitializationAotContribution} or {@code null}
   */
  @Nullable
  BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory);

}
