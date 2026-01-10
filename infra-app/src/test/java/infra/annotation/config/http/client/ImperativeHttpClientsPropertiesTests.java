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

package infra.annotation.config.http.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.annotation.config.http.client.ImperativeHttpClientsProperties.Factory;
import infra.http.client.config.HttpComponentsClientHttpRequestFactoryBuilder;
import infra.http.client.config.JdkClientHttpRequestFactoryBuilder;
import infra.http.client.config.ReactorClientHttpRequestFactoryBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImperativeHttpClientsProperties}.
 *
 * @author Phillip Webb
 */
class ImperativeHttpClientsPropertiesTests {

  @Nested
  class FactoryTests {

    @Test
    void httpComponentsBuilder() {
      assertThat(Factory.HTTP_COMPONENTS.builder())
              .isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
    }

    @Test
    void reactorBuilder() {
      assertThat(Factory.REACTOR.builder()).isInstanceOf(ReactorClientHttpRequestFactoryBuilder.class);
    }

    @Test
    void jdkBuilder() {
      assertThat(Factory.JDK.builder()).isInstanceOf(JdkClientHttpRequestFactoryBuilder.class);
    }

  }

}
