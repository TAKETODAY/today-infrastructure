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

package infra.web.server.context;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.beans.factory.support.BeanNameGenerator;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.ScopeMetadata;
import infra.core.env.MapPropertySource;
import infra.core.env.StandardEnvironment;
import infra.web.server.GenericWebServerFactory;
import infra.web.server.MockWebServerFactory;
import infra.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import infra.web.server.config.WebServerConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnnotationConfigWebServerApplicationContextTests {

  @Test
  void createFromConfigurationClass() {
    try (AnnotationConfigWebServerApplicationContext context =
            new AnnotationConfigWebServerApplicationContext(TestWebServerConfiguration.class)) {
      assertThat(context.getBean(GenericWebServerFactory.class)).isInstanceOf(MockWebServerFactory.class);
      assertThat(context.getWebServer()).isNotNull();
    }
  }

  @Test
  void registerAndRefresh() {
    try (AnnotationConfigWebServerApplicationContext context = new AnnotationConfigWebServerApplicationContext()) {
      context.register(TestWebServerConfiguration.class);
      context.refresh();

      assertThat(context.getBean(TestWebServerConfiguration.class)).isNotNull();
    }
  }

  @Test
  void scanAndRefresh() {
    try (AnnotationConfigWebServerApplicationContext context = new AnnotationConfigWebServerApplicationContext()) {
      context.scan(getClass().getPackageName());
      context.refresh();

      assertThat(context.getBean(GenericWebServerFactory.class)).isInstanceOf(MockWebServerFactory.class);
    }
  }

  @Test
  void createFromBasePackage() {
    try (AnnotationConfigWebServerApplicationContext context =
            new AnnotationConfigWebServerApplicationContext(getClass().getPackageName())) {
      assertThat(context.getBean(GenericWebServerFactory.class)).isInstanceOf(MockWebServerFactory.class);
    }
  }

  @Test
  void webServerConfigurationAppliesConfiguredPortToServer() {
    try (AnnotationConfigWebServerApplicationContext context = new AnnotationConfigWebServerApplicationContext()) {
      context.getEnvironment().getPropertySources()
              .addFirst(new MapPropertySource("test", Map.of("server.port", "9123")));
      context.register(WebServerConfiguration.class, TestWebServerConfiguration.class);
      context.refresh();

      assertThat(context.getWebServer()).isNotNull();
      assertThat(context.getWebServer().getPort()).isEqualTo(9123);
    }
  }

  @Test
  void webServerConfigurationReusesExistingCustomizerPostProcessor() {
    try (AnnotationConfigWebServerApplicationContext context = new AnnotationConfigWebServerApplicationContext()) {
      context.registerBean("existingCustomizer", WebServerFactoryCustomizerBeanPostProcessor.class);
      context.register(WebServerConfiguration.class, TestWebServerConfiguration.class);
      context.refresh();

      assertThat(context.getBeansOfType(WebServerFactoryCustomizerBeanPostProcessor.class))
              .containsOnlyKeys("existingCustomizer");
    }
  }

  @Test
  void shouldCreateContextWithBeanFactory() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();

    AnnotationConfigWebServerApplicationContext context = new AnnotationConfigWebServerApplicationContext(beanFactory);

    assertThat(context.getBeanFactory()).isSameAs(beanFactory);
  }

  @Test
  void shouldSetEnvironment() {
    AnnotationConfigWebServerApplicationContext context = new AnnotationConfigWebServerApplicationContext();
    StandardEnvironment environment = new StandardEnvironment();

    context.setEnvironment(environment);

    assertThat(context.getEnvironment()).isSameAs(environment);
  }

  @Test
  void shouldSetBeanNameGenerator() {
    try (AnnotationConfigWebServerApplicationContext context = new AnnotationConfigWebServerApplicationContext()) {
      BeanNameGenerator beanNameGenerator = (definition, registry) -> "customConfiguration";
      context.setBeanNameGenerator(beanNameGenerator);
      context.register(TestWebServerConfiguration.class);
      context.refresh();

      assertThat(context.getBean("customConfiguration")).isInstanceOf(TestWebServerConfiguration.class);
    }
  }

  @Test
  void shouldSetScopeMetadataResolver() {
    try (AnnotationConfigWebServerApplicationContext context = new AnnotationConfigWebServerApplicationContext()) {
      context.setScopeMetadataResolver(definition -> {
        ScopeMetadata metadata = new ScopeMetadata();
        metadata.setScopeName("prototype");
        return metadata;
      });
      context.register(TestWebServerConfiguration.class);
      context.refresh();

      assertThat(context.getBean(TestWebServerConfiguration.class))
              .isNotSameAs(context.getBean(TestWebServerConfiguration.class));
    }
  }

  @Test
  void shouldRejectEmptyRegistrationAndScan() {
    AnnotationConfigWebServerApplicationContext context = new AnnotationConfigWebServerApplicationContext();

    assertThatThrownBy(() -> context.register(new Class<?>[0]))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one annotated class must be specified");
    assertThatThrownBy(() -> context.scan())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one base package must be specified");
  }

  @Configuration(proxyBeanMethods = false)
  static class TestWebServerConfiguration {

    @Bean
    GenericWebServerFactory webServerFactory() {
      return new MockWebServerFactory();
    }

  }

}
