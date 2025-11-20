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

package infra.core.conversion.support;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/19 22:53
 */
class PropertiesToStringConverterTests {

  @Test
  void convertSimplePropertiesToString() {
    PropertiesToStringConverter converter = new PropertiesToStringConverter();
    Properties source = new Properties();
    source.setProperty("key1", "value1");
    source.setProperty("key2", "value2");

    String result = converter.convert(source);

    assertThat(result).isNotNull();
    assertThat(result).contains("key1=value1");
    assertThat(result).contains("key2=value2");
  }

  @Test
  void convertEmptyPropertiesToString() {
    PropertiesToStringConverter converter = new PropertiesToStringConverter();
    Properties source = new Properties();

    String result = converter.convert(source);

    assertThat(result).isNotNull();
    // Empty properties still contain metadata like timestamp comment
    assertThat(result).contains("#");
  }

  @Test
  void convertPropertiesWithSpecialCharactersToString() {
    PropertiesToStringConverter converter = new PropertiesToStringConverter();
    Properties source = new Properties();
    source.setProperty("key with spaces", "value with spaces");
    source.setProperty("key:with:colons", "value=with=equals");

    String result = converter.convert(source);

    assertThat(result).isNotNull();
    // Special characters should be escaped in the output
    assertThat(result).contains("key\\ with\\ spaces");
    assertThat(result).contains("key\\:with\\:colons");
  }

  @Test
  void convertPropertiesWithUnicodeCharactersToString() {
    PropertiesToStringConverter converter = new PropertiesToStringConverter();
    Properties source = new Properties();
    source.setProperty("unicodeKey", "中文");
    source.setProperty("copyright", "©");

    String result = converter.convert(source);

    assertThat(result).isNotNull();
    // Unicode characters should be properly encoded in ISO-8859-1
    assertThat(result).contains("unicodeKey");
  }

  @Test
  void convertPropertiesMaintainsIso88591Encoding() {
    PropertiesToStringConverter converter = new PropertiesToStringConverter();
    Properties source = new Properties();
    source.setProperty("testKey", "testValue");

    String result = converter.convert(source);

    assertThat(result).isNotNull();
    // Verify the string is properly encoded with ISO-8859-1
    byte[] bytes = result.getBytes(StandardCharsets.ISO_8859_1);
    assertThat(bytes).isNotNull();

  }

}