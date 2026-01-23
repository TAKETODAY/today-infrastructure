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

package infra.http.reactive.client.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.http.reactive.client.config.ReactiveHttpClientsProperties.Connector;
import infra.http.reactive.client.HttpComponentsClientHttpConnectorBuilder;
import infra.http.reactive.client.JdkClientHttpConnectorBuilder;
import infra.http.reactive.client.ReactorClientHttpConnectorBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveHttpClientsProperties}.
 *
 * @author Phillip Webb
 */
class ReactiveHttpClientsPropertiesTests {

  @Nested
  class ConnectorTests {

    @Test
    void reactorBuilder() {
      assertThat(Connector.REACTOR.builder()).isInstanceOf(ReactorClientHttpConnectorBuilder.class);
    }

    @Test
    void httpComponentsBuilder() {
      assertThat(Connector.HTTP_COMPONENTS.builder())
              .isInstanceOf(HttpComponentsClientHttpConnectorBuilder.class);
    }

    @Test
    void jdkBuilder() {
      assertThat(Connector.JDK.builder()).isInstanceOf(JdkClientHttpConnectorBuilder.class);
    }

  }

}
