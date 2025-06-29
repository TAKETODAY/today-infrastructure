/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.properties.source;

import org.junit.jupiter.api.Test;

import java.util.function.BiPredicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemEnvironmentPropertyMapper}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class SystemEnvironmentPropertyMapperTests extends AbstractPropertyMapperTests {

  @Override
  protected PropertyMapper getMapper() {
    return SystemEnvironmentPropertyMapper.INSTANCE;
  }

  @Test
  void mapFromStringShouldReturnBestGuess() {
    assertThat(mapPropertySourceName("SERVER")).isEqualTo("server");
    assertThat(mapPropertySourceName("SERVER_PORT")).isEqualTo("server.port");
    assertThat(mapPropertySourceName("HOST_0")).isEqualTo("host[0]");
    assertThat(mapPropertySourceName("HOST_0_1")).isEqualTo("host[0][1]");
    assertThat(mapPropertySourceName("HOST_0_NAME")).isEqualTo("host[0].name");
    assertThat(mapPropertySourceName("HOST_F00_NAME")).isEqualTo("host.f00.name");
    assertThat(mapPropertySourceName("S-ERVER")).isEqualTo("s-erver");
  }

  @Test
  void mapFromConfigurationShouldReturnBestGuess() {
    assertThat(mapConfigurationPropertyName("server")).containsExactly("SERVER", "server");
    assertThat(mapConfigurationPropertyName("server.port")).containsExactly("SERVER_PORT", "server_port");
    assertThat(mapConfigurationPropertyName("host[0]")).containsExactly("HOST_0", "host_0");
    assertThat(mapConfigurationPropertyName("host[0][1]")).containsExactly("HOST_0_1", "host_0_1");
    assertThat(mapConfigurationPropertyName("host[0].name")).containsExactly("HOST_0_NAME", "host_0_name");
    assertThat(mapConfigurationPropertyName("host.f00.name")).containsExactly("HOST_F00_NAME", "host_f00_name");
    assertThat(mapConfigurationPropertyName("foo.the-bar")).containsExactly("FOO_THEBAR", "FOO_THE_BAR",
            "foo_thebar", "foo_the_bar");
  }

  @Test
  void underscoreShouldMapToEmptyString() {
    ConfigurationPropertyName mapped = getMapper().map("_");
    assertThat(mapped.isEmpty()).isTrue();
  }

  @Test
  void underscoreWithWhitespaceShouldMapToEmptyString() {
    ConfigurationPropertyName mapped = getMapper().map(" _");
    assertThat(mapped.isEmpty()).isTrue();
  }

  @Test
  void isAncestorOfConsidersLegacyNames() {
    ConfigurationPropertyName name = ConfigurationPropertyName.of("my.spring-boot");
    BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> check = getMapper().getAncestorOfCheck();
    assertThat(check.test(name, ConfigurationPropertyName.adapt("MY_SPRING_BOOT_PROPERTY", '_'))).isTrue();
    assertThat(check.test(name, ConfigurationPropertyName.adapt("MY_SPRINGBOOT_PROPERTY", '_'))).isTrue();
    assertThat(check.test(name, ConfigurationPropertyName.adapt("MY_BOOT_PROPERTY", '_'))).isFalse();
  }

  @Test
  void isAncestorOfWhenNonCanonicalSource() {
    ConfigurationPropertyName name = ConfigurationPropertyName.adapt("my.springBoot", '.');
    BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> check = getMapper().getAncestorOfCheck();
    assertThat(check.test(name, ConfigurationPropertyName.of("my.spring-boot.property"))).isTrue();
    assertThat(check.test(name, ConfigurationPropertyName.of("my.springboot.property"))).isTrue();
    assertThat(check.test(name, ConfigurationPropertyName.of("my.boot.property"))).isFalse();
  }

  @Test
  void isAncestorOfWhenNonCanonicalAndDashedSource() {
    ConfigurationPropertyName name = ConfigurationPropertyName.adapt("my.springBoot.input-value", '.');
    BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> check = getMapper().getAncestorOfCheck();
    assertThat(check.test(name, ConfigurationPropertyName.of("my.spring-boot.input-value.property"))).isTrue();
    assertThat(check.test(name, ConfigurationPropertyName.of("my.springboot.inputvalue.property"))).isTrue();
    assertThat(check.test(name, ConfigurationPropertyName.of("my.boot.property"))).isFalse();
  }

}
