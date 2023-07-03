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

package cn.taketoday.framework.web.servlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.annotation.AnnotationConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/25 16:51
 */
class ServletComponentScanRegistrarTests {

  private AnnotationConfigApplicationContext context;

  @AfterEach
  void after() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void packagesConfiguredWithValue() {
    this.context = new AnnotationConfigApplicationContext(ValuePackages.class);
    ServletComponentRegisteringPostProcessor postProcessor = this.context
            .getBean(ServletComponentRegisteringPostProcessor.class);
    assertThat(postProcessor.getPackagesToScan()).contains("com.example.foo", "com.example.bar");
  }

  @Test
  void packagesConfiguredWithValueAsm() {
    this.context = new AnnotationConfigApplicationContext();
    this.context.registerBeanDefinition("valuePackages", new RootBeanDefinition(ValuePackages.class.getName()));
    this.context.refresh();
    ServletComponentRegisteringPostProcessor postProcessor = this.context
            .getBean(ServletComponentRegisteringPostProcessor.class);
    assertThat(postProcessor.getPackagesToScan()).contains("com.example.foo", "com.example.bar");
  }

  @Test
  void packagesConfiguredWithBackPackages() {
    this.context = new AnnotationConfigApplicationContext(BasePackages.class);
    ServletComponentRegisteringPostProcessor postProcessor = this.context
            .getBean(ServletComponentRegisteringPostProcessor.class);
    assertThat(postProcessor.getPackagesToScan()).contains("com.example.foo", "com.example.bar");
  }

  @Test
  void packagesConfiguredWithBasePackageClasses() {
    this.context = new AnnotationConfigApplicationContext(BasePackageClasses.class);
    ServletComponentRegisteringPostProcessor postProcessor = this.context
            .getBean(ServletComponentRegisteringPostProcessor.class);
    assertThat(postProcessor.getPackagesToScan()).contains(getClass().getPackage().getName());
  }

  @Test
  void packagesConfiguredWithBothValueAndBasePackages() {
    assertThatExceptionOfType(AnnotationConfigurationException.class)
            .isThrownBy(() -> this.context = new AnnotationConfigApplicationContext(ValueAndBasePackages.class))
            .withMessageContaining("'value'").withMessageContaining("'basePackages'")
            .withMessageContaining("com.example.foo").withMessageContaining("com.example.bar");
  }

  @Test
  void packagesFromMultipleAnnotationsAreMerged() {
    this.context = new AnnotationConfigApplicationContext(BasePackages.class, AdditionalPackages.class);
    ServletComponentRegisteringPostProcessor postProcessor = this.context
            .getBean(ServletComponentRegisteringPostProcessor.class);
    assertThat(postProcessor.getPackagesToScan()).contains("com.example.foo", "com.example.bar", "com.example.baz");
  }

  @Test
  void withNoBasePackagesScanningUsesBasePackageOfAnnotatedClass() {
    this.context = new AnnotationConfigApplicationContext(NoBasePackages.class);
    ServletComponentRegisteringPostProcessor postProcessor = this.context
            .getBean(ServletComponentRegisteringPostProcessor.class);
    assertThat(postProcessor.getPackagesToScan()).containsExactly("cn.taketoday.framework.web.servlet");
  }

  @Test
  void noBasePackageAndBasePackageAreCombinedCorrectly() {
    this.context = new AnnotationConfigApplicationContext(NoBasePackages.class, BasePackages.class);
    ServletComponentRegisteringPostProcessor postProcessor = this.context
            .getBean(ServletComponentRegisteringPostProcessor.class);
    assertThat(postProcessor.getPackagesToScan()).containsExactlyInAnyOrder("cn.taketoday.framework.web.servlet",
            "com.example.foo", "com.example.bar");
  }

  @Test
  void basePackageAndNoBasePackageAreCombinedCorrectly() {
    this.context = new AnnotationConfigApplicationContext(BasePackages.class, NoBasePackages.class);
    ServletComponentRegisteringPostProcessor postProcessor = this.context
            .getBean(ServletComponentRegisteringPostProcessor.class);
    assertThat(postProcessor.getPackagesToScan()).containsExactlyInAnyOrder("cn.taketoday.framework.web.servlet",
            "com.example.foo", "com.example.bar");
  }

  @Configuration(proxyBeanMethods = false)
  @ServletComponentScan({ "com.example.foo", "com.example.bar" })
  static class ValuePackages {

  }

  @Configuration(proxyBeanMethods = false)
  @ServletComponentScan(basePackages = { "com.example.foo", "com.example.bar" })
  static class BasePackages {

  }

  @Configuration(proxyBeanMethods = false)
  @ServletComponentScan(basePackages = "com.example.baz")
  static class AdditionalPackages {

  }

  @Configuration(proxyBeanMethods = false)
  @ServletComponentScan(basePackageClasses = ServletComponentScanRegistrarTests.class)
  static class BasePackageClasses {

  }

  @Configuration(proxyBeanMethods = false)
  @ServletComponentScan(value = "com.example.foo", basePackages = "com.example.bar")
  static class ValueAndBasePackages {

  }

  @Configuration(proxyBeanMethods = false)
  @ServletComponentScan
  static class NoBasePackages {

  }

}