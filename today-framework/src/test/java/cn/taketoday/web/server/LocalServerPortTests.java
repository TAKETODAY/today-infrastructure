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

package cn.taketoday.web.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.test.web.server.LocalServerPort;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LocalServerPort @LocalServerPort}.
 *
 * @author Anand Shah
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
@TestPropertySource(properties = "local.server.port=8181")
class LocalServerPortTests {

  @Value("${local.server.port}")
  private String fromValue;

  @LocalServerPort
  private String fromAnnotation;

  @Test
  void testLocalServerPortAnnotation() {
    assertThat(this.fromAnnotation).isNotNull().isEqualTo(this.fromValue);
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

  }

}
