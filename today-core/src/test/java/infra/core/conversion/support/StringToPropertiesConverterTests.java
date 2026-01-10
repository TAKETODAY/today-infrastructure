/*
 * Copyright 2017 - 2026 the TODAY authors.
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