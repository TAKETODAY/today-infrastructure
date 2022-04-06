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

package cn.taketoday.framework.web.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.ListableBeanFactory;
import cn.taketoday.framework.web.server.WebServerFactory;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizerBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebServerFactoryCustomizerBeanPostProcessor}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class WebServerFactoryCustomizerBeanPostProcessorTests {

  private WebServerFactoryCustomizerBeanPostProcessor processor = new WebServerFactoryCustomizerBeanPostProcessor();

  @Mock
  private ListableBeanFactory beanFactory;

  @BeforeEach
  void setup() {
    this.processor.setBeanFactory(this.beanFactory);
  }

  @Test
  void setBeanFactoryWhenNotListableShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.processor.setBeanFactory(mock(BeanFactory.class)))
            .withMessageContaining(
                    "WebServerCustomizerBeanPostProcessor can only be used with a ListableBeanFactory");
  }

  @Test
  void postProcessBeforeShouldReturnBean() {
    Object bean = new Object();
    Object result = this.processor.postProcessBeforeInitialization(bean, "foo");
    assertThat(result).isSameAs(bean);
  }

  @Test
  void postProcessAfterShouldReturnBean() {
    Object bean = new Object();
    Object result = this.processor.postProcessAfterInitialization(bean, "foo");
    assertThat(result).isSameAs(bean);
  }

  @Test
  void postProcessAfterShouldCallInterfaceCustomizers() {
    Map<String, Object> beans = addInterfaceBeans();
    addMockBeans(beans);
    postProcessBeforeInitialization(WebServerFactory.class);
    assertThat(wasCalled(beans, "one")).isFalse();
    assertThat(wasCalled(beans, "two")).isFalse();
    assertThat(wasCalled(beans, "all")).isTrue();
  }

  @Test
  void postProcessAfterWhenWebServerFactoryOneShouldCallInterfaceCustomizers() {
    Map<String, Object> beans = addInterfaceBeans();
    addMockBeans(beans);
    postProcessBeforeInitialization(WebServerFactoryOne.class);
    assertThat(wasCalled(beans, "one")).isTrue();
    assertThat(wasCalled(beans, "two")).isFalse();
    assertThat(wasCalled(beans, "all")).isTrue();
  }

  @Test
  void postProcessAfterWhenWebServerFactoryTwoShouldCallInterfaceCustomizers() {
    Map<String, Object> beans = addInterfaceBeans();
    addMockBeans(beans);
    postProcessBeforeInitialization(WebServerFactoryTwo.class);
    assertThat(wasCalled(beans, "one")).isFalse();
    assertThat(wasCalled(beans, "two")).isTrue();
    assertThat(wasCalled(beans, "all")).isTrue();
  }

  private Map<String, Object> addInterfaceBeans() {
    WebServerFactoryOneCustomizer oneCustomizer = new WebServerFactoryOneCustomizer();
    WebServerFactoryTwoCustomizer twoCustomizer = new WebServerFactoryTwoCustomizer();
    WebServerFactoryAllCustomizer allCustomizer = new WebServerFactoryAllCustomizer();
    Map<String, Object> beans = new LinkedHashMap<>();
    beans.put("one", oneCustomizer);
    beans.put("two", twoCustomizer);
    beans.put("all", allCustomizer);
    return beans;
  }

  @Test
  void postProcessAfterShouldCallLambdaCustomizers() {
    List<String> called = new ArrayList<>();
    addLambdaBeans(called);
    postProcessBeforeInitialization(WebServerFactory.class);
    assertThat(called).containsExactly("all");
  }

  @Test
  void postProcessAfterWhenWebServerFactoryOneShouldCallLambdaCustomizers() {
    List<String> called = new ArrayList<>();
    addLambdaBeans(called);
    postProcessBeforeInitialization(WebServerFactoryOne.class);
    assertThat(called).containsExactly("one", "all");
  }

  @Test
  void postProcessAfterWhenWebServerFactoryTwoShouldCallLambdaCustomizers() {
    List<String> called = new ArrayList<>();
    addLambdaBeans(called);
    postProcessBeforeInitialization(WebServerFactoryTwo.class);
    assertThat(called).containsExactly("two", "all");
  }

  private void addLambdaBeans(List<String> called) {
    WebServerFactoryCustomizer<WebServerFactoryOne> one = (f) -> called.add("one");
    WebServerFactoryCustomizer<WebServerFactoryTwo> two = (f) -> called.add("two");
    WebServerFactoryCustomizer<WebServerFactory> all = (f) -> called.add("all");
    Map<String, Object> beans = new LinkedHashMap<>();
    beans.put("one", one);
    beans.put("two", two);
    beans.put("all", all);
    addMockBeans(beans);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void addMockBeans(Map<String, ?> beans) {
    given(this.beanFactory.getBeansOfType(WebServerFactoryCustomizer.class, false, false))
            .willReturn((Map<String, WebServerFactoryCustomizer>) beans);
  }

  private void postProcessBeforeInitialization(Class<?> type) {
    this.processor.postProcessBeforeInitialization(mock(type), "foo");
  }

  private boolean wasCalled(Map<String, ?> beans, String name) {
    return ((MockWebServerFactoryCustomizer<?>) beans.get(name)).wasCalled();
  }

  interface WebServerFactoryOne extends WebServerFactory {

  }

  interface WebServerFactoryTwo extends WebServerFactory {

  }

  static class MockWebServerFactoryCustomizer<T extends WebServerFactory> implements WebServerFactoryCustomizer<T> {

    private boolean called;

    @Override
    public void customize(T factory) {
      this.called = true;
    }

    boolean wasCalled() {
      return this.called;
    }

  }

  static class WebServerFactoryOneCustomizer extends MockWebServerFactoryCustomizer<WebServerFactoryOne> {

  }

  static class WebServerFactoryTwoCustomizer extends MockWebServerFactoryCustomizer<WebServerFactoryTwo> {

  }

  static class WebServerFactoryAllCustomizer extends MockWebServerFactoryCustomizer<WebServerFactory> {

  }

}
