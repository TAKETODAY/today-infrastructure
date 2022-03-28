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
import java.util.function.Function;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.event.SmartApplicationListener;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.ResourceLoader;

/**
 * {@link SmartApplicationListener} used to trigger {@link EnvironmentPostProcessor
 * EnvironmentPostProcessors} registered in the {@code spring.factories} file.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class EnvironmentPostProcessorApplicationListener implements SmartApplicationListener, Ordered {

  /**
   * The default order for the processor.
   */
  public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

  private final DeferredLogs deferredLogs;

  private int order = DEFAULT_ORDER;

  private final Function<ClassLoader, EnvironmentPostProcessorsFactory> postProcessorsFactory;

  /**
   * Create a new {@link EnvironmentPostProcessorApplicationListener} with
   * {@link EnvironmentPostProcessor} classes loaded via {@code spring.factories}.
   */
  public EnvironmentPostProcessorApplicationListener() {
    this(EnvironmentPostProcessorsFactory::fromSpringFactories, new DeferredLogs());
  }

  /**
   * Create a new {@link EnvironmentPostProcessorApplicationListener} with post
   * processors created by the given factory.
   *
   * @param postProcessorsFactory the post processors factory
   */
  public EnvironmentPostProcessorApplicationListener(EnvironmentPostProcessorsFactory postProcessorsFactory) {
    this((classloader) -> postProcessorsFactory, new DeferredLogs());
  }

  EnvironmentPostProcessorApplicationListener(
          Function<ClassLoader, EnvironmentPostProcessorsFactory> postProcessorsFactory, DeferredLogs deferredLogs) {
    this.postProcessorsFactory = postProcessorsFactory;
    this.deferredLogs = deferredLogs;
  }

  @Override
  public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
    return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType)
            || ApplicationPreparedEvent.class.isAssignableFrom(eventType)
            || ApplicationFailedEvent.class.isAssignableFrom(eventType);
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof ApplicationEnvironmentPreparedEvent) {
      onApplicationEnvironmentPreparedEvent((ApplicationEnvironmentPreparedEvent) event);
    }
    if (event instanceof ApplicationPreparedEvent) {
      onApplicationPreparedEvent();
    }
    if (event instanceof ApplicationFailedEvent) {
      onApplicationFailedEvent();
    }
  }

  private void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
    ConfigurableEnvironment environment = event.getEnvironment();
    Application application = event.getApplication();
    for (EnvironmentPostProcessor postProcessor : getEnvironmentPostProcessors(application.getResourceLoader(),
            event.getBootstrapContext())) {
      postProcessor.postProcessEnvironment(environment, application);
    }
  }

  private void onApplicationPreparedEvent() {
    finish();
  }

  private void onApplicationFailedEvent() {
    finish();
  }

  private void finish() {
    this.deferredLogs.switchOverAll();
  }

  List<EnvironmentPostProcessor> getEnvironmentPostProcessors(ResourceLoader resourceLoader,
          ConfigurableBootstrapContext bootstrapContext) {
    ClassLoader classLoader = (resourceLoader != null) ? resourceLoader.getClassLoader() : null;
    EnvironmentPostProcessorsFactory postProcessorsFactory = this.postProcessorsFactory.apply(classLoader);
    return postProcessorsFactory.getEnvironmentPostProcessors(this.deferredLogs, bootstrapContext);
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

}
