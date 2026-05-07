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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Set;

import infra.app.Application;
import infra.app.ApplicationArguments;
import infra.app.ApplicationShutdownHandlers;
import infra.app.context.event.ApplicationPreparedEvent;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.context.properties.bind.Binder;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DockerComposeListener}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposeListenerTests {

  @Test
  void onApplicationPreparedEventCreatesAndStartsDockerComposeLifecycleManager() {
    ApplicationShutdownHandlers shutdownHandlers = mock(ApplicationShutdownHandlers.class);
    Application application = mock(Application.class);
    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    MockEnvironment environment = new MockEnvironment();
    given(context.getEnvironment()).willReturn(environment);
    TestDockerComposeListener listener = new TestDockerComposeListener(shutdownHandlers, context);
    ApplicationPreparedEvent event = new ApplicationPreparedEvent(application, new ApplicationArguments(), context);
    listener.onApplicationEvent(event);
    assertThat(listener.getManager()).isNotNull();
    then(listener.getManager()).should().start();
  }

  static class TestDockerComposeListener extends DockerComposeListener {

    private final ConfigurableApplicationContext context;

    private @Nullable DockerComposeLifecycleManager manager;

    TestDockerComposeListener(ApplicationShutdownHandlers shutdownHandlers,
            ConfigurableApplicationContext context) {
      super(shutdownHandlers);
      this.context = context;
    }

    @Override
    protected DockerComposeLifecycleManager createDockerComposeLifecycleManager(
            ConfigurableApplicationContext applicationContext, Binder binder, DockerComposeProperties properties,
            Set<ApplicationListener<?>> eventListeners) {
      this.manager = mock(DockerComposeLifecycleManager.class);
      assertThat(applicationContext).isSameAs(this.context);
      assertThat(binder).isNotNull();
      assertThat(properties).isNotNull();
      return this.manager;
    }

    @Nullable DockerComposeLifecycleManager getManager() {
      return this.manager;
    }

  }

}
