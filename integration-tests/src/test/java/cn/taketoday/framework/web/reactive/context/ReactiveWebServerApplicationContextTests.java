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

package cn.taketoday.framework.web.reactive.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.event.ContextClosedEvent;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.framework.availability.AvailabilityChangeEvent;
import cn.taketoday.framework.availability.ReadinessState;
import cn.taketoday.framework.web.context.ServerPortInfoApplicationContextInitializer;
import cn.taketoday.framework.web.reactive.server.MockReactiveWebServerFactory;
import cn.taketoday.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link ReactiveWebServerApplicationContext}.
 *
 * @author Andy Wilkinson
 */
class ReactiveWebServerApplicationContextTests {

  private ReactiveWebServerApplicationContext context = new ReactiveWebServerApplicationContext();

  @AfterEach
  void cleanUp() {
    this.context.close();
  }

  @Test
  void whenThereIsNoWebServerFactoryBeanThenContextRefreshWillFail() {
    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(() -> this.context.refresh())
            .havingCause()
            .isInstanceOf(ApplicationContextException.class)
            .withMessageContaining(
                    "Unable to start ReactiveWebApplicationContext due to missing ReactiveWebServerFactory bean");
  }

  @Test
  void whenThereIsNoHttpHandlerBeanThenContextRefreshWillFail() {
    addWebServerFactoryBean();
    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(() -> this.context.refresh())
            .havingCause()
            .isInstanceOf(ApplicationContextException.class)
            .withMessageContaining("Unable to start ReactiveWebApplicationContext due to missing HttpHandler bean");
  }

  @Test
  void whenThereAreMultipleWebServerFactoryBeansThenContextRefreshWillFail() {
    addWebServerFactoryBean();
    addWebServerFactoryBean("anotherWebServerFactory");
    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(() -> this.context.refresh())
            .havingCause()
            .isInstanceOf(ApplicationContextException.class)
            .withMessageContaining(
                    "Unable to start ReactiveWebApplicationContext due to multiple ReactiveWebServerFactory beans");
  }

  @Test
  void whenThereAreMultipleHttpHandlerBeansThenContextRefreshWillFail() {
    addWebServerFactoryBean();
    addHttpHandlerBean("httpHandler1");
    addHttpHandlerBean("httpHandler2");
    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(() -> this.context.refresh())
            .havingCause()
            .isInstanceOf(ApplicationContextException.class)
            .withMessageContaining(
                    "Unable to start ReactiveWebApplicationContext due to multiple HttpHandler beans");
  }

  @Test
  void whenContextIsRefreshedThenReactiveWebServerInitializedEventIsPublished() {
    addWebServerFactoryBean();
    addHttpHandlerBean();
    TestApplicationListener listener = new TestApplicationListener();
    this.context.addApplicationListener(listener);
    this.context.refresh();
    List<ApplicationEvent> events = listener.receivedEvents();
    assertThat(events).hasSize(2).extracting("class").containsExactly(ReactiveWebServerInitializedEvent.class,
            ContextRefreshedEvent.class);
    ReactiveWebServerInitializedEvent initializedEvent = (ReactiveWebServerInitializedEvent) events.get(0);
    assertThat(initializedEvent.getSource().getPort()).isGreaterThanOrEqualTo(0);
    assertThat(initializedEvent.getApplicationContext()).isEqualTo(this.context);
  }

  @Test
  void whenContextIsRefreshedThenLocalServerPortIsAvailableFromTheEnvironment() {
    addWebServerFactoryBean();
    addHttpHandlerBean();
    new ServerPortInfoApplicationContextInitializer().initialize(this.context);
    this.context.refresh();
    ConfigurableEnvironment environment = this.context.getEnvironment();
    assertThat(environment.containsProperty("local.server.port")).isTrue();
    assertThat(environment.getProperty("local.server.port")).isEqualTo("8080");
  }

  @Test
  void whenContextIsClosedThenWebServerIsStopped() {
    addWebServerFactoryBean();
    addHttpHandlerBean();
    this.context.refresh();
    MockReactiveWebServerFactory factory = this.context.getBean(MockReactiveWebServerFactory.class);
    this.context.close();
    then(factory.getWebServer()).should().stop();
  }

  @Test
  @SuppressWarnings("unchecked")
  void whenContextIsClosedThenApplicationAvailabilityChangesToRefusingTraffic() {
    addWebServerFactoryBean();
    addHttpHandlerBean();
    TestApplicationListener listener = new TestApplicationListener();
    this.context.refresh();
    this.context.addApplicationListener(listener);
    this.context.close();
    List<ApplicationEvent> events = listener.receivedEvents();
    assertThat(events).hasSize(2).extracting("class").contains(AvailabilityChangeEvent.class,
            ContextClosedEvent.class);
    assertThat(((AvailabilityChangeEvent<ReadinessState>) events.get(0)).getState())
            .isEqualTo(ReadinessState.REFUSING_TRAFFIC);
  }

  @Test
  void whenContextIsNotActiveThenCloseDoesNotChangeTheApplicationAvailability() {
    addWebServerFactoryBean();
    addHttpHandlerBean();
    TestApplicationListener listener = new TestApplicationListener();
    this.context.addApplicationListener(listener);
    this.context.registerBeanDefinition("refreshFailure", new RootBeanDefinition(RefreshFailure.class));
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(this.context::refresh);
    this.context.close();
    assertThat(listener.receivedEvents()).isEmpty();
  }

  @Test
  void whenTheContextIsRefreshedThenASubsequentRefreshAttemptWillFail() {
    addWebServerFactoryBean();
    addHttpHandlerBean();
    this.context.refresh();
    assertThatIllegalStateException().isThrownBy(() -> this.context.refresh())
            .withMessageContaining("multiple refresh attempts");
  }

  private void addHttpHandlerBean() {
    addHttpHandlerBean("httpHandler");
  }

  private void addHttpHandlerBean(String beanName) {
    this.context.registerBeanDefinition(beanName,
            new RootBeanDefinition(HttpHandler.class, () -> (request, response) -> null));
  }

  private void addWebServerFactoryBean() {
    addWebServerFactoryBean("webServerFactory");
  }

  private void addWebServerFactoryBean(String beanName) {
    this.context.registerBeanDefinition(beanName, new RootBeanDefinition(MockReactiveWebServerFactory.class));
  }

  static class TestApplicationListener implements ApplicationListener<ApplicationEvent> {

    private Deque<ApplicationEvent> events = new ArrayDeque<>();

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
      this.events.add(event);
    }

    List<ApplicationEvent> receivedEvents() {
      List<ApplicationEvent> receivedEvents = new ArrayList<>();
      while (!this.events.isEmpty()) {
        receivedEvents.add(this.events.pollFirst());
      }
      return receivedEvents;
    }

  }

  static class RefreshFailure {

    RefreshFailure() {
      throw new RuntimeException("Fail refresh");
    }

  }

}
