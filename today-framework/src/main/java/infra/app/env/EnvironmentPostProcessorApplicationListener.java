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

package infra.app.env;

import java.util.List;

import infra.app.Application;
import infra.app.BootstrapContext;
import infra.app.BootstrapRegistry;
import infra.app.ConfigurableBootstrapContext;
import infra.app.context.event.ApplicationEnvironmentPreparedEvent;
import infra.context.ApplicationEvent;
import infra.context.event.SmartApplicationListener;
import infra.core.Ordered;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.ResourceLoader;
import infra.lang.Nullable;
import infra.lang.TodayStrategies;
import infra.util.Instantiator;

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
