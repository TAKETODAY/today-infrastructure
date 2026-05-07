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

package infra.docker.compose.lifecycle;

import java.util.Set;

import infra.app.Application;
import infra.app.ApplicationShutdownHandlers;
import infra.app.context.event.ApplicationPreparedEvent;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.context.properties.bind.Binder;

/**
 * {@link ApplicationListener} used to set up a {@link DockerComposeLifecycleManager}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposeListener implements ApplicationListener<ApplicationPreparedEvent> {

  private final ApplicationShutdownHandlers shutdownHandlers;

  DockerComposeListener() {
    this(Application.getShutdownHandlers());
  }

  DockerComposeListener(ApplicationShutdownHandlers shutdownHandlers) {
    this.shutdownHandlers = shutdownHandlers;
  }

  @Override
  public void onApplicationEvent(ApplicationPreparedEvent event) {
    ConfigurableApplicationContext applicationContext = event.getApplicationContext();
    Binder binder = Binder.get(applicationContext.getEnvironment());
    DockerComposeProperties properties = DockerComposeProperties.get(binder);
    Set<ApplicationListener<?>> eventListeners = event.getApplication().getListeners();
    createDockerComposeLifecycleManager(applicationContext, binder, properties, eventListeners).start();
  }

  protected DockerComposeLifecycleManager createDockerComposeLifecycleManager(
          ConfigurableApplicationContext applicationContext, Binder binder, DockerComposeProperties properties,
          Set<ApplicationListener<?>> eventListeners) {
    return new DockerComposeLifecycleManager(applicationContext, this.shutdownHandlers, properties, eventListeners);
  }

}
