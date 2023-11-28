/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.event.SmartApplicationListener;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.BootstrapContext;
import cn.taketoday.framework.BootstrapRegistry;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.framework.context.event.ApplicationEnvironmentPreparedEvent;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.Instantiator;

/**
 * {@link SmartApplicationListener} used to trigger {@link EnvironmentPostProcessor
 * EnvironmentPostProcessors} registered in the {@code today.strategies} file.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/3 00:32
 */
public class EnvironmentPostProcessorApplicationListener implements SmartApplicationListener, Ordered {

  /**
   * The default order for the processor.
   */
  public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

  private int order = DEFAULT_ORDER;

  @Override
  public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
    return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType);
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof ApplicationEnvironmentPreparedEvent e) {
      Application application = e.getApplication();
      ConfigurableEnvironment environment = e.getEnvironment();
      ResourceLoader resourceLoader = application.getResourceLoader();
      for (var postProcessor : getPostProcessors(resourceLoader, e.getBootstrapContext())) {
        postProcessor.postProcessEnvironment(environment, application);
      }
    }
  }

  List<EnvironmentPostProcessor> getPostProcessors(@Nullable ResourceLoader resourceLoader, ConfigurableBootstrapContext bootstrapContext) {
    ClassLoader classLoader = resourceLoader != null ? resourceLoader.getClassLoader() : null;

    Instantiator<EnvironmentPostProcessor> instantiator = new Instantiator<>(EnvironmentPostProcessor.class,
            parameters -> {
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
