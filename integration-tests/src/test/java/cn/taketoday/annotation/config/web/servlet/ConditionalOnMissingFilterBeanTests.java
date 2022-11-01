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

package cn.taketoday.annotation.config.web.servlet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Consumer;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.web.servlet.ConditionalOnMissingFilterBean;
import cn.taketoday.framework.web.servlet.FilterRegistrationBean;
import cn.taketoday.util.StringUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnMissingFilterBean @ConditionalOnMissingFilterBean}.
 *
 * @author Phillip Webb
 */
class ConditionalOnMissingFilterBeanTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void outcomeWhenValueIsOfMissingBeanReturnsMatch() {

    this.contextRunner.withUserConfiguration(WithoutTestFilterConfig.class, OnMissingWithValueConfig.class)
            .run((context) -> assertThat(context).satisfies(filterBeanRequirement("myOtherFilter", "testFilter")));
  }

  @Test
  void outcomeWhenValueIsOfExistingBeanReturnsNoMatch() {
    this.contextRunner.withUserConfiguration(WithTestFilterConfig.class, OnMissingWithValueConfig.class)
            .run((context) -> assertThat(context).satisfies(filterBeanRequirement("myTestFilter")));
  }

  @Test
  void outcomeWhenValueIsOfMissingBeanRegistrationReturnsMatch() {
    this.contextRunner
            .withUserConfiguration(WithoutTestFilterRegistrationConfig.class, OnMissingWithValueConfig.class)
            .run((context) -> assertThat(context).satisfies(filterBeanRequirement("myOtherFilter", "testFilter")));
  }

  @Test
  void outcomeWhenValueIsOfExistingBeanRegistrationReturnsNoMatch() {
    this.contextRunner.withUserConfiguration(WithTestFilterRegistrationConfig.class, OnMissingWithValueConfig.class)
            .run((context) -> assertThat(context).satisfies(filterBeanRequirement("myTestFilter")));
  }

  @Test
  void outcomeWhenReturnTypeIsOfExistingBeanReturnsNoMatch() {
    this.contextRunner.withUserConfiguration(WithTestFilterConfig.class, OnMissingWithReturnTypeConfig.class)
            .run((context) -> assertThat(context).satisfies(filterBeanRequirement("myTestFilter")));
  }

  @Test
  void outcomeWhenReturnTypeIsOfExistingBeanRegistrationReturnsNoMatch() {
    this.contextRunner
            .withUserConfiguration(WithTestFilterRegistrationConfig.class, OnMissingWithReturnTypeConfig.class)
            .run((context) -> assertThat(context).satisfies(filterBeanRequirement("myTestFilter")));
  }

  @Test
  void outcomeWhenReturnRegistrationTypeIsOfExistingBeanReturnsNoMatch() {
    this.contextRunner
            .withUserConfiguration(WithTestFilterConfig.class, OnMissingWithReturnRegistrationTypeConfig.class)
            .run((context) -> assertThat(context).satisfies(filterBeanRequirement("myTestFilter")));
  }

  @Test
  void outcomeWhenReturnRegistrationTypeIsOfExistingBeanRegistrationReturnsNoMatch() {
    this.contextRunner
            .withUserConfiguration(WithTestFilterRegistrationConfig.class,
                    OnMissingWithReturnRegistrationTypeConfig.class)
            .run((context) -> assertThat(context).satisfies(filterBeanRequirement("myTestFilter")));
  }

  private Consumer<ConfigurableApplicationContext> filterBeanRequirement(String... names) {
    return (context) -> {
      String[] filters = StringUtils.toStringArray(context.getBeanNamesForType(Filter.class));
      String[] registrations = StringUtils.toStringArray(context.getBeanNamesForType(FilterRegistrationBean.class));
      assertThat(StringUtils.concatenateStringArrays(filters, registrations)).containsOnly(names);
    };
  }

  @Configuration(proxyBeanMethods = false)
  static class WithTestFilterConfig {

    @Bean
    TestFilter myTestFilter() {
      return new TestFilter();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class WithoutTestFilterConfig {

    @Bean
    OtherFilter myOtherFilter() {
      return new OtherFilter();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class WithoutTestFilterRegistrationConfig {

    @Bean
    FilterRegistrationBean<OtherFilter> myOtherFilter() {
      return new FilterRegistrationBean<>(new OtherFilter());
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class WithTestFilterRegistrationConfig {

    @Bean
    FilterRegistrationBean<TestFilter> myTestFilter() {
      return new FilterRegistrationBean<>(new TestFilter());
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class OnMissingWithValueConfig {

    @Bean
    @ConditionalOnMissingFilterBean(TestFilter.class)
    TestFilter testFilter() {
      return new TestFilter();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class OnMissingWithReturnTypeConfig {

    @Bean
    @ConditionalOnMissingFilterBean
    TestFilter testFilter() {
      return new TestFilter();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class OnMissingWithReturnRegistrationTypeConfig {

    @Bean
    @ConditionalOnMissingFilterBean
    FilterRegistrationBean<TestFilter> testFilter() {
      return new FilterRegistrationBean<>(new TestFilter());
    }

  }

  static class TestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
    }

  }

  static class OtherFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
    }

  }

}
