/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
