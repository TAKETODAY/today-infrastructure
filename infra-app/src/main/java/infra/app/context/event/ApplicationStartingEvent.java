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
import infra.app.ConfigurableBootstrapContext;
import infra.context.ApplicationContext;
import infra.context.ApplicationListener;
import infra.core.env.Environment;

/**
 * Event published as early as conceivably possible as soon as a {@link Application}
 * has been started - before the {@link Environment} or {@link ApplicationContext} is
 * available, but after the {@link ApplicationListener}s have been registered. The source
 * of the event is the {@link Application} itself, but beware of using its internal
 * state too much at this early stage since it might be modified later in the lifecycle.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ApplicationStartingEvent extends ApplicationStartupEvent {

  private final ConfigurableBootstrapContext bootstrapContext;

  /**
   * Create a new {@link ApplicationStartingEvent} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param application the current application
   * @param args the arguments the application is running with
   */
  public ApplicationStartingEvent(ConfigurableBootstrapContext bootstrapContext,
          Application application, ApplicationArguments args) {
    super(application, args);
    this.bootstrapContext = bootstrapContext;
  }

  /**
   * Return the bootstrap context.
   *
   * @return the bootstrap context
   */
  public ConfigurableBootstrapContext getBootstrapContext() {
    return this.bootstrapContext;
  }

}
