/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.json;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import infra.test.classpath.resources.ResourceContent;
import infra.test.classpath.resources.WithPackageResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Base for {@link JsonParser} tests.
 *
 * @author Dave Syer
 * @author Jean de Klerk
 * @author Stephane Nicoll
 */
abstract class AbstractJsonParserTests {

  private final JsonParser parser = getParser();

  protected abstract JsonParser getParser();

  @Test
  void simpleMap() {
    Map<String, Object> map = this.parser.parseMap("{\"foo\":\"bar\",\"spam\":1}");
    assertThat(map).hasSize(2);
    assertThat(map).containsEntry("foo", "bar");
    Object spam = map.get("spam");
    assertThat(spam).isNotNull();
    assertThat(((Number) spam).longValue()).isOne();
  }

  @Test
  void doubleValue() {
    Map<String, Object> map = this.parser.parseMap("{\"foo\":\"bar\",\"spam\":1.23}");
    assertThat(map).hasSize(2);
    assertThat(map).containsEntry("foo", "bar");
    assertThat(map).containsEntry("spam", 1.23d);
  }

  @Test
  void stringContainingNumber() {
    Map<String, Object> map = this.parser.parseMap("{\"foo\":\"123\"}");
    assertThat(map).hasSize(1);
    assertThat(map).containsEntry("foo", "123");
  }

  @Test
  void stringContainingComma() {
    Map<String, Object> map = this.parser.parseMap("{\"foo\":\"bar1,bar2\"}");
    assertThat(map).hasSize(1);
    assertThat(map).containsEntry("foo", "bar1,bar2");
  }

  @Test
  void stringContainingOpeningBrace() {
    Map<String, Object> map = this.parser.parseMap("{\"foo\":\"{\",\"bar\":\"baz\"}");
    assertThat(map).hasSize(2);
    assertThat(map).containsEntry("foo", "{");
    assertThat(map).containsEntry("bar", "baz");
  }

  @Test
  void stringContainingOpeningBracket() {
    Map<String, Object> map = this.parser.parseMap("{\"foo\":\"[\",\"bar\":\"baz\"}");
    assertThat(map).hasSize(2);
    assertThat(map).containsEntry("foo", "[");
    assertThat(map).containsEntry("bar", "baz");
  }

  @Test
  void listWithStringContainingOpeningBracket() {
    List<Object> list = this.parser.parseList("[\"[\",\"baz\"]");
    assertThat(list).containsExactly("[", "baz");
  }

  @Test
  void stringContainingClosingBraceWithNestedMap() {
    Map<String, Object> map = this.parser.parseMap("{\"a\":\"}\",\"b\":{\"c\":\"d\",\"e\":\"f\"}}");
    assertThat(map).containsEntry("a", "}");
    assertThat(map.get("b")).asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("c", "d")
            .containsEntry("e", "f");
  }

  @Test
  void emptyMap() {
    Map<String, Object> map = this.parser.parseMap("{}");
    assertThat(map).isEmpty();
  }

  @Test
  void simpleList() {
    List<Object> list = this.parser.parseList("[\"foo\",\"bar\",1]");
    assertThat(list).hasSize(3);
    assertThat(list.get(1)).isEqualTo("bar");
  }

  @Test
  void emptyList() {
    List<Object> list = this.parser.parseList("[]");
    assertThat(list).isEmpty();
  }

  @Test
  void listOfMaps() {
    List<Object> list = this.parser.parseList("[{\"foo\":\"bar\",\"spam\":1},{\"foo\":\"baz\",\"spam\":2}]");
    assertThat(list).hasSize(2);
    assertThat(list.get(1)).asInstanceOf(InstanceOfAssertFactories.MAP).hasSize(2);
  }

  @Test
  void mapOfLists() {
    Map<String, Object> map = this.parser
            .parseMap("{\"foo\":[{\"foo\":\"bar\",\"spam\":1},{\"foo\":\"baz\",\"spam\":2}]}");
    assertThat(map).hasSize(1);
    assertThat(map.get("foo")).asInstanceOf(InstanceOfAssertFactories.LIST)
            .hasSize(2)
            .allMatch(Map.class::isInstance);
  }

  @Test
  void nestedLeadingAndTrailingWhitespace() {
    Map<String, Object> map = this.parser
            .parseMap(" {\"foo\": [ { \"foo\" : \"bar\" , \"spam\" : 1 } , { \"foo\" : \"baz\" , \"spam\" : 2 } ] } ");
    assertThat(map).hasSize(1);
    assertThat(map.get("foo")).asInstanceOf(InstanceOfAssertFactories.LIST)
            .hasSize(2)
            .allMatch(Map.class::isInstance);
  }

  @Test
  void listWithWhitespaceBeforeComma() {
    List<Object> list = this.parser.parseList("[\"foo\" ,\"bar\"]");
    assertThat(list).containsExactly("foo", "bar");
  }

  @Test
  void listOfNumbersWithWhitespaceBeforeComma() {
    List<Object> list = this.parser.parseList("[1 ,2]");
    assertThat(list).hasSize(2);
    assertThat(list).extracting(element -> ((Number) element).longValue()).containsExactly(1L, 2L);
    assertThat(list).isEqualTo(this.parser.parseList("[1,2]"));
  }

  @Test
  void listOfNumbersWithWhitespaceAfterComma() {
    List<Object> list = this.parser.parseList("[1, 2, 3]");
    assertThat(list).hasSize(3);
    assertThat(list).extracting(element -> ((Number) element).longValue()).containsExactly(1L, 2L, 3L);
    assertThat(list).isEqualTo(this.parser.parseList("[1,2,3]"));
  }

  @Test
  void listOfMapsWithWhitespaceAfterComma() {
    List<Object> list = this.parser.parseList("[{\"foo\":\"bar\"}, {\"foo\":\"baz\"}, {\"foo\":\"qux\"}]");
    assertThat(list).hasSize(3);
    assertThat(list.get(0)).isEqualTo(Map.of("foo", "bar"));
    assertThat(list.get(1)).isEqualTo(Map.of("foo", "baz"));
    assertThat(list.get(2)).isEqualTo(Map.of("foo", "qux"));
  }

  @Test
  void listOfMapsWithWhitespaceBeforeComma() {
    List<Object> list = this.parser.parseList("[{\"foo\":\"bar\"} ,{\"foo\":\"baz\"}]");
    assertThat(list).hasSize(2);
    assertThat(list.get(0)).isEqualTo(Map.of("foo", "bar"));
    assertThat(list.get(1)).isEqualTo(Map.of("foo", "baz"));
  }

  @Test
  void mapOfListsWithWhitespaceBeforeComma() {
    Map<String, Object> map = this.parser.parseMap("{\"foo\":[\"bar\" ,\"baz\"]}");
    assertThat(map.get("foo")).asInstanceOf(InstanceOfAssertFactories.LIST).containsExactly("bar", "baz");
  }

  @Test
  void mapWithNullThrowsARuntimeException() {
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseMap(null));
  }

  @Test
  void listWithNullThrowsARuntimeException() {
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseList(null));
  }

  @Test
  void mapWithEmptyStringThrowsARuntimeException() {
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseMap(""));
  }

  @Test
  void listWithEmptyStringThrowsARuntimeException() {
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseList(""));
  }

  @Test
  void mapWithListThrowsARuntimeException() {
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseMap("[]"));
  }

  @Test
  void listWithMapThrowsARuntimeException() {
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseList("{}"));
  }

  @Test
  void listWithLeadingWhitespace() {
    List<Object> list = this.parser.parseList("\n\t[\"foo\"]");
    assertThat(list).hasSize(1);
    assertThat(list.get(0)).isEqualTo("foo");
  }

  @Test
  void mapWithLeadingWhitespace() {
    Map<String, Object> map = this.parser.parseMap("\n\t{\"foo\":\"bar\"}");
    assertThat(map).hasSize(1);
    assertThat(map).containsEntry("foo", "bar");
  }

  @Test
  void mapWithLeadingWhitespaceListThrowsARuntimeException() {
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseMap("\n\t[]"));
  }

  @Test
  void listWithLeadingWhitespaceMapThrowsARuntimeException() {
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.parser.parseList("\n\t{}"));
  }

  @Test
  void escapeBackslash() {
    String input = """
            {
              "a": "alpha\\\\"
            }
            """;
    Map<String, Object> map = this.parser.parseMap(input);
    assertThat(map).containsEntry("a", "alpha\\");
  }

  @Test
  void escapeDoubleQuote() {
    String input = """
            {
              "a": "alpha\\\""
            }
            """;
    Map<String, Object> map = this.parser.parseMap(input);
    assertThat(map).containsEntry("a", "alpha\"");
  }

  @Test
  void escapeBackslashInNestedMap() {
    String input = """
            {
              "outer": {
                "a": "alpha\\\\"
              }
            }
            """;
    Map<String, Object> map = this.parser.parseMap(input);
    assertThat(map).extractingByKey("outer", InstanceOfAssertFactories.MAP).containsEntry("a", "alpha\\");
  }

  @Test
  void escapeDoubleQuoteInNestedMap() {
    String input = """
            {
              "outer": {
                "a": "alpha\\\""
              }
            }
            """;
    Map<String, Object> map = this.parser.parseMap(input);
    assertThat(map).extractingByKey("outer", InstanceOfAssertFactories.MAP).containsEntry("a", "alpha\"");
  }

  @Test
  void escapeBackslashInNestedList() {
    String input = """
            {
              "list": [
                "alpha\\\\"
              ]
            }
            """;
    Map<String, Object> map = this.parser.parseMap(input);
    assertThat(map).extractingByKey("list", InstanceOfAssertFactories.LIST).containsExactly("alpha\\");
  }

  @Test
  void escapeDoubleQuoteInNestedList() {
    String input = """
            {
              "list": [
                "alpha\\\""
              ]
            }
            """;
    Map<String, Object> map = this.parser.parseMap(input);
    assertThat(map).extractingByKey("list", InstanceOfAssertFactories.LIST).containsExactly("alpha\"");
  }

  @Test
  void escapeDoubleQuoteInNestedMapWithSiblingEntry() {
    String input = """
            {
              "a": {
                "x\\"y": "v"
              },
              "c": "d"
            }
            """;
    Map<String, Object> map = this.parser.parseMap(input);
    assertThat(map).extractingByKey("a", InstanceOfAssertFactories.MAP).containsEntry("x\"y", "v");
    assertThat(map).containsEntry("c", "d");
  }

  @Test
  void escapeDoubleQuoteInNestedListWithSiblingEntry() {
    String input = """
            {
              "a": [
                "x\\"y",
                "z"
              ],
              "c": "d"
            }
            """;
    Map<String, Object> map = this.parser.parseMap(input);
    assertThat(map).extractingByKey("a", InstanceOfAssertFactories.LIST).containsExactly("x\"y", "z");
    assertThat(map).containsEntry("c", "d");
  }

  @Test
  void listWithMalformedMap() {
    assertThatExceptionOfType(JsonParseException.class)
            .isThrownBy(() -> this.parser.parseList("[tru,erqett,{\"foo\":fatrue,true,true,true,tr''ue}]"));
  }

  @Test
  void mapWithKeyAndNoValue() {
    assertThatExceptionOfType(JsonParseException.class).isThrownBy(() -> this.parser.parseMap("{\"foo\"}"));
  }

  @Test
  @WithPackageResources("repeated-open-array.txt")
  void listWithRepeatedOpenArray(@ResourceContent("repeated-open-array.txt") String input) {
    assertThatExceptionOfType(JsonParseException.class).isThrownBy(() -> this.parser.parseList(input))
            .havingCause()
            .withMessageContaining("too deeply nested");
  }

  @Test
  @WithPackageResources("large-malformed-json.txt")
  void largeMalformed(@ResourceContent("large-malformed-json.txt") String input) {
    assertThatExceptionOfType(JsonParseException.class).isThrownBy(() -> this.parser.parseList(input));
  }

  @Test
  @WithPackageResources("deeply-nested-map-json.txt")
  void deeplyNestedMap(@ResourceContent("deeply-nested-map-json.txt") String input) {
    assertThatExceptionOfType(JsonParseException.class).isThrownBy(() -> this.parser.parseList(input));
  }

}
