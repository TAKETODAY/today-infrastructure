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

import java.util.List;

import cn.taketoday.context.event.SmartApplicationListener;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationStartupListener;
import cn.taketoday.framework.BootstrapContext;
import cn.taketoday.framework.BootstrapRegistry;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.Instantiator;

/**
 * {@link SmartApplicationListener} used to trigger {@link EnvironmentPostProcessor
 * EnvironmentPostProcessors} registered in the {@code spring.factories} file.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/3 00:32
 */
public class EnvironmentPostProcessorApplicationStartupListener implements ApplicationStartupListener, Ordered {

  /**
   * The default order for the processor.
   */
  public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

  private int order = DEFAULT_ORDER;
  private final Logger applicationLog;

  public EnvironmentPostProcessorApplicationStartupListener(Logger applicationLog) {
    this.applicationLog = applicationLog;
  }

  @Override
  public void environmentPrepared(ConfigurableBootstrapContext context, ConfigurableEnvironment environment) {
    Application application = context.get(Application.class);
    for (var postProcessor : getEnvironmentPostProcessors(application.getResourceLoader(), context)) {
      postProcessor.postProcessEnvironment(environment, application);
    }
  }

  List<EnvironmentPostProcessor> getEnvironmentPostProcessors(
          ResourceLoader resourceLoader, ConfigurableBootstrapContext bootstrapContext) {
    ClassLoader classLoader = resourceLoader != null ? resourceLoader.getClassLoader() : null;

    Instantiator<EnvironmentPostProcessor> instantiator = new Instantiator<>(EnvironmentPostProcessor.class,
            parameters -> {
              parameters.add(Logger.class, applicationLog);
              parameters.add(BootstrapContext.class, bootstrapContext);
              parameters.add(BootstrapRegistry.class, bootstrapContext);
              parameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
            });

    List<String> strategiesNames = TodayStrategies.findNames(EnvironmentPostProcessor.class, classLoader);
    return instantiator.instantiate(strategiesNames);
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

}
