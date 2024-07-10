/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.nativex;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BasicJsonWriter}.
 *
 * @author Stephane Nicoll
 */
class BasicJsonWriterTests {

	private final StringWriter out = new StringWriter();

	private final BasicJsonWriter json = new BasicJsonWriter(out, "\t");

	@Test
	void writeObject() {
		Map<String, Object> attributes = orderedMap("test", "value");
		attributes.put("another", true);
		this.json.writeObject(attributes);
		assertThat(out.toString()).isEqualToNormalizingNewlines("""
				{
					"test": "value",
					"another": true
				}
				""");
	}

	@Test
	void writeObjectWithNestedObject() {
		Map<String, Object> attributes = orderedMap("test", "value");
		attributes.put("nested", orderedMap("enabled", false));
		this.json.writeObject(attributes);
		assertThat(out.toString()).isEqualToNormalizingNewlines("""
				{
					"test": "value",
					"nested": {
						"enabled": false
					}
				}
				""");
	}

	@Test
	void writeObjectWithNestedArrayOfString() {
		Map<String, Object> attributes = orderedMap("test", "value");
		attributes.put("nested", List.of("test", "value", "another"));
		this.json.writeObject(attributes);
		assertThat(out.toString()).isEqualToNormalizingNewlines("""
				{
					"test": "value",
					"nested": [
						"test",
						"value",
						"another"
					]
				}
				""");
	}

	@Test
	void writeObjectWithNestedArrayOfObject() {
		Map<String, Object> attributes = orderedMap("test", "value");
		LinkedHashMap<String, Object> secondNested = orderedMap("name", "second");
		secondNested.put("enabled", false);
		attributes.put("nested", List.of(orderedMap("name", "first"), secondNested, orderedMap("name", "third")));
		this.json.writeObject(attributes);
		assertThat(out.toString()).isEqualToNormalizingNewlines("""
				{
					"test": "value",
					"nested": [
						{
							"name": "first"
						},
						{
							"name": "second",
							"enabled": false
						},
						{
							"name": "third"
						}
					]
				}
				""");
	}

	@Test
	void writeObjectWithNestedEmptyArray() {
		Map<String, Object> attributes = orderedMap("test", "value");
		attributes.put("nested", Collections.emptyList());
		this.json.writeObject(attributes);
		assertThat(out.toString()).isEqualToNormalizingNewlines("""
				{
					"test": "value",
					"nested": [ ]
				}
				""");
	}

	@Test
	void writeObjectWithNestedEmptyObject() {
		Map<String, Object> attributes = orderedMap("test", "value");
		attributes.put("nested", Collections.emptyMap());
		this.json.writeObject(attributes);
		assertThat(out.toString()).isEqualToNormalizingNewlines("""
				{
					"test": "value",
					"nested": { }
				}
				""");
	}

	@Test
	void writeWithEscapeDoubleQuote() {
		assertStringAttribute("foo\"bar", "foo\\\"bar");
	}

	@Test
	void writeWithEscapeBackslash() {
		assertStringAttribute("foo\"bar", "foo\\\"bar");
	}

	@Test
	void writeWithEscapeBackspace() {
		assertStringAttribute("foo\bbar", "foo\\bbar");
	}

	@Test
	void writeWithEscapeFormFeed() {
		assertStringAttribute("foo\fbar", "foo\\fbar");
	}

	@Test
	void writeWithEscapeNewline() {
		assertStringAttribute("foo\nbar", "foo\\nbar");
	}

	@Test
	void writeWithEscapeCarriageReturn() {
		assertStringAttribute("foo\rbar", "foo\\rbar");
	}

	@Test
	void writeWithEscapeTab() {
		assertStringAttribute("foo\tbar", "foo\\tbar");
	}

	@Test
	void writeWithEscapeUnicode() {
		assertStringAttribute("foo\u001Fbar", "foo\\u001fbar");
	}

	@Test
	void writeWithTypeReferenceForSimpleClass() {
		assertStringAttribute(TypeReference.of(String.class), "java.lang.String");
	}

	@Test
	void writeWithTypeReferenceForInnerClass() {
		assertStringAttribute(TypeReference.of(Nested.class),
				"cn.taketoday.aot.nativex.BasicJsonWriterTests$Nested");
	}

	@Test
	void writeWithTypeReferenceForDoubleInnerClass() {
		assertStringAttribute(TypeReference.of(Nested.Inner.class),
				"cn.taketoday.aot.nativex.BasicJsonWriterTests$Nested$Inner");
	}

	void assertStringAttribute(Object value, String expectedValue) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("test", value);
		this.json.writeObject(attributes);
		assertThat(out.toString()).contains("\"test\": \"" + expectedValue + "\"");
	}

	private static LinkedHashMap<String, Object> orderedMap(String key, Object value) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put(key, value);
		return map;
	}


	static class Nested {

		static class Inner {

		}
	}

}
