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

package cn.taketoday.framework.env;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.framework.Application;
import cn.taketoday.logging.Logger;

/**
 * {@link EnvironmentPostProcessor} to add the {@link RandomValuePropertySource}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class RandomValuePropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  /**
   * The default order of this post-processor.
   */
  public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 1;

  private final Logger logger;

  /**
   * Create a new {@link RandomValuePropertySourceEnvironmentPostProcessor} instance.
   *
   * @param logger the logger to use
   */
  public RandomValuePropertySourceEnvironmentPostProcessor(Logger logger) {
    this.logger = logger;
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, Application application) {
    RandomValuePropertySource.addToEnvironment(environment, this.logger);
  }

}
