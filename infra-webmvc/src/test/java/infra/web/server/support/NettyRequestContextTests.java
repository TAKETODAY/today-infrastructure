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

    NettyRequestContext.parseParameters(parameters, "1=1&=2&n=v");
    assertThat(parameters).hasSize(3).containsKey("n").containsKey("1").containsKey("2");
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

  @Test
  void parseParameters_withPercentEncodedValues_shouldDecodeCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "name=%E5%BC%A0%E4%B8%89&city=%E5%8C%97%E4%BA%AC");

    assertThat(params)
            .hasSize(2)
            .containsEntry("name", List.of("张三"))
            .containsEntry("city", List.of("北京"));
  }

  @Test
  void parseParameters_withReservedCharacters_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "q=Java%3A%24%26%2B%2C%2F%3F%23%5B%5D%40");

    assertThat(params)
            .hasSize(1)
            .containsEntry("q", List.of("Java:$&+,/?#[]@"));
  }

  @Test
  void parseParameters_withMalformedPercentEncoding_shouldThrowException() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();

    assertThatThrownBy(() -> NettyRequestContext.parseParameters(params, "invalid=%2"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unterminated escape sequence");

    assertThatThrownBy(() -> NettyRequestContext.parseParameters(params, "invalid=%2G"))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parseParameters_withSpaceEncoding_shouldHandlePlusAndPercent20() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "q1=hello+world&q2=hello%20world");

    assertThat(params)
            .hasSize(2)
            .containsEntry("q1", List.of("hello world"))
            .containsEntry("q2", List.of("hello world"));
  }

  @Test
  void parseParameters_withControlCharacters_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "data=%00%01%02%03%04%05");

    assertThat(params)
            .hasSize(1)
            .containsEntry("data", List.of("\u0000\u0001\u0002\u0003\u0004\u0005"));
  }

  @Test
  void parseParameters_withMultipleEncodedDelimiters_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key1=value%26one&key2=value%3Btwo");

    assertThat(params)
            .hasSize(2)
            .containsEntry("key1", List.of("value&one"))
            .containsEntry("key2", List.of("value;two"));
  }

  @Test
  void parseParameters_withNonAsciiCharactersInKeys_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "%E5%90%8D%E5%AD%97=zhang&%E5%B9%B4%E9%BE%84=20");

    assertThat(params)
            .hasSize(2)
            .containsEntry("名字", List.of("zhang"))
            .containsEntry("年龄", List.of("20"));
  }

  @Test
  void parseParameters_withMultipleEmptyValues_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key1=&key2=&key3=");

    assertThat(params)
            .hasSize(3)
            .containsEntry("key1", List.of(""))
            .containsEntry("key2", List.of(""))
            .containsEntry("key3", List.of(""));
  }

  @Test
  void parseParameters_withMultipleKeysNoValues_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key1&key2&key3");

    assertThat(params)
            .hasSize(3)
            .containsEntry("key1", List.of(""))
            .containsEntry("key2", List.of(""))
            .containsEntry("key3", List.of(""));
  }

  @Test
  void parseParameters_withComplexEncodedCharacters_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key=%E2%98%83%E2%9C%88%F0%9F%98%80");

    assertThat(params)
            .hasSize(1)
            .containsEntry("key", List.of("☃✈😀"));
  }

  @Test
  void parseParameters_withNullBytes_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key1=abc%00def&key2=ghi%00jkl");

    assertThat(params)
            .hasSize(2)
            .containsEntry("key1", List.of("abc\u0000def"))
            .containsEntry("key2", List.of("ghi\u0000jkl"));
  }

  @Test
  void parseParameters_withLongChainedParams_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    StringBuilder longValue = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      longValue.append("a");
    }
    NettyRequestContext.parseParameters(params, "key=" + longValue);

    assertThat(params)
            .hasSize(1)
            .containsEntry("key", List.of(longValue.toString()));
  }

  @Test
  void parseParameters_withConsecutiveDelimiters_shouldParseCorrectly() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key1=value1&&&&key2=value2;;;;key3=value3");

    assertThat(params)
            .hasSize(3)
            .containsEntry("key1", List.of("value1"))
            .containsEntry("key2", List.of("value2"))
            .containsEntry("key3", List.of("value3"));
  }

  @Test
  void parseParameters_withSemicolonAndFlagTrue_shouldTreatSemicolonAsNormal() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key1=value;1&key2=value;2", true);

    assertThat(params)
            .hasSize(2)
            .containsEntry("key1", List.of("value;1"))
            .containsEntry("key2", List.of("value;2"));
  }

  @Test
  void parseParameters_withMultipleSemicolonsAndFlagTrue_shouldNotSplitOnSemicolons() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key=a;b;c;d", true);

    assertThat(params)
            .hasSize(1)
            .containsEntry("key", List.of("a;b;c;d"));
  }

  @Test
  void parseParameters_withSemicolonInKeyAndFlagTrue_shouldTreatAsNormal() {
    MultiValueMap<String, String> params = MultiValueMap.forLinkedHashMap();
    NettyRequestContext.parseParameters(params, "key;with;semicolons=value", true);

    assertThat(params)
            .hasSize(1)
            .containsEntry("key;with;semicolons", List.of("value"));

  }

}