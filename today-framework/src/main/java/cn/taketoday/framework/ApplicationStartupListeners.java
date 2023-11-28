/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;

/**
 * dispatch events to {@link ApplicationStartupListener}
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @author Brian Clozel
 * @author Chris Bono
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 20:01
 */
class ApplicationStartupListeners {

  private final Logger log;
  private final ArrayList<ApplicationStartupListener> listeners;

  ApplicationStartupListeners(Logger log, Collection<? extends ApplicationStartupListener> listeners) {
    this.log = log;
    this.listeners = new ArrayList<>(listeners);
  }

  void starting(ConfigurableBootstrapContext bootstrapContext,
          @Nullable Class<?> mainApplicationClass, ApplicationArguments arguments) {
    for (ApplicationStartupListener listener : listeners) {
      listener.starting(bootstrapContext, mainApplicationClass, arguments);
    }
  }

  void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
    for (ApplicationStartupListener listener : listeners) {
      listener.environmentPrepared(bootstrapContext, environment);
    }
  }

  void contextPrepared(ConfigurableApplicationContext context) {
    for (ApplicationStartupListener listener : listeners) {
      listener.contextPrepared(context);
    }
  }

  void contextLoaded(ConfigurableApplicationContext context) {
    for (ApplicationStartupListener listener : listeners) {
      listener.contextLoaded(context);
    }
  }

  void started(ConfigurableApplicationContext context, Duration timeTaken) {
    for (ApplicationStartupListener listener : listeners) {
      listener.started(context, timeTaken);
    }
  }

  void ready(ConfigurableApplicationContext context, Duration timeTaken) {
    for (ApplicationStartupListener listener : listeners) {
      listener.ready(context, timeTaken);
    }
  }

  void failed(@Nullable ConfigurableApplicationContext context, Throwable exception) {
    for (ApplicationStartupListener listener : listeners) {
      callFailedListener(listener, context, exception);
    }
  }

  private void callFailedListener(ApplicationStartupListener listener,
          @Nullable ConfigurableApplicationContext context, Throwable exception) {
    try {
      listener.failed(context, exception);
    }
    catch (Throwable ex) {
      if (log.isDebugEnabled()) {
        log.error("Error handling failed", ex);
      }
      else {
        String message = ex.getMessage();
        if (message == null) {
          message = "no error message";
        }
        log.warn("Error handling failed ({})", message);
      }
    }
  }

}
