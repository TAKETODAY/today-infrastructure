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

package cn.taketoday.framework.context.event;

import java.time.Duration;

import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
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
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ErrorHandler;

/**
 * {@link ApplicationStartupListener} to publish {@link SpringApplicationEvent}s.
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
 * @since 4.0
 */
public class EventPublishingRunListener implements ApplicationStartupListener, Ordered {

  private final Application application;

  private final String[] args;

  private final SimpleApplicationEventMulticaster initialMulticaster;

  public EventPublishingRunListener(Application application, String[] args) {
    this.application = application;
    this.args = args;
    this.initialMulticaster = new SimpleApplicationEventMulticaster();
    for (ApplicationListener<?> listener : application.getListeners()) {
      this.initialMulticaster.addApplicationListener(listener);
    }
  }

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public void starting(ConfigurableBootstrapContext bootstrapContext, Class<?> mainApplicationClass, ApplicationArguments arguments) {
    this.initialMulticaster
            .multicastEvent(new ApplicationStartingEvent(bootstrapContext, this.application, this.args));
  }

  @Override
  public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
    this.initialMulticaster.multicastEvent(
            new ApplicationEnvironmentPreparedEvent(bootstrapContext, this.application, this.args, environment));
  }

  @Override
  public void contextPrepared(ConfigurableApplicationContext context) {
    this.initialMulticaster
            .multicastEvent(new ApplicationContextInitializedEvent(this.application, this.args, context));
  }

  @Override
  public void contextLoaded(ConfigurableApplicationContext context) {
    for (ApplicationListener<?> listener : this.application.getListeners()) {
      if (listener instanceof ApplicationContextAware) {
        ((ApplicationContextAware) listener).setApplicationContext(context);
      }
      context.addApplicationListener(listener);
    }
    this.initialMulticaster.multicastEvent(new ApplicationPreparedEvent(this.application, this.args, context));
  }

  @Override
  public void started(ConfigurableApplicationContext context, Duration timeTaken) {
    context.publishEvent(new ApplicationStartedEvent(this.application, this.args, context, timeTaken));
    AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
  }

  @Override
  public void ready(ConfigurableApplicationContext context, Duration timeTaken) {
    context.publishEvent(new ApplicationReadyEvent(this.application, this.args, context, timeTaken));
    AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
  }

  @Override
  public void failed(ConfigurableApplicationContext context, Throwable exception) {
    ApplicationFailedEvent event = new ApplicationFailedEvent(this.application, this.args, context, exception);
    if (context != null && context.isActive()) {
      // Listeners have been registered to the application context so we should
      // use it at this point if we can
      context.publishEvent(event);
    }
    else {
      // An inactive context may not have a multicaster so we use our multicaster to
      // call all of the context's listeners instead
      if (context instanceof AbstractApplicationContext) {
        for (ApplicationListener<?> listener : ((AbstractApplicationContext) context)
                .getApplicationListeners()) {
          this.initialMulticaster.addApplicationListener(listener);
        }
      }
      this.initialMulticaster.setErrorHandler(new LoggingErrorHandler());
      this.initialMulticaster.multicastEvent(event);
    }
  }

  private static class LoggingErrorHandler implements ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(EventPublishingRunListener.class);

    @Override
    public void handleError(Throwable throwable) {
      logger.warn("Error calling ApplicationEventListener", throwable);
    }

  }

}
