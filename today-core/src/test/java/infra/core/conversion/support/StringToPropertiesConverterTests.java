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

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/19 22:52
 */
class StringToPropertiesConverterTests {

  @Test
  void convertSimpleStringToProperties() {
    StringToPropertiesConverter converter = new StringToPropertiesConverter();
    String source = "key1=value1\nkey2=value2";

    Properties result = converter.convert(source);

    assertThat(result).isNotNull();
    assertThat(result.getProperty("key1")).isEqualTo("value1");
    assertThat(result.getProperty("key2")).isEqualTo("value2");
  }

  @Test
  void convertStringWithCommentsToProperties() {
    StringToPropertiesConverter converter = new StringToPropertiesConverter();
    String source = "# This is a comment\nkey1=value1\n! Another comment\nkey2=value2";

    Properties result = converter.convert(source);

    assertThat(result).isNotNull();
    assertThat(result.getProperty("key1")).isEqualTo("value1");
    assertThat(result.getProperty("key2")).isEqualTo("value2");
  }

  @Test
  void convertStringWithSpacesToProperties() {
    StringToPropertiesConverter converter = new StringToPropertiesConverter();
    String source = "key1 = value1\nkey2 : value2\nkey3=value3";

    Properties result = converter.convert(source);

    assertThat(result).isNotNull();
    assertThat(result.getProperty("key1")).isEqualTo("value1");
    assertThat(result.getProperty("key2")).isEqualTo("value2");
    assertThat(result.getProperty("key3")).isEqualTo("value3");
  }

  @Test
  void convertEmptyStringToProperties() {
    StringToPropertiesConverter converter = new StringToPropertiesConverter();
    String source = "";

    Properties result = converter.convert(source);

    assertThat(result).isNotNull();
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void convertStringWithSpecialCharactersToProperties() {
    StringToPropertiesConverter converter = new StringToPropertiesConverter();
    String source = "key1=value\\ with\\ spaces\nkey2=value\\:with\\=equals";

    Properties result = converter.convert(source);

    assertThat(result).isNotNull();
    assertThat(result.getProperty("key1")).isEqualTo("value with spaces");
    assertThat(result.getProperty("key2")).isEqualTo("value:with=equals");
  }

  @Test
  void convertStringWithUnicodeCharactersToProperties() {
    StringToPropertiesConverter converter = new StringToPropertiesConverter();
    String source = "key1=\\u4E2D\\u6587\nkey2=\\u00A9";

    Properties result = converter.convert(source);

    assertThat(result).isNotNull();
    assertThat(result.getProperty("key1")).isEqualTo("中文");
    assertThat(result.getProperty("key2")).isEqualTo("©");
  }

  @Test
  void convertMalformedStringThrowsException() {
    StringToPropertiesConverter converter = new StringToPropertiesConverter();

    assertThatIllegalArgumentException()
            .isThrownBy(() -> converter.convert(null))
            .withMessageContaining("Failed to parse");
  }

}