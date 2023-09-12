/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.web.socket;

import org.eclipse.jetty.ee10.websocket.servlet.WebSocketUpgradeFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import cn.taketoday.annotation.config.web.WebMvcAutoConfiguration;
import cn.taketoday.annotation.config.web.servlet.DispatcherServletAutoConfiguration;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.core.Ordered;
import cn.taketoday.framework.test.context.runner.WebApplicationContextRunner;
import cn.taketoday.framework.test.web.client.TestRestTemplate;
import cn.taketoday.framework.web.embedded.jetty.JettyServletWebServerFactory;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import cn.taketoday.framework.web.servlet.FilterRegistrationBean;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.test.classpath.ForkedClassPath;
import cn.taketoday.test.web.servlet.DirtiesUrlFactories;
import cn.taketoday.test.web.servlet.Servlet5ClassPathOverrides;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/3 19:45
 */
@DirtiesUrlFactories
class WebSocketAutoConfigurationTests {

  @ParameterizedTest(name = "{0}")
  @MethodSource("testConfiguration")
  @ForkedClassPath
  void serverContainerIsAvailableFromTheServletContext(String server, Class<?>... configuration) {
    try (var context = new AnnotationConfigServletWebServerApplicationContext(configuration)) {
      Object serverContainer = context.getServletContext()
              .getAttribute("jakarta.websocket.server.ServerContainer");
      assertThat(serverContainer).isInstanceOf(ServerContainer.class);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testConfiguration")
  @ForkedClassPath
  void webSocketUpgradeDoesNotPreventAFilterFromRejectingTheRequest(String server, Class<?>... configuration)
          throws DeploymentException {
    try (var context = new AnnotationConfigServletWebServerApplicationContext(configuration)) {
      ServerContainer serverContainer = (ServerContainer) context.getServletContext()
              .getAttribute("jakarta.websocket.server.ServerContainer");
      serverContainer.addEndpoint(TestEndpoint.class);
      WebServer webServer = context.getWebServer();
      int port = webServer.getPort();
      TestRestTemplate rest = new TestRestTemplate();
      RequestEntity<Void> request = RequestEntity.get("http://localhost:" + port)
              .header("Upgrade", "websocket")
              .header("Connection", "upgrade")
              .header("Sec-WebSocket-Version", "13")
              .header("Sec-WebSocket-Key", "key")
              .build();
      ResponseEntity<Void> response = rest.exchange(request, Void.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Test
  @SuppressWarnings("rawtypes")
  void whenCustomUpgradeFilterRegistrationIsDefinedAutoConfiguredRegistrationOfJettyUpgradeFilterBacksOff() {
    new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JettyConfiguration.class,
                    WebSocketAutoConfiguration.JettyWebSocketConfiguration.class))
            .withUserConfiguration(CustomUpgradeFilterRegistrationConfiguration.class)
            .run((context) -> {
              Map<String, FilterRegistrationBean> filterRegistrations = context
                      .getBeansOfType(FilterRegistrationBean.class);
              assertThat(filterRegistrations).containsOnlyKeys("unauthorizedFilter",
                      "customUpgradeFilterRegistration");
            });
  }

  @Test
  @SuppressWarnings("rawtypes")
  void whenCustomUpgradeFilterIsDefinedAutoConfiguredRegistrationOfJettyUpgradeFilterBacksOff() {
    new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JettyConfiguration.class,
                    WebSocketAutoConfiguration.JettyWebSocketConfiguration.class))
            .withUserConfiguration(CustomUpgradeFilterConfiguration.class)
            .run((context) -> {
              Map<String, FilterRegistrationBean> filterRegistrations = context
                      .getBeansOfType(FilterRegistrationBean.class);
              assertThat(filterRegistrations).containsOnlyKeys("unauthorizedFilter");
            });
  }

  static Stream<Arguments> testConfiguration() {
    String response = "Tomcat";
    return Stream.of(
            Arguments.of("Jetty",
                    new Class<?>[] { JettyConfiguration.class, DispatcherServletAutoConfiguration.class,
                            WebSocketAutoConfiguration.JettyWebSocketConfiguration.class, WebMvcAutoConfiguration.class }),
            Arguments.of(response,
                    new Class<?>[] { TomcatConfiguration.class, DispatcherServletAutoConfiguration.class,
                            WebSocketAutoConfiguration.TomcatWebSocketConfiguration.class, WebMvcAutoConfiguration.class }));
  }

  @Configuration(proxyBeanMethods = false)
  static class CommonConfiguration {

    @Bean
    FilterRegistrationBean<Filter> unauthorizedFilter() {
      FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(new Filter() {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException {
          ((HttpServletResponse) response).sendError(HttpStatus.UNAUTHORIZED.value());
        }

      });
      registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
      registration.addUrlPatterns("/*");
      registration.setDispatcherTypes(DispatcherType.REQUEST);
      return registration;
    }

    @Bean
    WebServerFactoryCustomizerBeanPostProcessor ServletWebServerCustomizerBeanPostProcessor() {
      return new WebServerFactoryCustomizerBeanPostProcessor();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TomcatConfiguration extends CommonConfiguration {

    @Bean
    ServletWebServerFactory webServerFactory() {
      TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
      factory.setPort(0);
      return factory;
    }

  }

  @Servlet5ClassPathOverrides
  @Configuration(proxyBeanMethods = false)
  static class JettyConfiguration extends CommonConfiguration {

    @Bean
    ServletWebServerFactory webServerFactory() {
      JettyServletWebServerFactory JettyServletWebServerFactory = new JettyServletWebServerFactory();
      JettyServletWebServerFactory.setPort(0);
      return JettyServletWebServerFactory;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomUpgradeFilterRegistrationConfiguration {

    @Bean
    FilterRegistrationBean<WebSocketUpgradeFilter> customUpgradeFilterRegistration() {
      FilterRegistrationBean<WebSocketUpgradeFilter> registration = new FilterRegistrationBean<>(
              new WebSocketUpgradeFilter());
      return registration;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomUpgradeFilterConfiguration {

    @Bean
    WebSocketUpgradeFilter customUpgradeFilter() {
      return new WebSocketUpgradeFilter();
    }

  }

  @ServerEndpoint("/")
  public static class TestEndpoint {

  }

}