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
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;

/**
 * Event published when a {@link Application} is starting up and the
 * {@link Environment} is first available for inspection and modification.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ApplicationEnvironmentPreparedEvent extends ApplicationStartupEvent {

  private final ConfigurableBootstrapContext bootstrapContext;

  private final ConfigurableEnvironment environment;

  /**
   * Create a new {@link ApplicationEnvironmentPreparedEvent} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param application the current application
   * @param args the arguments the application is running with
   * @param environment the environment that was just created
   */
  public ApplicationEnvironmentPreparedEvent(ConfigurableBootstrapContext bootstrapContext,
          Application application, ApplicationArguments args, ConfigurableEnvironment environment) {
    super(application, args);
    this.bootstrapContext = bootstrapContext;
    this.environment = environment;
  }

  /**
   * Return the bootstrap context.
   *
   * @return the bootstrap context
   */
  public ConfigurableBootstrapContext getBootstrapContext() {
    return this.bootstrapContext;
  }

  /**
   * Return the environment.
   *
   * @return the environment
   */
  public ConfigurableEnvironment getEnvironment() {
    return this.environment;
  }

}
