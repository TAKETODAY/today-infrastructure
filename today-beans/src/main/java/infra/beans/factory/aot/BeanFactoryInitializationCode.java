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

package infra.beans.factory.aot;

import infra.aot.generate.GeneratedMethods;
import infra.aot.generate.MethodReference;
import infra.javapoet.ClassName;

/**
 * Interface that can be used to configure the code that will be generated to
 * perform bean factory initialization.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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
   * Return the name of the class used by the initializing code.
   *
   * @return the generated class name
   * @since 5.0
   */
  ClassName getClassName();

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
