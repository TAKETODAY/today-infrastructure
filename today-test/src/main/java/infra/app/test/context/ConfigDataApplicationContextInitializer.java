/*
 * Copyright 2017 - 2024 the original author or authors.
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
