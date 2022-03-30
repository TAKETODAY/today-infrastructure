/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.util;

import org.junit.jupiter.api.Test;

import cn.taketoday.test.util.JsonPathExpectationsHelper;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link JsonPathExpectationsHelper}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
class JsonPathExpectationsHelperTests {

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


	@Test
	void exists() throws Exception {
		new JsonPathExpectationsHelper("$.str").exists(CONTENT);
	}

	@Test
	void existsForAnEmptyArray() throws Exception {
		new JsonPathExpectationsHelper("$.emptyArray").exists(CONTENT);
	}

	@Test
	void existsForAnEmptyMap() throws Exception {
		new JsonPathExpectationsHelper("$.emptyMap").exists(CONTENT);
	}

	@Test
	void existsForIndefinitePathWithResults() throws Exception {
		new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Bart')]").exists(SIMPSONS);
	}

	@Test
	void existsForIndefinitePathWithEmptyResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).exists(SIMPSONS))
			.withMessageContaining("No value at JSON path \"" + expression + "\"");
	}

	@Test
	void doesNotExist() throws Exception {
		new JsonPathExpectationsHelper("$.bogus").doesNotExist(CONTENT);
	}

	@Test
	void doesNotExistForAnEmptyArray() throws Exception {
		String expression = "$.emptyArray";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).doesNotExist(CONTENT))
			.withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: []");
	}

	@Test
	void doesNotExistForAnEmptyMap() throws Exception {
		String expression = "$.emptyMap";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).doesNotExist(CONTENT))
			.withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: {}");
	}

	@Test
	void doesNotExistForIndefinitePathWithResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).doesNotExist(SIMPSONS))
			.withMessageContaining("Expected no value at JSON path \"" + expression + "\" but found: [{\"name\":\"Bart\"}]");
	}

	@Test
	void doesNotExistForIndefinitePathWithEmptyResults() throws Exception {
		new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Dilbert')]").doesNotExist(SIMPSONS);
	}

	@Test
	void assertValueIsEmptyForAnEmptyString() throws Exception {
		new JsonPathExpectationsHelper("$.emptyString").assertValueIsEmpty(CONTENT);
	}

	@Test
	void assertValueIsEmptyForAnEmptyArray() throws Exception {
		new JsonPathExpectationsHelper("$.emptyArray").assertValueIsEmpty(CONTENT);
	}

	@Test
	void assertValueIsEmptyForAnEmptyMap() throws Exception {
		new JsonPathExpectationsHelper("$.emptyMap").assertValueIsEmpty(CONTENT);
	}

	@Test
	void assertValueIsEmptyForIndefinitePathWithEmptyResults() throws Exception {
		new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Dilbert')]").assertValueIsEmpty(SIMPSONS);
	}

	@Test
	void assertValueIsEmptyForIndefinitePathWithResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Bart')]";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsEmpty(SIMPSONS))
			.withMessageContaining("Expected an empty value at JSON path \"" + expression + "\" but found: [{\"name\":\"Bart\"}]");
	}

	@Test
	void assertValueIsEmptyForWhitespace() throws Exception {
		String expression = "$.whitespace";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsEmpty(CONTENT))
			.withMessageContaining("Expected an empty value at JSON path \"" + expression + "\" but found: '    '");
	}

	@Test
	void assertValueIsNotEmptyForString() throws Exception {
		new JsonPathExpectationsHelper("$.str").assertValueIsNotEmpty(CONTENT);
	}

	@Test
	void assertValueIsNotEmptyForNumber() throws Exception {
		new JsonPathExpectationsHelper("$.num").assertValueIsNotEmpty(CONTENT);
	}

	@Test
	void assertValueIsNotEmptyForBoolean() throws Exception {
		new JsonPathExpectationsHelper("$.bool").assertValueIsNotEmpty(CONTENT);
	}

	@Test
	void assertValueIsNotEmptyForArray() throws Exception {
		new JsonPathExpectationsHelper("$.arr").assertValueIsNotEmpty(CONTENT);
	}

	@Test
	void assertValueIsNotEmptyForMap() throws Exception {
		new JsonPathExpectationsHelper("$.colorMap").assertValueIsNotEmpty(CONTENT);
	}

	@Test
	void assertValueIsNotEmptyForIndefinitePathWithResults() throws Exception {
		new JsonPathExpectationsHelper("$.familyMembers[?(@.name == 'Bart')]").assertValueIsNotEmpty(SIMPSONS);
	}

	@Test
	void assertValueIsNotEmptyForIndefinitePathWithEmptyResults() throws Exception {
		String expression = "$.familyMembers[?(@.name == 'Dilbert')]";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(SIMPSONS))
			.withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: []");
	}

	@Test
	void assertValueIsNotEmptyForAnEmptyString() throws Exception {
		String expression = "$.emptyString";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(CONTENT))
			.withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: ''");
	}

	@Test
	void assertValueIsNotEmptyForAnEmptyArray() throws Exception {
		String expression = "$.emptyArray";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsNotEmpty(CONTENT))
			.withMessageContaining("Expected a non-empty value at JSON path \"" + expression + "\" but found: []");
	}

	@Test
	void assertValueIsNotEmptyForAnEmptyMap() throws Exception {
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

	@Test // SPR-16339
	void doesNotHaveJsonPath() {
		new JsonPathExpectationsHelper("$.abc").doesNotHaveJsonPath("{}");
	}

	@Test // SPR-16339
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
	void assertValue() throws Exception {
		new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, 5);
	}

	@Test // SPR-14498
	void assertValueWithNumberConversion() throws Exception {
		new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, 5.0);
	}

	@Test // SPR-14498
	void assertValueWithNumberConversionAndMatcher() throws Exception {
		new JsonPathExpectationsHelper("$.num").assertValue(CONTENT, is(5.0), Double.class);
	}

	@Test
	void assertValueIsString() throws Exception {
		new JsonPathExpectationsHelper("$.str").assertValueIsString(CONTENT);
	}

	@Test
	void assertValueIsStringForAnEmptyString() throws Exception {
		new JsonPathExpectationsHelper("$.emptyString").assertValueIsString(CONTENT);
	}

	@Test
	void assertValueIsStringForNonString() throws Exception {
		String expression = "$.bool";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsString(CONTENT))
			.withMessageContaining("Expected a string at JSON path \"" + expression + "\" but found: true");
	}

	@Test
	void assertValueIsNumber() throws Exception {
		new JsonPathExpectationsHelper("$.num").assertValueIsNumber(CONTENT);
	}

	@Test
	void assertValueIsNumberForNonNumber() throws Exception {
		String expression = "$.bool";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsNumber(CONTENT))
			.withMessageContaining("Expected a number at JSON path \"" + expression + "\" but found: true");
	}

	@Test
	void assertValueIsBoolean() throws Exception {
		new JsonPathExpectationsHelper("$.bool").assertValueIsBoolean(CONTENT);
	}

	@Test
	void assertValueIsBooleanForNonBoolean() throws Exception {
		String expression = "$.num";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsBoolean(CONTENT))
			.withMessageContaining("Expected a boolean at JSON path \"" + expression + "\" but found: 5");
	}

	@Test
	void assertValueIsArray() throws Exception {
		new JsonPathExpectationsHelper("$.arr").assertValueIsArray(CONTENT);
	}

	@Test
	void assertValueIsArrayForAnEmptyArray() throws Exception {
		new JsonPathExpectationsHelper("$.emptyArray").assertValueIsArray(CONTENT);
	}

	@Test
	void assertValueIsArrayForNonArray() throws Exception {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsArray(CONTENT))
			.withMessageContaining("Expected an array at JSON path \"" + expression + "\" but found: 'foo'");
	}

	@Test
	void assertValueIsMap() throws Exception {
		new JsonPathExpectationsHelper("$.colorMap").assertValueIsMap(CONTENT);
	}

	@Test
	void assertValueIsMapForAnEmptyMap() throws Exception {
		new JsonPathExpectationsHelper("$.emptyMap").assertValueIsMap(CONTENT);
	}

	@Test
	void assertValueIsMapForNonMap() throws Exception {
		String expression = "$.str";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new JsonPathExpectationsHelper(expression).assertValueIsMap(CONTENT))
			.withMessageContaining("Expected a map at JSON path \"" + expression + "\" but found: 'foo'");
	}

}
