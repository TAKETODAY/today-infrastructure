/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.server.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import infra.context.properties.bind.Binder;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.mock.env.MockEnvironment;
import infra.test.classpath.ClassPathExclusions;
import infra.web.config.WebProperties;
import infra.web.config.WebProperties.ForwardedHeaders.Naming;
import infra.web.server.ForwardedRequestHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class ServerPropertyAdaptersTests {

  @Test
  void adaptWhenNativeSpecified() {
    MockEnvironment environment = new MockEnvironment()
            .withProperty("web.forwarded-headers.strategy", "web-server")
            .withProperty("web.forwarded-headers.naming", "x-forwarded")
            .withProperty("web.forwarded-headers.use-prefix", "true");
    assertThat(adapt(environment)).isEqualTo(new ForwardedRequestHeaders(Naming.X_FORWARDED, true));
  }

  @Test
  void adaptWhenFrameworkAndCloudPlatformUsesForwardHeadersReturnsNull() {
    MockEnvironment environment = new MockEnvironment()
            .withProperty("web.forwarded-headers.strategy", "framework")
            .withProperty("app.main.cloud-platform", "cloud_foundry");
    assertThat(adapt(environment)).isNull();
  }

  @Test
  void adaptWhenNoneAndCloudPlatformUsesForwardHeadersReturnsNull() {
    MockEnvironment environment = new MockEnvironment()
            .withProperty("web.forwarded-headers.strategy", "none")
            .withProperty("app.main.cloud-platform", "cloud_foundry");
    assertThat(adapt(environment)).isNull();
  }

  @Test
  void adaptWhenNotSpecifiedAndNoEnvironmentReturnsNull() {
    assertThat(adapt(null)).isNull();
  }

  @Test
  void adaptWhenNotSpecifiedAndCloudPlatformUsesForwardHeaders() {
    MockEnvironment environment = new MockEnvironment()
            .withProperty("app.main.cloud-platform", "cloud_foundry");
    assertThat(adapt(environment)).isEqualTo(new ForwardedRequestHeaders(Naming.X_FORWARDED, false));
  }

  @Test
  void adaptWhenNotSpecifiedAndCloudPlatformDetectedFromEnvironment() {
    MockEnvironment environment = new MockEnvironment().withProperty("DYNO", "-");
    assertThat(adapt(environment)).isEqualTo(new ForwardedRequestHeaders(Naming.X_FORWARDED, false));
  }

  @Test
  void adaptWhenNotSpecifiedAndCloudPlatformUsesForwardHeadersWithCustomNamingAndPrefix() {
    MockEnvironment environment = new MockEnvironment()
            .withProperty("app.main.cloud-platform", "cloud_foundry")
            .withProperty("web.forwarded-headers.naming", "rfc-7239");
    assertThat(adapt(environment)).isEqualTo(new ForwardedRequestHeaders(Naming.RFC_7239, false));
  }

  @Test
  void adaptWhenNotSpecifiedAndCloudPlatformDoesNotUseForwardHeaders() {
    MockEnvironment environment = new MockEnvironment()
            .withProperty("app.main.cloud-platform", "none");
    assertThat(adapt(environment)).isNull();
  }

  @Test
  @ClassPathExclusions("infra-app-*.jar")
  void adaptWhenNotSpecifiedAndAppIsNotPresentReturnsNull() {
    MockEnvironment environment = new MockEnvironment()
            .withProperty("DYNO", "-")
            .withProperty("app.main.cloud-platform", "cloud_foundry");
    assertThat(adapt(environment)).isNull();
  }

  @Test
  @ClassPathExclusions("infra-app-*.jar")
  void adaptWhenNativeSpecifiedAndAppIsNotPresent() {
    MockEnvironment environment = new MockEnvironment()
            .withProperty("web.forwarded-headers.strategy", "web-server")
            .withProperty("web.forwarded-headers.naming", "rfc-7239");
    assertThat(adapt(environment)).isEqualTo(new ForwardedRequestHeaders(Naming.RFC_7239, false));
  }

  private @Nullable ForwardedRequestHeaders adapt(@Nullable Environment environment) {
    WebProperties webProperties = environment != null
            ? Binder.get((ConfigurableEnvironment) environment).bindOrCreate("web", WebProperties.class) : new WebProperties();
    return ServerPropertyAdapters.webServerForwardedRequestHeaders(environment).adapt(webProperties);
  }

}
