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

package cn.taketoday.framework.json;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import cn.taketoday.framework.json.JsonParser;

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
		assertThat(map.get("foo")).isEqualTo("bar");
		assertThat(((Number) map.get("spam")).longValue()).isEqualTo(1L);
	}

	@Test
	void doubleValue() {
		Map<String, Object> map = this.parser.parseMap("{\"foo\":\"bar\",\"spam\":1.23}");
		assertThat(map).hasSize(2);
		assertThat(map.get("foo")).isEqualTo("bar");
		assertThat(map.get("spam")).isEqualTo(1.23d);
	}

	@Test
	void stringContainingNumber() {
		Map<String, Object> map = this.parser.parseMap("{\"foo\":\"123\"}");
		assertThat(map).hasSize(1);
		assertThat(map.get("foo")).isEqualTo("123");
	}

	@Test
	void stringContainingComma() {
		Map<String, Object> map = this.parser.parseMap("{\"foo\":\"bar1,bar2\"}");
		assertThat(map).hasSize(1);
		assertThat(map.get("foo")).isEqualTo("bar1,bar2");
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
		assertThat(map.get("foo")).asList().allMatch(Map.class::isInstance);
	}

	@SuppressWarnings("unchecked")
	@Test
	void nestedLeadingAndTrailingWhitespace() {
		Map<String, Object> map = this.parser.parseMap(
				" {\"foo\": [ { \"foo\" : \"bar\" , \"spam\" : 1 } , { \"foo\" : \"baz\" , \"spam\" : 2 } ] } ");
		assertThat(map).hasSize(1);
		assertThat(((List<Object>) map.get("foo"))).hasSize(2);
		assertThat(map.get("foo")).asList().allMatch(Map.class::isInstance);
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
		assertThat(map.get("foo")).isEqualTo("bar");
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
		assertThat(map.get("foo")).isEqualTo("\"bar\"");
	}

}
