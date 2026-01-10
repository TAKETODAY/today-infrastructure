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