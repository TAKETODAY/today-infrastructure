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

package infra.test.context.support;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryInitializer;
import infra.beans.factory.BeanFactoryUtils;
import infra.context.EnvironmentAware;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.DynamicPropertyRegistrar;
import infra.test.context.DynamicPropertyRegistry;

/**
 * {@link BeanFactoryInitializer} that eagerly initializes {@link DynamicPropertyRegistrar}
 * beans.
 *
 * <p>Primarily intended for internal use within the Infra TestContext Framework.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class DynamicPropertyRegistrarBeanInitializer implements BeanFactoryInitializer<BeanFactory>, EnvironmentAware {

  private static final Logger logger = LoggerFactory.getLogger(DynamicPropertyRegistrarBeanInitializer.class);

  /**
   * The bean name of the internally managed {@code DynamicPropertyRegistrarBeanInitializer}.
   */
  static final String BEAN_NAME =
          "infra.test.context.support.internalDynamicPropertyRegistrarBeanInitializer";

  private @Nullable ConfigurableEnvironment environment;

  @Override
  public void setEnvironment(Environment environment) {
    if (!(environment instanceof ConfigurableEnvironment configurableEnvironment)) {
      throw new IllegalArgumentException("Environment must be a ConfigurableEnvironment");
    }
    this.environment = configurableEnvironment;
  }

  @Override
  public void initialize(BeanFactory beanFactory) {
    if (this.environment == null) {
      throw new IllegalStateException("Environment is required");
    }
    String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
            beanFactory, DynamicPropertyRegistrar.class);
    if (beanNames.length > 0) {
      DynamicValuesPropertySource propertySource = DynamicValuesPropertySource.getOrCreate(this.environment);
      DynamicPropertyRegistry registry = propertySource.dynamicPropertyRegistry;
      for (String name : beanNames) {
        if (logger.isDebugEnabled()) {
          logger.debug("Eagerly initializing DynamicPropertyRegistrar bean '{}'", name);
        }
        DynamicPropertyRegistrar registrar = beanFactory.getBean(name, DynamicPropertyRegistrar.class);
        registrar.accept(registry);
      }
    }
  }

}
