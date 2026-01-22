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

package infra.app.config.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.context.properties.ConfigurationProperties;
import infra.stereotype.Component;
import infra.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesAutoConfigurationTests {

  private AnnotationConfigApplicationContext context;

  @AfterEach
  void tearDown() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void processAnnotatedBean() {
    load(new Class<?>[] { AutoConfig.class, SampleBean.class }, "foo.name:test");
    assertThat(this.context.getBean(SampleBean.class).getName()).isEqualTo("test");
  }

  @Test
  void processAnnotatedBeanNoAutoConfig() {
    load(new Class<?>[] { SampleBean.class }, "foo.name:test");
    assertThat(this.context.getBean(SampleBean.class).getName()).isEqualTo("default");
  }

  private void load(Class<?>[] configs, String... environment) {
    this.context = new AnnotationConfigApplicationContext();
    this.context.register(configs);
    TestPropertyValues.of(environment).applyTo(this.context);
    this.context.refresh();
  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration(ConfigurationPropertiesAutoConfiguration.class)
  static class AutoConfig {

  }

  @Component
  @ConfigurationProperties("foo")
  static class SampleBean {

    private String name = "default";

    String getName() {
      return this.name;
    }

    void setName(String name) {
      this.name = name;
    }

  }

}
