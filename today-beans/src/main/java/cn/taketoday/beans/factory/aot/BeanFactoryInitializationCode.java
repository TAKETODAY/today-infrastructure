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

import cn.taketoday.aot.generate.GeneratedMethods;
import cn.taketoday.aot.generate.MethodReference;

/**
 * Interface that can be used to configure the code that will be generated to
 * perform bean factory initialization.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see BeanFactoryInitializationAotContribution
 * @since 4.0
 */
public interface BeanFactoryInitializationCode {

  /**
   * The recommended variable name to use to refer to the bean factory.
   */
  String BEAN_FACTORY_VARIABLE = "beanFactory";

  /**
   * Get the {@link GeneratedMethods} used by the initializing code.
   *
   * @return the generated methods
   */
  GeneratedMethods getMethods();

  /**
   * Add an initializer method call. An initializer can use a flexible signature,
   * using any of the following:
   * <ul>
   * <li>{@code StandardBeanFactory}, or {@code ConfigurableBeanFactory}
   * to use the bean factory.</li>
   * <li>{@code ConfigurableEnvironment} or {@code Environment} to access the
   * environment.</li>
   * <li>{@code ResourceLoader} to load resources.</li>
   * </ul>
   *
   * @param methodReference a reference to the initialize method to call.
   */
  void addInitializer(MethodReference methodReference);

}
