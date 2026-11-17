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

package infra.web.config;

import org.junit.jupiter.api.Test;

import infra.mock.env.MockEnvironment;
import infra.context.properties.bind.Binder;
import infra.web.config.WebProperties.ForwardedHeaders.Naming;

import static org.assertj.core.api.Assertions.assertThat;

class ForwardedHeadersPropertiesTests {

  @Test
  void bindsSharedForwardedHeaders() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("web.forwarded-headers.strategy", "web-server");
    environment.setProperty("web.forwarded-headers.naming", "rfc-7239");
    environment.setProperty("web.forwarded-headers.use-prefix", "true");
    var properties = Binder.get(environment).bindOrCreate("web", WebProperties.class).forwardedHeaders;
    assertThat(properties.strategy).isEqualTo(ForwardedHeaderStrategy.WEB_SERVER);
    assertThat(properties.naming).isEqualTo(Naming.RFC_7239);
    assertThat(properties.usePrefix).isTrue();
  }

  @Test
  void defaultsDoNotSelectStrategy() {
    var properties = Binder.get(new MockEnvironment()).bindOrCreate("web", WebProperties.class).forwardedHeaders;
    assertThat(properties.strategy).isNull();
    assertThat(properties.naming).isEqualTo(Naming.X_FORWARDED);
    assertThat(properties.usePrefix).isFalse();
  }

  @Test
  void oldServerPropertyDoesNotSelectStrategy() {
    var environment = new MockEnvironment().withProperty("server.request.header.forwarded.strategy", "framework");
    var properties = Binder.get(environment).bindOrCreate("web", WebProperties.class);
    assertThat(properties.forwardedHeaders.strategy).isNull();
  }

}
