/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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
import cn.taketoday.test.web.servlet.DirtiesUrlFactories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatServletWebServerFactoryCustomizer}.
 *
 * @author Phillip Webb
 */
@DirtiesUrlFactories
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
    assertThat(factory.getTldSkipPatterns()).contains("junit-*.jar", "today-framework-*.jar");
  }

  @Test
  void redirectContextRootCanBeConfigured() {
    bind("server.tomcat.redirect-context-root=false");
    ServerProperties.Tomcat tomcat = this.serverProperties.tomcat;
    assertThat(tomcat.redirectContextRoot).isFalse();
    TomcatWebServer server = customizeAndGetServer();
    Context context = (Context) server.getTomcat().getHost().findChildren()[0];
    assertThat(context.getMapperContextRootRedirectEnabled()).isFalse();
  }

  @Test
  void useRelativeRedirectsCanBeConfigured() {
    bind("server.tomcat.use-relative-redirects=true");
    assertThat(this.serverProperties.tomcat.useRelativeRedirects).isTrue();
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
