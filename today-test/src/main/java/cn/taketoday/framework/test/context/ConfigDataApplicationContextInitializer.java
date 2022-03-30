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

package cn.taketoday.framework.test.context;

import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.framework.DefaultBootstrapContext;
import cn.taketoday.framework.DefaultPropertiesPropertySource;
import cn.taketoday.framework.context.config.ConfigData;
import cn.taketoday.framework.context.config.ConfigDataEnvironmentPostProcessor;
import cn.taketoday.framework.env.RandomValuePropertySource;
import cn.taketoday.test.context.ContextConfiguration;

/**
 * {@link ApplicationContextInitializer} that can be used with the
 * {@link ContextConfiguration#initializers()} to trigger loading of {@link ConfigData}
 * such as {@literal application.properties}.
 *
 * @author Phillip Webb
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
