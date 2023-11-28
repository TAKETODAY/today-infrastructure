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

package cn.taketoday.framework.context.event;

import java.time.Duration;

import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.event.ApplicationEventMulticaster;
import cn.taketoday.context.event.SimpleApplicationEventMulticaster;
import cn.taketoday.context.support.AbstractApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationArguments;
import cn.taketoday.framework.ApplicationStartupListener;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.framework.availability.AvailabilityChangeEvent;
import cn.taketoday.framework.availability.LivenessState;
import cn.taketoday.framework.availability.ReadinessState;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ErrorHandler;

/**
 * {@link ApplicationStartupListener} to publish {@link ApplicationStartupEvent}s.
 * <p>
 * Uses an internal {@link ApplicationEventMulticaster} for the events that are fired
 * before the context is actually refreshed.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @author Brian Clozel
 * @author Chris Bono
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class EventPublishingStartupListener implements ApplicationStartupListener, Ordered, ErrorHandler {
  private volatile Logger logger;

  private final Application application;

  private final ApplicationArguments args;

  private final SimpleApplicationEventMulticaster initialMulticaster;

  public EventPublishingStartupListener(Application application, ApplicationArguments args) {
    this.args = args;
    this.application = application;
    this.initialMulticaster = new SimpleApplicationEventMulticaster();
  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE;
  }

  @Override
  public void starting(ConfigurableBootstrapContext bootstrapContext, Class<?> mainApplicationClass, ApplicationArguments arguments) {
    multicastInitialEvent(new ApplicationStartingEvent(bootstrapContext, application, args));
  }

  @Override
  public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
    multicastInitialEvent(
            new ApplicationEnvironmentPreparedEvent(bootstrapContext, application, args, environment));
  }

  @Override
  public void contextPrepared(ConfigurableApplicationContext context) {
    multicastInitialEvent(new ApplicationContextInitializedEvent(application, args, context));
  }

  @Override
  public void contextLoaded(ConfigurableApplicationContext context) {
    for (ApplicationListener<?> listener : application.getListeners()) {
      if (listener instanceof ApplicationContextAware contextAware) {
        contextAware.setApplicationContext(context);
      }
      context.addApplicationListener(listener);
    }
    multicastInitialEvent(new ApplicationPreparedEvent(application, args, context));
  }

  @Override
  public void started(ConfigurableApplicationContext context, @Nullable Duration timeTaken) {
    context.publishEvent(new ApplicationStartedEvent(application, args, context, timeTaken));
    AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
  }

  @Override
  public void ready(ConfigurableApplicationContext context, @Nullable Duration timeTaken) {
    context.publishEvent(new ApplicationReadyEvent(application, args, context, timeTaken));
    AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
  }

  @Override
  public void failed(@Nullable ConfigurableApplicationContext context, Throwable exception) {
    ApplicationFailedEvent event = new ApplicationFailedEvent(application, args, context, exception);
    if (context != null && context.isActive()) {
      // Listeners have been registered to the application context, so we should
      // use it at this point if we can
      context.publishEvent(event);
    }
    else {
      // An inactive context may not have a multicaster so we use our multicaster to
      // call all the context's listeners instead
      if (context instanceof AbstractApplicationContext aaCtx) {
        for (ApplicationListener<?> listener : aaCtx.getApplicationListeners()) {
          initialMulticaster.addApplicationListener(listener);
        }
      }
      initialMulticaster.setErrorHandler(this);
      initialMulticaster.multicastEvent(event);
    }
  }

  @Override
  public void handleError(Throwable throwable) {
    Logger logger = this.logger;
    if (logger == null) {
      synchronized(this) {
        logger = this.logger;
        if (logger == null) {
          logger = LoggerFactory.getLogger(EventPublishingStartupListener.class);
          this.logger = logger;
        }
      }
    }
    logger.warn("Error calling ApplicationEventListener", throwable);
  }

  private void multicastInitialEvent(ApplicationEvent event) {
    refreshApplicationListeners();
    initialMulticaster.multicastEvent(event);
  }

  private void refreshApplicationListeners() {
    for (ApplicationListener<?> listener : application.getListeners()) {
      initialMulticaster.addApplicationListener(listener);
    }
  }

}
