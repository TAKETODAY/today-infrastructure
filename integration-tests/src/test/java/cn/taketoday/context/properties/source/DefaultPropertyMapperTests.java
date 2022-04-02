/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.context.properties.source;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultPropertyMapper}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class DefaultPropertyMapperTests extends AbstractPropertyMapperTests {

  @Override
  protected PropertyMapper getMapper() {
    return DefaultPropertyMapper.INSTANCE;
  }

  @Test
  void mapFromStringShouldReturnBestGuess() {
    assertThat(mapPropertySourceName("server")).isEqualTo("server");
    assertThat(mapPropertySourceName("server.port")).isEqualTo("server.port");
    assertThat(mapPropertySourceName("host[0]")).isEqualTo("host[0]");
    assertThat(mapPropertySourceName("host[0][1]")).isEqualTo("host[0][1]");
    assertThat(mapPropertySourceName("host[0].name")).isEqualTo("host[0].name");
    assertThat(mapPropertySourceName("host.f00.name")).isEqualTo("host.f00.name");
    assertThat(mapPropertySourceName("my.host-name")).isEqualTo("my.host-name");
    assertThat(mapPropertySourceName("my.hostName")).isEqualTo("my.hostname");
    assertThat(mapPropertySourceName("my.HOST_NAME")).isEqualTo("my.hostname");
    assertThat(mapPropertySourceName("s[!@#$%^&*()=+]e-rVeR")).isEqualTo("s[!@#$%^&*()=+].e-rver");
    assertThat(mapPropertySourceName("host[FOO].name")).isEqualTo("host[FOO].name");
  }

  @Test
  void mapFromConfigurationShouldReturnBestGuess() {
    assertThat(mapConfigurationPropertyName("server")).containsExactly("server");
    assertThat(mapConfigurationPropertyName("server.port")).containsExactly("server.port");
    assertThat(mapConfigurationPropertyName("host[0]")).containsExactly("host[0]");
    assertThat(mapConfigurationPropertyName("host[0][1]")).containsExactly("host[0][1]");
    assertThat(mapConfigurationPropertyName("host[0].name")).containsExactly("host[0].name");
    assertThat(mapConfigurationPropertyName("host.f00.name")).containsExactly("host.f00.name");
    assertThat(mapConfigurationPropertyName("my.host-name")).containsExactly("my.host-name");
    assertThat(mapConfigurationPropertyName("host[FOO].name")).containsExactly("host[FOO].name");
  }

}
