/*
 * Copyright 2012-2020 the original author or authors.
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

import org.apache.catalina.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.embedded.tomcat.TomcatWebServer;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.mock.env.MockEnvironment;
import cn.taketoday.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatServletWebServerFactoryCustomizer}.
 *
 * @author Phillip Webb
 */
class TomcatServletWebServerFactoryCustomizerTests {

  private TomcatServletWebServerFactoryCustomizer customizer;

  private MockEnvironment environment;

  private ServerProperties serverProperties;

  @BeforeEach
  void setup() {
    this.environment = new MockEnvironment();
    this.serverProperties = new ServerProperties();
    ConfigurationPropertySources.attach(this.environment);
    this.customizer = new TomcatServletWebServerFactoryCustomizer(this.serverProperties);
  }

  @Test
  void customTldSkip() {
    bind("server.tomcat.additional-tld-skip-patterns=foo.jar,bar.jar");
    testCustomTldSkip("foo.jar", "bar.jar");
  }

  @Test
  void customTldSkipAsList() {
    bind("server.tomcat.additional-tld-skip-patterns[0]=biz.jar",
            "server.tomcat.additional-tld-skip-patterns[1]=bah.jar");
    testCustomTldSkip("biz.jar", "bah.jar");
  }

  private void testCustomTldSkip(String... expectedJars) {
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(factory.getTldSkipPatterns()).contains(expectedJars);
    assertThat(factory.getTldSkipPatterns()).contains("junit-*.jar", "spring-boot-*.jar");
  }

  @Test
  void redirectContextRootCanBeConfigured() {
    bind("server.tomcat.redirect-context-root=false");
    ServerProperties.Tomcat tomcat = this.serverProperties.getTomcat();
    assertThat(tomcat.getRedirectContextRoot()).isFalse();
    TomcatWebServer server = customizeAndGetServer();
    Context context = (Context) server.getTomcat().getHost().findChildren()[0];
    assertThat(context.getMapperContextRootRedirectEnabled()).isFalse();
  }

  @Test
  void useRelativeRedirectsCanBeConfigured() {
    bind("server.tomcat.use-relative-redirects=true");
    assertThat(this.serverProperties.getTomcat().isUseRelativeRedirects()).isTrue();
    TomcatWebServer server = customizeAndGetServer();
    Context context = (Context) server.getTomcat().getHost().findChildren()[0];
    assertThat(context.getUseRelativeRedirects()).isTrue();
  }

  private void bind(String... inlinedProperties) {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, inlinedProperties);
    new Binder(ConfigurationPropertySources.get(this.environment)).bind("server",
            Bindable.ofInstance(this.serverProperties));
  }

  private TomcatWebServer customizeAndGetServer() {
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    return (TomcatWebServer) factory.getWebServer();
  }

  private TomcatServletWebServerFactory customizeAndGetFactory() {
    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
    this.customizer.customize(factory);
    return factory;
  }

}
