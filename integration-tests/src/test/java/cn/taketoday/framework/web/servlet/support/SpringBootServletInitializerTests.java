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

package cn.taketoday.framework.web.servlet.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.AbstractApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.framework.SpringApplication;
import cn.taketoday.framework.builder.SpringApplicationBuilder;
import cn.taketoday.framework.context.event.ApplicationEnvironmentPreparedEvent;
import cn.taketoday.framework.context.logging.LoggingApplicationListener;
import cn.taketoday.framework.testsupport.system.CapturedOutput;
import cn.taketoday.framework.testsupport.system.OutputCaptureExtension;
import cn.taketoday.framework.web.embedded.undertow.UndertowServletWebServerFactory;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.framework.web.servlet.FilterRegistrationBean;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.context.support.StandardServletEnvironment;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringBootServletInitializer}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class SpringBootServletInitializerTests {

  private ServletContext servletContext = new MockServletContext();

  private SpringApplication application;

  @AfterEach
  void verifyLoggingOutput(CapturedOutput output) {
    assertThat(output).doesNotContain(StandardServletEnvironment.class.getSimpleName());
  }

  @Test
  void failsWithoutConfigure() {
    assertThatIllegalStateException()
            .isThrownBy(
                    () -> new MockSpringBootServletInitializer().createRootApplicationContext(this.servletContext))
            .withMessageContaining("No SpringApplication sources have been defined");
  }

  @Test
  void withConfigurationAnnotation() {
    new WithConfigurationAnnotation().createRootApplicationContext(this.servletContext);
    assertThat(this.application.getAllSources()).containsOnly(WithConfigurationAnnotation.class,
            ErrorPageFilterConfiguration.class);
  }

  @Test
  void withConfiguredSource() {
    new WithConfiguredSource().createRootApplicationContext(this.servletContext);
    assertThat(this.application.getAllSources()).containsOnly(Config.class, ErrorPageFilterConfiguration.class);
  }

  @Test
  void applicationBuilderCanBeCustomized() {
    CustomSpringBootServletInitializer servletInitializer = new CustomSpringBootServletInitializer();
    servletInitializer.createRootApplicationContext(this.servletContext);
    assertThat(servletInitializer.applicationBuilder.built).isTrue();
  }

  @Test
  void mainClassHasSensibleDefault() {
    new WithConfigurationAnnotation().createRootApplicationContext(this.servletContext);
    assertThat(this.application).hasFieldOrPropertyWithValue("mainApplicationClass",
            WithConfigurationAnnotation.class);
  }

  @Test
  void shutdownHooksAreNotRegistered() throws ServletException {
    new WithConfigurationAnnotation().onStartup(this.servletContext);
    assertThat(this.servletContext.getAttribute(LoggingApplicationListener.REGISTER_SHUTDOWN_HOOK_PROPERTY))
            .isEqualTo(false);
    assertThat(this.application).hasFieldOrPropertyWithValue("registerShutdownHook", false);
  }

  @Test
  void errorPageFilterRegistrationCanBeDisabled() {
    WebServer webServer = new UndertowServletWebServerFactory(0).getWebServer((servletContext) -> {
      try (AbstractApplicationContext context = (AbstractApplicationContext) new WithErrorPageFilterNotRegistered()
              .createRootApplicationContext(servletContext)) {
        assertThat(context.getBeansOfType(ErrorPageFilter.class)).hasSize(0);
      }
    });
    try {
      webServer.start();
    }
    finally {
      webServer.stop();
    }
  }

  @Test
  @SuppressWarnings("rawtypes")
  void errorPageFilterIsRegisteredWithNearHighestPrecedence() {
    WebServer webServer = new UndertowServletWebServerFactory(0).getWebServer((servletContext) -> {
      try (AbstractApplicationContext context = (AbstractApplicationContext) new WithErrorPageFilter()
              .createRootApplicationContext(servletContext)) {
        Map<String, FilterRegistrationBean> registrations = context
                .getBeansOfType(FilterRegistrationBean.class);
        assertThat(registrations).hasSize(1);
        FilterRegistrationBean errorPageFilterRegistration = registrations.get("errorPageFilterRegistration");
        assertThat(errorPageFilterRegistration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
      }
    });
    try {
      webServer.start();
    }
    finally {
      webServer.stop();
    }
  }

  @Test
  @SuppressWarnings("rawtypes")
  void errorPageFilterIsRegisteredForRequestAndAsyncDispatch() {
    WebServer webServer = new UndertowServletWebServerFactory(0).getWebServer((servletContext) -> {
      try (AbstractApplicationContext context = (AbstractApplicationContext) new WithErrorPageFilter()
              .createRootApplicationContext(servletContext)) {
        Map<String, FilterRegistrationBean> registrations = context
                .getBeansOfType(FilterRegistrationBean.class);
        assertThat(registrations).hasSize(1);
        FilterRegistrationBean errorPageFilterRegistration = registrations.get("errorPageFilterRegistration");
        assertThat(errorPageFilterRegistration).hasFieldOrPropertyWithValue("dispatcherTypes",
                EnumSet.of(DispatcherType.ASYNC, DispatcherType.REQUEST));
      }
    });
    try {
      webServer.start();
    }
    finally {
      webServer.stop();
    }
  }

  @Test
  void executableWarThatUsesServletInitializerDoesNotHaveErrorPageFilterConfigured() {
    try (ConfigurableApplicationContext context = new SpringApplication(ExecutableWar.class).run()) {
      assertThat(context.getBeansOfType(ErrorPageFilter.class)).hasSize(0);
    }
  }

  @Test
  void servletContextPropertySourceIsAvailablePriorToRefresh() {
    ServletContext servletContext = mock(ServletContext.class);
    given(servletContext.getInitParameterNames())
            .willReturn(Collections.enumeration(Collections.singletonList("spring.profiles.active")));
    given(servletContext.getInitParameter("spring.profiles.active")).willReturn("from-servlet-context");
    given(servletContext.getAttributeNames()).willReturn(Collections.emptyEnumeration());
    try (ConfigurableApplicationContext context = (ConfigurableApplicationContext) new PropertySourceVerifyingSpringBootServletInitializer()
            .createRootApplicationContext(servletContext)) {
      assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("from-servlet-context");
    }
  }

  @Test
  void whenServletContextIsDestroyedThenJdbcDriversAreDeregistered() throws ServletException {
    ServletContext servletContext = mock(ServletContext.class);
    given(servletContext.getInitParameterNames()).willReturn(new Vector<String>().elements());
    given(servletContext.getAttributeNames()).willReturn(new Vector<String>().elements());
    AtomicBoolean driversDeregistered = new AtomicBoolean();
    new SpringBootServletInitializer() {

      @Override
      protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Config.class);
      }

      @Override
      protected void deregisterJdbcDrivers(ServletContext servletContext) {
        driversDeregistered.set(true);
      }

    }.onStartup(servletContext);
    ArgumentCaptor<ServletContextListener> captor = ArgumentCaptor.forClass(ServletContextListener.class);
    then(servletContext).should().addListener(captor.capture());
    captor.getValue().contextDestroyed(new ServletContextEvent(servletContext));
    assertThat(driversDeregistered).isTrue();
  }

  static class PropertySourceVerifyingSpringBootServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
      return builder.sources(TestApp.class).listeners(new PropertySourceVerifyingApplicationListener());
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestApp {

  }

  private class MockSpringBootServletInitializer extends SpringBootServletInitializer {

    @Override
    protected WebApplicationContext run(SpringApplication application) {
      SpringBootServletInitializerTests.this.application = application;
      return null;
    }

  }

  private class CustomSpringBootServletInitializer extends MockSpringBootServletInitializer {

    private final CustomSpringApplicationBuilder applicationBuilder = new CustomSpringApplicationBuilder();

    @Override
    protected SpringApplicationBuilder createSpringApplicationBuilder() {
      return this.applicationBuilder;
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
      return application.sources(Config.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  public class WithConfigurationAnnotation extends MockSpringBootServletInitializer {

  }

  public class WithConfiguredSource extends MockSpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
      return application.sources(Config.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class WithErrorPageFilterNotRegistered extends SpringBootServletInitializer {

    WithErrorPageFilterNotRegistered() {
      setRegisterErrorPageFilter(false);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class WithErrorPageFilter extends SpringBootServletInitializer {

  }

  @Configuration(proxyBeanMethods = false)
  static class ExecutableWar extends SpringBootServletInitializer {

    @Bean
    ServletWebServerFactory webServerFactory() {
      return new UndertowServletWebServerFactory(0);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

  }

  static class CustomSpringApplicationBuilder extends SpringApplicationBuilder {

    private boolean built;

    @Override
    public SpringApplication build() {
      this.built = true;
      return super.build();
    }

  }

  private static final class PropertySourceVerifyingApplicationListener
          implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
      PropertySource<?> propertySource = event.getEnvironment().getPropertySources()
              .get(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME);
      assertThat(propertySource.getProperty("spring.profiles.active")).isEqualTo("from-servlet-context");
    }

  }

}
