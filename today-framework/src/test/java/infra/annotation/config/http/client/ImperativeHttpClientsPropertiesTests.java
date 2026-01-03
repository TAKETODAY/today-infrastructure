/*
 * Copyright 2017 - 2026 the original author or authors.
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
