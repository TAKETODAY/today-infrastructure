/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.context.event;

import infra.app.Application;
import infra.app.ApplicationArguments;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;

/**
 * Event published when a {@link Application} is starting up and the
 * {@link ApplicationContext} is prepared and ApplicationContextInitializers have been
 * called but before any bean definitions are loaded.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Artsiom Yudovin
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ApplicationContextInitializedEvent extends ApplicationStartupEvent {

  private final ConfigurableApplicationContext context;

  /**
   * Create a new {@link ApplicationContextInitializedEvent} instance.
   *
   * @param application the current application
   * @param args the arguments the application is running with
   * @param context the context that has been initialized
   */
  public ApplicationContextInitializedEvent(Application application, ApplicationArguments args,
          ConfigurableApplicationContext context) {
    super(application, args);
    this.context = context;
  }

  /**
   * Return the application context.
   *
   * @return the context
   */
  public ConfigurableApplicationContext getApplicationContext() {
    return this.context;
  }

}
