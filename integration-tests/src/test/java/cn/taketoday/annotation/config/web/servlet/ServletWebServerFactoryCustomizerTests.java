/*
 * Copyright 2012-2022 the original author or authors.
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

package cn.taketoday.annotation.config.web.servlet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.MapConfigurationPropertySource;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.Shutdown;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.servlet.server.ConfigurableServletWebServerFactory;
import cn.taketoday.framework.web.servlet.server.JspProperties;
import cn.taketoday.session.config.CookieProperties;
import cn.taketoday.session.config.SessionProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServletWebServerFactoryCustomizer}.
 *
 * @author Brian Clozel
 * @author Yunkun Huang
 */
class ServletWebServerFactoryCustomizerTests {

  private final ServerProperties properties = new ServerProperties();

  private ServletWebServerFactoryCustomizer customizer;

  @BeforeEach
  void setup() {
    this.customizer = new ServletWebServerFactoryCustomizer(this.properties);
  }

  @Test
  void testDefaultDisplayName() {
    ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setDisplayName("application");
  }

  @Test
  void testCustomizeDisplayName() {
    ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
    this.properties.getServlet().setApplicationDisplayName("TestName");
    this.customizer.customize(factory);
    then(factory).should().setDisplayName("TestName");
  }

  @Test
  void testCustomizeDefaultServlet() {
    ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
    this.properties.getServlet().setRegisterDefaultServlet(false);
    this.customizer.customize(factory);
    then(factory).should().setRegisterDefaultServlet(false);
  }

  @Test
  void testCustomizeSsl() {
    ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
    Ssl ssl = mock(Ssl.class);
    this.properties.setSsl(ssl);
    this.customizer.customize(factory);
    then(factory).should().setSsl(ssl);
  }

  @Test
  void testCustomizeJsp() {
    ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setJsp(any(JspProperties.class));
  }

  @Test
  void customizeSessionProperties() {
    Map<String, String> map = new HashMap<>();
    map.put("server.servlet.session.timeout", "123");
    map.put("server.servlet.session.tracking-modes", "cookie,url");
    map.put("server.servlet.session.cookie.name", "testname");
    map.put("server.servlet.session.cookie.domain", "testdomain");
    map.put("server.servlet.session.cookie.path", "/testpath");
    map.put("server.servlet.session.cookie.comment", "testcomment");
    map.put("server.servlet.session.cookie.http-only", "true");
    map.put("server.servlet.session.cookie.secure", "true");
    map.put("server.servlet.session.cookie.max-age", "60");
    bindProperties(map);
    ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
    this.customizer.customize(factory);
    ArgumentCaptor<SessionProperties> sessionCaptor = ArgumentCaptor.forClass(SessionProperties.class);
    then(factory).should().setSession(sessionCaptor.capture());
    assertThat(sessionCaptor.getValue().getTimeout()).hasSeconds(123);
    CookieProperties cookie = sessionCaptor.getValue().getCookie();
    assertThat(cookie.getName()).isEqualTo("testname");
    assertThat(cookie.getDomain()).isEqualTo("testdomain");
    assertThat(cookie.getPath()).isEqualTo("/testpath");
    assertThat(cookie.getComment()).isEqualTo("testcomment");
    assertThat(cookie.getHttpOnly()).isTrue();
    assertThat(cookie.getMaxAge()).hasSeconds(60);
  }

  @Test
  void testCustomizeTomcatPort() {
    ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
    this.properties.setPort(8080);
    this.customizer.customize(factory);
    then(factory).should().setPort(8080);
  }

  @Test
  void customizeServletDisplayName() {
    Map<String, String> map = new HashMap<>();
    map.put("server.servlet.application-display-name", "MyBootApp");
    bindProperties(map);
    ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setDisplayName("MyBootApp");
  }

  @Test
  void testCustomizeTomcatMinSpareThreads() {
    Map<String, String> map = new HashMap<>();
    map.put("server.tomcat.threads.min-spare", "10");
    bindProperties(map);
    assertThat(this.properties.getTomcat().getThreads().getMinSpare()).isEqualTo(10);
  }

  @Test
  void sessionStoreDir() {
    Map<String, String> map = new HashMap<>();
    map.put("server.servlet.session.store-dir", "mydirectory");
    bindProperties(map);
    ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
    this.customizer.customize(factory);
    ArgumentCaptor<SessionProperties> sessionCaptor = ArgumentCaptor.forClass(SessionProperties.class);
    then(factory).should().setSession(sessionCaptor.capture());
    assertThat(sessionCaptor.getValue().getStoreDir()).isEqualTo(new File("mydirectory"));
  }

  @Test
  void whenShutdownPropertyIsSetThenShutdownIsCustomized() {
    Map<String, String> map = new HashMap<>();
    map.put("server.shutdown", "graceful");
    bindProperties(map);
    ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
    this.customizer.customize(factory);
    ArgumentCaptor<Shutdown> shutdownCaptor = ArgumentCaptor.forClass(Shutdown.class);
    then(factory).should().setShutdown(shutdownCaptor.capture());
    assertThat(shutdownCaptor.getValue()).isEqualTo(Shutdown.GRACEFUL);
  }

  private void bindProperties(Map<String, String> map) {
    ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
    new Binder(source).bind("server", Bindable.ofInstance(this.properties));
  }

}
