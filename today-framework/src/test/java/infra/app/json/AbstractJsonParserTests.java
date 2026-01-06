/*
 * Copyright 2017 - 2026 the original author or authors.
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

  @SuppressWarnings("unchecked")
  @Test
  void listOfMaps() {
    List<Object> list = this.parser.parseList("[{\"foo\":\"bar\",\"spam\":1},{\"foo\":\"baz\",\"spam\":2}]");
    assertThat(list).hasSize(2);
    assertThat(((Map<String, Object>) list.get(1))).hasSize(2);
  }

  @SuppressWarnings("unchecked")
  @Test
  void mapOfLists() {
    Map<String, Object> map = this.parser
            .parseMap("{\"foo\":[{\"foo\":\"bar\",\"spam\":1},{\"foo\":\"baz\",\"spam\":2}]}");
    assertThat(map).hasSize(1);
    assertThat(((List<Object>) map.get("foo"))).hasSize(2);
    assertThat(map.get("foo")).asInstanceOf(InstanceOfAssertFactories.LIST).allMatch(Map.class::isInstance);
  }

  @SuppressWarnings("unchecked")
  @Test
  void nestedLeadingAndTrailingWhitespace() {
    Map<String, Object> map = this.parser
            .parseMap(" {\"foo\": [ { \"foo\" : \"bar\" , \"spam\" : 1 } , { \"foo\" : \"baz\" , \"spam\" : 2 } ] } ");
    assertThat(map).hasSize(1);
    assertThat(((List<Object>) map.get("foo"))).hasSize(2);
    assertThat(map.get("foo")).asInstanceOf(InstanceOfAssertFactories.LIST).allMatch(Map.class::isInstance);
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
  void escapeDoubleQuote() {
    String input = "{\"foo\": \"\\\"bar\\\"\"}";
    Map<String, Object> map = this.parser.parseMap(input);
    assertThat(map).containsEntry("foo", "\"bar\"");
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
