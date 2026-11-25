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

package infra.beans.factory.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import infra.beans.factory.support.StandardBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CustomScopeConfigurerBehaviorTests {

  @Test
  void scopeDefaultsToNoConversationId() {
    var scope = org.mockito.Mockito.mock(Scope.class, org.mockito.Answers.CALLS_REAL_METHODS);
    assertThat(scope.getConversationId()).isNull();
  }

  @Test
  void registersScopeInstancesClassesAndClassNames() {
    StandardBeanFactory factory = new StandardBeanFactory();
    CustomScopeConfigurer configurer = new CustomScopeConfigurer();
    RecordingScope instance = new RecordingScope();
    Map<String, Object> scopes = new LinkedHashMap<>();
    scopes.put("instance", instance);
    scopes.put("class", RecordingScope.class);
    scopes.put("name", RecordingScope.class.getName());
    configurer.setScopes(scopes);
    configurer.postProcessBeanFactory(factory);

    assertThat(factory.getRegisteredScope("instance")).isSameAs(instance);
    assertThat(factory.getRegisteredScope("class")).isInstanceOf(RecordingScope.class);
    assertThat(factory.getRegisteredScope("name")).isInstanceOf(RecordingScope.class);
    assertThat(factory.getRegisteredScopeNames()).containsExactlyInAnyOrder("instance", "class", "name");
  }

  @Test
  void addScopeCreatesConfigurationAndRejectsInvalidMappings() {
    StandardBeanFactory factory = new StandardBeanFactory();
    CustomScopeConfigurer configurer = new CustomScopeConfigurer();
    RecordingScope scope = new RecordingScope();
    configurer.addScope("custom", scope);
    configurer.setBeanClassLoader(getClass().getClassLoader());
    configurer.postProcessBeanFactory(factory);
    assertThat(factory.getRegisteredScope("custom")).isSameAs(scope);

    CustomScopeConfigurer invalid = new CustomScopeConfigurer();
    invalid.setScopes(Map.of("invalid", String.class));
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> invalid.postProcessBeanFactory(new StandardBeanFactory()));

    CustomScopeConfigurer unsupported = new CustomScopeConfigurer();
    unsupported.setScopes(Map.of("invalid", 42));
    assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> unsupported.postProcessBeanFactory(new StandardBeanFactory()));
  }

  @Test
  void protectsStandardScopes() {
    StandardBeanFactory factory = new StandardBeanFactory();
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> factory.registerScope(Scope.SINGLETON, new RecordingScope()));
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> factory.registerScope(Scope.PROTOTYPE, new RecordingScope()));
  }

  static class RecordingScope implements Scope {

    @Override
    public Object get(String name, java.util.function.Supplier<?> objectFactory) {
      return objectFactory.get();
    }

    @Override
    public Object remove(String name) {
      return null;
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) { }

    @Override
    public Object resolveContextualObject(String key) {
      return null;
    }

    @Override
    public String getConversationId() {
      return "conversation";
    }
  }

}
