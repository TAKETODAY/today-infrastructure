/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.test.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import infra.core.ParameterizedTypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link JsonPathExpectationsHelper}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
class JsonPathExpectationsHelperTests {

  private static final Configuration JACKSON_MAPPING_CONFIGURATION = Configuration.defaultConfiguration()
          .mappingProvider(new JacksonMappingProvider(new ObjectMapper()));

  private static final String CONTENT = """
          {
          	'str':         'foo',
          	'num':         5,
          	'bool':        true,
          	'arr':         [42],
          	'colorMap':    {'red': 'rojo'},
          	'whitespace':  '    ',
          	'emptyString': '',
          	'emptyArray':  [],
          	'emptyMap':    {}
          }""";

  private static final String SIMPSONS = """
          {
          	'familyMembers': [
          		{'name': 'Homer' },
          		{'name': 'Marge' },
          		{'name': 'Bart'  },
          		{'name': 'Lisa'  },
          		{'name': 'Maggie'}
          	]
          }""";

  @ParameterizedTest
  @ValueSource(strings = { "$.str", "$.emptyArray", "$.emptyMap" })
  void exists(String expression) {
    new JsonPathExpectationsHelper(expression).exists(CONTENT);
  }

  @Test
  void existsForIndefinitePathWithResults() {
    new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Bart')]").exists(SIMPSONS);
  }

  @Test
  void existsForIndefinitePathWithEmptyResults() {
    String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).exists(SIMPSONS))
            .withMessageContaining("No value at JSON path \"" + expression + "\"");
  }

  @ParameterizedTest
  @ValueSource(strings = { "$.bogus" })
  void doesNotExist(String expression) {
    new JsonPathExpectationsHelper(expression).doesNotExist(CONTENT);
  }

  @Test
  void doesNotExistForAnEmptyArray() {
    String expression = "$.emptyArray";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).doesNotExist(CONTENT))
            .withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: []");
  }

  @Test
  void doesNotExistForAnEmptyMap() {
    String expression = "$.emptyMap";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).doesNotExist(CONTENT))
            .withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: {}");
  }

  @Test
  void doesNotExistForIndefinitePathWithResults() {
    String expression = "$.familyMembers[?(@.name == 'Bart')]";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).doesNotExist(SIMPSONS))
            .withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: [{\"name\":\"Bart\"}]");
  }

  @Test
  void doesNotExistForIndefinitePathWithEmptyResults() {
    new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Dilbert')]").doesNotExist(SIMPSONS);
  }

  @ParameterizedTest
  @ValueSource(strings = { "$.emptyString", "$.emptyArray", "$.emptyMap" })
  void valueIsEmpty(String expression) {
    new JsonPathExpectationsHelper(expression).assertValueIsEmpty(CONTENT);
  }

  @Test
  void assertValueIsEmptyForIndefinitePathWithEmptyResults() {
    new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Dilbert')]").assertValueIsEmpty(SIMPSONS);
  }

  @Test
  void assertValueIsEmptyForIndefinitePathWithResults() {
    String expression = "$.familyMembers[?(@.name == 'Bart')]";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).assertValueIsEmpty(SIMPSONS))
            .withMessageContaining("Expected an empty value at JSON path \"" + expression + "\" but found: [{\"name\":\"Bart\"}]");
  }

  @Test
  void assertValueIsEmptyForWhitespace() {
    String expression = "$.whitespace";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).assertValueIsEmpty(CONTENT))
            .withMessageContaining("Expected an empty value at JSON path \"" + expression + "\" but found: '    '");
  }

  @ParameterizedTest
  @ValueSource(strings = { "$.str", "$.num", "$.bool", "$.arr", "$.colorMap" })
  void valueIsNotEmpty(String expression) {
    new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(CONTENT);
  }

  @Test
  void assertValueIsNotEmptyForIndefinitePathWithResults() {
    new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Bart')]").assertValueIsNotEmpty(SIMPSONS);
  }

  @Test
  void assertValueIsNotEmptyForIndefinitePathWithEmptyResults() {
    String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(SIMPSONS))
            .withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: []");
  }

  @Test
  void assertValueIsNotEmptyForAnEmptyString() {
    String expression = "$.emptyString";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(CONTENT))
            .withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: ''");
  }

  @Test
  void assertValueIsNotEmptyForAnEmptyArray() {
    String expression = "$.emptyArray";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(CONTENT))
            .withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: []");
  }

  @Test
  void assertValueIsNotEmptyForAnEmptyMap() {
    String expression = "$.emptyMap";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(CONTENT))
            .withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: {}");
  }

  @Test
  void hasJsonPath() {
    new JsonPathExpectationsHelper("$.abc").hasJsonPath("{\"abc\": \"123\"}");
  }

  @Test
  void hasJsonPathWithNull() {
    new JsonPathExpectationsHelper("$.abc").hasJsonPath("{\"abc\": null}");
  }

  @Test
  void hasJsonPathForIndefinitePathWithResults() {
    new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Bart')]").hasJsonPath(SIMPSONS);
  }

  @Test
  void hasJsonPathForIndefinitePathWithEmptyResults() {
    String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).hasJsonPath(SIMPSONS))
            .withMessageContaining("No values for JSON path \"" + expression + "\"");
  }

  @Test

  void doesNotHaveJsonPath() {
    new JsonPathExpectationsHelper("$.abc").doesNotHaveJsonPath("{}");
  }

  @Test

  void doesNotHaveJsonPathWithNull() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
            new JsonPathExpectationsHelper("$.abc").doesNotHaveJsonPath("{\"abc\": null}"));
  }

  @Test
  void doesNotHaveJsonPathForIndefinitePathWithEmptyResults() {
    new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Dilbert')]").doesNotHaveJsonPath(SIMPSONS);
  }

  @Test
  void doesNotHaveEmptyPathForIndefinitePathWithResults() {
    String expression = "$.familyMembers[?(@.name == 'Bart')]";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).doesNotHaveJsonPath(SIMPSONS))
            .withMessageContaining("Expected no values at JSON path \"" + expression + "\" " + "but found: [{\"name\":\"Bart\"}]");
  }

  @Test
  void assertValue() {
    new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, 5);
  }

  @Test

  void assertValueWithNumberConversion() {
    new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, 5.0);
  }

  @Test

  void assertValueWithNumberConversionAndMatcher() {
    new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, is(5.0), Double.class);
  }

  @Test
  void assertValueIsString() {
    new JsonPathExpectationsHelper("$.str").assertValueIsString(CONTENT);
  }

  @Test
  void assertValueIsStringForAnEmptyString() {
    new JsonPathExpectationsHelper("$.emptyString").assertValueIsString(CONTENT);
  }

  @Test
  void assertValueIsStringForNonString() {
    String expression = "$.bool";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).assertValueIsString(CONTENT))
            .withMessageContaining("Expected a string at JSON path \"" + expression + "\" but found: true");
  }

  @Test
  void assertValueIsNumber() {
    new JsonPathExpectationsHelper("$.num").assertValueIsNumber(CONTENT);
  }

  @Test
  void assertValueIsNumberForNonNumber() {
    String expression = "$.bool";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).assertValueIsNumber(CONTENT))
            .withMessageContaining("Expected a number at JSON path \"" + expression + "\" but found: true");
  }

  @Test
  void assertValueIsBoolean() {
    new JsonPathExpectationsHelper("$.bool").assertValueIsBoolean(CONTENT);
  }

  @Test
  void assertValueIsBooleanForNonBoolean() {
    String expression = "$.num";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).assertValueIsBoolean(CONTENT))
            .withMessageContaining("Expected a boolean at JSON path \"" + expression + "\" but found: 5");
  }

  @Test
  void assertValueIsArray() {
    new JsonPathExpectationsHelper("$.arr").assertValueIsArray(CONTENT);
  }

  @Test
  void assertValueIsArrayForAnEmptyArray() {
    new JsonPathExpectationsHelper("$.emptyArray").assertValueIsArray(CONTENT);
  }

  @Test
  void assertValueIsArrayForNonArray() {
    String expression = "$.str";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).assertValueIsArray(CONTENT))
            .withMessageContaining("Expected an array at JSON path \"" + expression + "\" but found: 'foo'");
  }

  @Test
  void assertValueIsMap() {
    new JsonPathExpectationsHelper("$.colorMap").assertValueIsMap(CONTENT);
  }

  @Test
  void assertValueIsMapForAnEmptyMap() {
    new JsonPathExpectationsHelper("$.emptyMap").assertValueIsMap(CONTENT);
  }

  @Test
  void assertValueIsMapForNonMap() {
    String expression = "$.str";
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    new JsonPathExpectationsHelper(expression).assertValueIsMap(CONTENT))
            .withMessageContaining("Expected a map at JSON path \"" + expression + "\" but found: 'foo'");
  }

  @Test
  void assertValueWithComplexTypeFallbacksOnValueType() {
    new JsonPathExpectationsHelper("$.familyMembers[0]", JACKSON_MAPPING_CONFIGURATION)
            .assertValue(SIMPSONS, new Member("Homer"));
  }

  @Test
  void assertValueWithComplexTypeAndMatcher() {
    new JsonPathExpectationsHelper("$.familyMembers[0]", JACKSON_MAPPING_CONFIGURATION)
            .assertValue(SIMPSONS, CoreMatchers.instanceOf(Member.class), Member.class);
  }

  @Test
  void assertValueWithComplexGenericTypeAndMatcher() {
    JsonPathExpectationsHelper helper = new JsonPathExpectationsHelper("$.familyMembers", JACKSON_MAPPING_CONFIGURATION);
    helper.assertValue(SIMPSONS, hasSize(5), new ParameterizedTypeReference<List<Member>>() { });
    helper.assertValue(SIMPSONS, hasItem(new Member("Lisa")), new ParameterizedTypeReference<List<Member>>() { });
  }

  @Test
  void evaluateJsonPathWithClassType() {
    Member firstMember = new JsonPathExpectationsHelper("$.familyMembers[0]", JACKSON_MAPPING_CONFIGURATION)
            .evaluateJsonPath(SIMPSONS, Member.class);
    assertThat(firstMember).isEqualTo(new Member("Homer"));
  }

  @Test
  void evaluateJsonPathWithGenericType() {
    List<Member> family = new JsonPathExpectationsHelper("$.familyMembers", JACKSON_MAPPING_CONFIGURATION)
            .evaluateJsonPath(SIMPSONS, new ParameterizedTypeReference<List<Member>>() { });
    assertThat(family).containsExactly(new Member("Homer"), new Member("Marge"),
            new Member("Bart"), new Member("Lisa"), new Member("Maggie"));
  }

  public record Member(String name) { }

}
