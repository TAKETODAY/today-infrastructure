/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.test.context;

import infra.app.DefaultBootstrapContext;
import infra.app.DefaultPropertiesPropertySource;
import infra.app.context.config.ConfigData;
import infra.app.context.config.ConfigDataEnvironmentPostProcessor;
import infra.app.env.RandomValuePropertySource;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.test.context.ContextConfiguration;

/**
 * {@link ApplicationContextInitializer} that can be used with the
 * {@link ContextConfiguration#initializers()} to trigger loading of {@link ConfigData}
 * such as {@literal application.properties}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ConfigDataEnvironmentPostProcessor
 * @since 4.0
 */
public class ConfigDataApplicationContextInitializer implements ApplicationContextInitializer {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    ConfigurableEnvironment environment = applicationContext.getEnvironment();
    RandomValuePropertySource.addToEnvironment(environment);
    DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();
    ConfigDataEnvironmentPostProcessor.applyTo(environment, applicationContext, bootstrapContext);
    bootstrapContext.close(applicationContext);
    DefaultPropertiesPropertySource.moveToEnd(environment);
  }

}
