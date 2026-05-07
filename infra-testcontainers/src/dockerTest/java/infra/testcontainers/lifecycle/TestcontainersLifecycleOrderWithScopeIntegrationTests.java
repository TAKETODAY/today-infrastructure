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

package infra.testcontainers.lifecycle;

import com.redis.testcontainers.RedisContainer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Scope;
import infra.context.support.GenericApplicationContext;
import infra.test.annotation.DirtiesContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.support.AnnotationConfigContextLoader;
import infra.test.testcontainers.DisabledIfDockerUnavailable;
import infra.test.testcontainers.TestImage;
import infra.testcontainers.lifecycle.TestcontainersLifecycleOrderWithScopeIntegrationTests.AssertingInfraExtension;
import infra.testcontainers.lifecycle.TestcontainersLifecycleOrderWithScopeIntegrationTests.ContainerConfig;
import infra.testcontainers.lifecycle.TestcontainersLifecycleOrderWithScopeIntegrationTests.ScopedContextLoader;
import infra.testcontainers.lifecycle.TestcontainersLifecycleOrderWithScopeIntegrationTests.TestConfig;
import infra.testcontainers.service.connection.ServiceConnection;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestcontainersLifecycleBeanPostProcessor} to ensure create
 * and destroy events happen in the correct order.
 *
 * @author Phillip Webb
 */
@ExtendWith(AssertingInfraExtension.class)
@ContextConfiguration(loader = ScopedContextLoader.class, classes = { TestConfig.class, ContainerConfig.class })
@DirtiesContext
@DisabledIfDockerUnavailable
class TestcontainersLifecycleOrderWithScopeIntegrationTests {

  static List<String> events = Collections.synchronizedList(new ArrayList<>());

  @Test
  void eventsAreOrderedCorrectlyAfterStartup() {
    assertThat(events).containsExactly("start-container", "create-bean");
  }

  @Configuration(proxyBeanMethods = false)
  static class ContainerConfig {

    @Bean
    @Scope("custom")
    @ServiceConnection
    RedisContainer redisContainer() {
      return TestImage.container(EventRecordingRedisContainer.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfig {

    @Bean
    TestBean testBean() {
      events.add("create-bean");
      return new TestBean();
    }

  }

  static class TestBean implements AutoCloseable {

    @Override
    public void close() throws Exception {
      events.add("destroy-bean");
    }

  }

  static class AssertingInfraExtension extends InfraExtension {

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
      super.afterAll(context);
      assertThat(events).containsExactly("start-container", "create-bean", "destroy-bean", "stop-container");
    }

  }

  static class EventRecordingRedisContainer extends RedisContainer {

    EventRecordingRedisContainer(DockerImageName dockerImageName) {
      super(dockerImageName);
    }

    @Override
    public void start() {
      events.add("start-container");
      super.start();
    }

    @Override
    public void stop() {
      events.add("stop-container");
      super.stop();
    }

  }

  static class ScopedContextLoader extends AnnotationConfigContextLoader {

    @Override
    protected GenericApplicationContext createContext() {
      CustomScope customScope = new CustomScope();
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext() {

        @Override
        protected void onClose() {
          customScope.destroy();
          super.onClose();
        }

      };
      context.getBeanFactory().registerScope("custom", customScope);
      return context;
    }

  }

  static class CustomScope implements infra.beans.factory.config.Scope {

    private Map<String, Object> instances = new HashMap<>();

    private MultiValueMap<String, Runnable> destructors = new LinkedMultiValueMap<>();

    @Override
    public Object get(String name, Supplier<?> objectFactory) {
      return this.instances.computeIfAbsent(name, (key) -> objectFactory.get());
    }

    @Override
    public Object remove(String name) {
      synchronized(this) {
        Object removed = this.instances.remove(name);
        List<Runnable> destructor = this.destructors.get(name);
        assertThat(destructor).isNotNull();
        destructor.forEach(Runnable::run);
        this.destructors.remove(name);
        return removed;
      }
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
      this.destructors.add(name, callback);
    }

    @Override
    public @Nullable Object resolveContextualObject(String key) {
      return null;
    }

    @Override
    public @Nullable String getConversationId() {
      return null;
    }

    void destroy() {
      synchronized(this) {
        this.destructors.forEach((name, actions) -> actions.forEach(Runnable::run));
        this.destructors.clear();
        this.instances.clear();
      }
    }

  }

}
