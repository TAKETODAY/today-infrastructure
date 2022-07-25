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

package cn.taketoday.context.annotation.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.packagestest.one.FirstConfiguration;
import cn.taketoday.context.annotation.config.packagestest.two.SecondConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/6 18:49
 */
class AutoConfigurationPackagesTests {

  @Test
  void setAndGet() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            ConfigWithAutoConfigurationPackage.class);
    assertThat(AutoConfigurationPackages.get(context.getBeanFactory()))
            .containsExactly(getClass().getPackage().getName());
  }

  @Test
  void getWithoutSet() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(EmptyConfig.class);
    assertThatIllegalStateException().isThrownBy(() -> AutoConfigurationPackages.get(context.getBeanFactory()))
            .withMessageContaining("Unable to retrieve @EnableAutoConfiguration base packages");
  }

  @Test
  void detectsMultipleAutoConfigurationPackages() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(FirstConfiguration.class,
            SecondConfiguration.class);
    List<String> packages = AutoConfigurationPackages.get(context.getBeanFactory());
    Package package1 = FirstConfiguration.class.getPackage();
    Package package2 = SecondConfiguration.class.getPackage();
    assertThat(packages).containsOnly(package1.getName(), package2.getName());
  }

  @Test
  void whenBasePackagesAreSpecifiedThenTheyAreRegistered() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            ConfigWithAutoConfigurationBasePackages.class);
    List<String> packages = AutoConfigurationPackages.get(context.getBeanFactory());
    assertThat(packages).containsExactly("com.example.alpha", "com.example.bravo");
  }

  @Test
  void whenBasePackageClassesAreSpecifiedThenTheirPackagesAreRegistered() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            ConfigWithAutoConfigurationBasePackageClasses.class);
    List<String> packages = AutoConfigurationPackages.get(context.getBeanFactory());
    assertThat(packages).containsOnly(FirstConfiguration.class.getPackage().getName(),
            SecondConfiguration.class.getPackage().getName());
  }

  @Configuration(proxyBeanMethods = false)
  @AutoConfigurationPackage
  static class ConfigWithAutoConfigurationPackage {

  }

  @Configuration(proxyBeanMethods = false)
  @AutoConfigurationPackage(basePackages = { "com.example.alpha", "com.example.bravo" })
  static class ConfigWithAutoConfigurationBasePackages {

  }

  @Configuration(proxyBeanMethods = false)
  @AutoConfigurationPackage(basePackageClasses = { FirstConfiguration.class, SecondConfiguration.class })
  static class ConfigWithAutoConfigurationBasePackageClasses {

  }

  @Configuration(proxyBeanMethods = false)
  static class EmptyConfig {

  }

}
