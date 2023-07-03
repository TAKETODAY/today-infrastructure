/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.ImportAutoConfiguration;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.stereotype.Component;
import cn.taketoday.test.util.TestPropertyValues;

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
