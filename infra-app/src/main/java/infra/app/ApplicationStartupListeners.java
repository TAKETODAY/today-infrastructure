/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.app;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

import infra.context.ConfigurableApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.logging.Logger;

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
final class ApplicationStartupListeners implements ApplicationStartupListener {

  private final Logger log;

  private final ArrayList<ApplicationStartupListener> listeners;

  ApplicationStartupListeners(Logger log, Collection<? extends ApplicationStartupListener> listeners) {
    this.log = log;
    this.listeners = new ArrayList<>(listeners);
  }

  @Override
  public void starting(ConfigurableBootstrapContext bootstrapContext,
          @Nullable Class<?> mainApplicationClass, ApplicationArguments arguments) {
    for (ApplicationStartupListener listener : listeners) {
      listener.starting(bootstrapContext, mainApplicationClass, arguments);
    }
  }

  @Override
  public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
    for (ApplicationStartupListener listener : listeners) {
      listener.environmentPrepared(bootstrapContext, environment);
    }
  }

  @Override
  public void contextPrepared(ConfigurableApplicationContext context) {
    for (ApplicationStartupListener listener : listeners) {
      listener.contextPrepared(context);
    }
  }

  @Override
  public void contextLoaded(ConfigurableApplicationContext context) {
    for (ApplicationStartupListener listener : listeners) {
      listener.contextLoaded(context);
    }
  }

  @Override
  public void started(ConfigurableApplicationContext context, @Nullable Duration timeTaken) {
    for (ApplicationStartupListener listener : listeners) {
      listener.started(context, timeTaken);
    }
  }

  @Override
  public void ready(ConfigurableApplicationContext context, @Nullable Duration timeTaken) {
    for (ApplicationStartupListener listener : listeners) {
      listener.ready(context, timeTaken);
    }
  }

  @Override
  public void failed(@Nullable ConfigurableApplicationContext context, Throwable exception) {
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
