/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 20:01
 */
class ApplicationStartupListeners {

  private final Logger log;
  private final List<ApplicationStartupListener> listeners;

  ApplicationStartupListeners(Logger log, Collection<? extends ApplicationStartupListener> listeners) {
    this.log = log;
    this.listeners = new ArrayList<>(listeners);
  }

  void starting(Class<?> mainApplicationClass) {
    doWithListeners(ApplicationStartupListener::starting);
  }

  void environmentPrepared(ConfigurableEnvironment environment) {
    doWithListeners(listener -> listener.environmentPrepared(environment));
  }

  void contextPrepared(ConfigurableApplicationContext context) {
    doWithListeners(listener -> listener.contextPrepared(context));
  }

  void contextLoaded(ConfigurableApplicationContext context) {
    doWithListeners(listener -> listener.contextLoaded(context));
  }

  void started(ConfigurableApplicationContext context, Duration timeTaken) {
    doWithListeners(listener -> listener.started(context, timeTaken));
  }

  void ready(ConfigurableApplicationContext context, Duration timeTaken) {
    doWithListeners(listener -> listener.ready(context, timeTaken));
  }

  void failed(ConfigurableApplicationContext context, Throwable exception) {
    doWithListeners(listener -> callFailedListener(listener, context, exception));
  }

  private void callFailedListener(
          ApplicationStartupListener listener,
          ConfigurableApplicationContext context, Throwable exception) {
    try {
      listener.failed(context, exception);
    }
    catch (Throwable ex) {
      if (exception == null) {
        ReflectionUtils.rethrowRuntimeException(ex);
      }
      if (this.log.isDebugEnabled()) {
        this.log.error("Error handling failed", ex);
      }
      else {
        String message = ex.getMessage();
        message = (message != null) ? message : "no error message";
        this.log.warn("Error handling failed ({})", message);
      }
    }
  }

  private void doWithListeners(Consumer<ApplicationStartupListener> listenerAction) {
    this.listeners.forEach(listenerAction);
  }

}
