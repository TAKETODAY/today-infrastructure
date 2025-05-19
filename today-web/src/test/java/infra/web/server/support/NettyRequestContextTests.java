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

package infra.web.server.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.util.MultiValueMap;
import io.netty.handler.codec.http.QueryStringEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/5/19 16:56
 */
class NettyRequestContextTests {

  @Test
  void parseParameters() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();

    NettyRequestContext.parseParameters(parameters, "most-popular");
    assertThat(parameters).hasSize(1).containsKey("most-popular");

    parameters.clear();
    NettyRequestContext.parseParameters(parameters, "most-popular&name=value&name=");
    assertThat(parameters).hasSize(2).containsKeys("name", "most-popular")
            .containsValues(List.of(""), List.of("value", ""));

    parameters.clear();

    NettyRequestContext.parseParameters(parameters, "most-popular&name=value&name=");
    assertThat(parameters).hasSize(2).containsKeys("name", "most-popular")
            .containsValues(List.of(""), List.of("value", ""));

  }

  @Test
  void parseParameters_withEmptyString_shouldNotAddParameters() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "");
    assertThat(parameters).isEmpty();
  }

  @Test
  void parseParameters_withSingleKeyValuePair_shouldParseCorrectly() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "key=value");
    assertThat(parameters).hasSize(1)
            .containsEntry("key", List.of("value"));
  }

  @Test
  void parseParameters_withMultipleKeyValuePairs_shouldParseCorrectly() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "key1=value1&key2=value2;key3=value3");
    assertThat(parameters).hasSize(3)
            .containsEntry("key1", List.of("value1"))
            .containsEntry("key2", List.of("value2"))
            .containsEntry("key3", List.of("value3"));
  }

  @Test
  void parseParameters_withEmptyValue_shouldParseCorrectly() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "key=");
    assertThat(parameters).hasSize(1)
            .containsEntry("key", List.of(""));
  }

  @Test
  void parseParameters_withKeyOnly_shouldParseCorrectly() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "key");
    assertThat(parameters).hasSize(1)
            .containsEntry("key", List.of(""));
  }

  @Test
  void parseParameters_withMultipleEquals_shouldParseCorrectly() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "key=value=more");
    assertThat(parameters).hasSize(1)
            .containsEntry("key", List.of("value=more"));
  }

  @Test
  void parseParameters_withFragmentInUrl_shouldIgnoreFragmentPart() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(parameters, "key1=value1#key2=value2");
    assertThat(parameters).hasSize(1)
            .containsEntry("key1", List.of("value1"));
  }

  @Test
  void parseParameters_withSpecialCharactersInKeyAndValue_shouldParseCorrectly() {
    MultiValueMap<String, String> parameters = MultiValueMap.forLinkedHashMap();
    QueryStringEncoder encoder = new QueryStringEncoder("");
    encoder.addParam("key@123", "value$456");
    encoder.addParam("key#789", "value%0A");
    NettyRequestContext.parseParameters(parameters, encoder.toString().substring(1));
    assertThat(parameters).hasSize(2)
            .containsEntry("key@123", List.of("value$456"))
            .containsEntry("key#789", List.of("value%0A"));
  }

  @Test
  void parseParameters_withUTF8SpecialCharacters_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "name=张三&age=25&city=北京");

    assertThat(params)
            .hasSize(3)
            .containsEntry("name", List.of("张三"))
            .containsEntry("age", List.of("25"))
            .containsEntry("city", List.of("北京"));
  }

  @Test
  void parseParameters_withEncodedCharacters_shouldDecodeCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "title=Hello+World&q=Java%2BSpring");

    assertThat(params)
            .hasSize(2)
            .containsEntry("title", List.of("Hello World"))
            .containsEntry("q", List.of("Java+Spring"));
  }

  @Test
  void parseParameters_withMixedDelimiters_shouldParseAllParameters() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "a=1&b=2;c=3&d=4");

    assertThat(params)
            .hasSize(4)
            .containsEntry("a", List.of("1"))
            .containsEntry("b", List.of("2"))
            .containsEntry("c", List.of("3"))
            .containsEntry("d", List.of("4"));
  }

  @Test
  void parseParameters_withEmptyValues_shouldParseAsEmptyStrings() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "a=&b&c=");

    assertThat(params)
            .hasSize(3)
            .containsEntry("a", List.of(""))
            .containsEntry("b", List.of(""))
            .containsEntry("c", List.of(""));
  }

  @Test
  void parseParameters_withEqualSignInValue_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "equation=x=1&formula=a=b=c");

    assertThat(params)
            .hasSize(2)
            .containsEntry("equation", List.of("x=1"))
            .containsEntry("formula", List.of("a=b=c"));
  }

  @Test
  void parseParameters_withMoreThanMaxParams_shouldLimitParameters() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    StringBuilder query = new StringBuilder();
    for (int i = 0; i < 2000; i++) {
      query.append("p").append(i).append("=").append(i).append("&");
    }

    NettyRequestContext.parseParameters(params, query.toString());
    assertThat(params).hasSize(1024);
  }

  @Test
  void parseParameters_withFragmentIdentifier_shouldIgnoreFragment() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "a=1&b=2#fragment&c=3");

    assertThat(params)
            .hasSize(2)
            .containsEntry("a", List.of("1"))
            .containsEntry("b", List.of("2"))
            .doesNotContainKey("c");
  }

  @Test
  void parseParameters_withMalformedEncoding_shouldThrowIllegalArgumentException() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();

    assertThatThrownBy(() -> NettyRequestContext.parseParameters(params, "key=%2"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unterminated escape sequence");
  }

}