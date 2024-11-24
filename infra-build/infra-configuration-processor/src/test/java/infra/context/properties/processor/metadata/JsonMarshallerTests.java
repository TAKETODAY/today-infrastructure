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

package infra.context.properties.processor.metadata;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Tests for {@link JsonMarshaller}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class JsonMarshallerTests {

  @Test
  void marshallAndUnmarshal() throws Exception {
    ConfigurationMetadata metadata = new ConfigurationMetadata();
    metadata.add(ItemMetadata.newProperty("a", "b", StringBuffer.class.getName(), InputStream.class.getName(), null,
            "desc", "x", new ItemDeprecation("Deprecation comment", "b.c.d", "1.2.3")));
    metadata.add(ItemMetadata.newProperty("b.c.d", null, null, null, null, null, null, null));
    metadata.add(ItemMetadata.newProperty("c", null, null, null, null, null, 123, null));
    metadata.add(ItemMetadata.newProperty("d", null, null, null, null, null, true, null));
    metadata.add(ItemMetadata.newProperty("e", null, null, null, null, null, new String[] { "y", "n" }, null));
    metadata.add(ItemMetadata.newProperty("f", null, null, null, null, null, new Boolean[] { true, false }, null));
    metadata.add(ItemMetadata.newGroup("d", null, null, null));
    metadata.add(ItemMetadata.newGroup("e", null, null, "sourceMethod"));
    metadata.add(ItemHint.newHint("a.b"));
    metadata.add(ItemHint.newHint("c", new ItemHint.ValueHint(123, "hey"), new ItemHint.ValueHint(456, null)));
    metadata.add(new ItemHint("d", null,
            Arrays.asList(new ItemHint.ValueProvider("first", Collections.singletonMap("target", "foo")),
                    new ItemHint.ValueProvider("second", null))));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    JsonMarshaller marshaller = new JsonMarshaller();
    marshaller.write(metadata, outputStream);
    ConfigurationMetadata read = marshaller.read(new ByteArrayInputStream(outputStream.toByteArray()));
    assertThat(read).has(Metadata.withProperty("a.b", StringBuffer.class)
            .fromSource(InputStream.class)
            .withDescription("desc")
            .withDefaultValue("x")
            .withDeprecation("Deprecation comment", "b.c.d", "1.2.3"));
    assertThat(read).has(Metadata.withProperty("b.c.d"));
    assertThat(read).has(Metadata.withProperty("c").withDefaultValue(123));
    assertThat(read).has(Metadata.withProperty("d").withDefaultValue(true));
    assertThat(read).has(Metadata.withProperty("e").withDefaultValue(new String[] { "y", "n" }));
    assertThat(read).has(Metadata.withProperty("f").withDefaultValue(new Object[] { true, false }));
    assertThat(read).has(Metadata.withGroup("d"));
    assertThat(read).has(Metadata.withGroup("e").fromSourceMethod("sourceMethod"));
    assertThat(read).has(Metadata.withHint("a.b"));
    assertThat(read).has(Metadata.withHint("c").withValue(0, 123, "hey").withValue(1, 456, null));
    assertThat(read).has(Metadata.withHint("d").withProvider("first", "target", "foo").withProvider("second"));
  }

  @Test
  void marshallOrderItems() throws IOException {
    ConfigurationMetadata metadata = new ConfigurationMetadata();
    metadata.add(ItemHint.newHint("fff"));
    metadata.add(ItemHint.newHint("eee"));
    metadata.add(ItemMetadata.newProperty("com.example.bravo", "bbb", null, null, null, null, null, null));
    metadata.add(ItemMetadata.newProperty("com.example.bravo", "aaa", null, null, null, null, null, null));
    metadata.add(ItemMetadata.newProperty("com.example.alpha", "ddd", null, null, null, null, null, null));
    metadata.add(ItemMetadata.newProperty("com.example.alpha", "ccc", null, null, null, null, null, null));
    metadata.add(ItemMetadata.newGroup("com.acme.bravo", "com.example.AnotherTestProperties", null, null));
    metadata.add(ItemMetadata.newGroup("com.acme.alpha", "com.example.TestProperties", null, null));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    JsonMarshaller marshaller = new JsonMarshaller();
    marshaller.write(metadata, outputStream);
    String json = outputStream.toString();
    assertThat(json).containsSubsequence("\"groups\"", "\"com.acme.alpha\"", "\"com.acme.bravo\"", "\"properties\"",
            "\"com.example.alpha.ccc\"", "\"com.example.alpha.ddd\"", "\"com.example.bravo.aaa\"",
            "\"com.example.bravo.bbb\"", "\"hints\"", "\"eee\"", "\"fff\"");
  }

  @Test
  void marshallPutDeprecatedItemsAtTheEnd() throws IOException {
    ConfigurationMetadata metadata = new ConfigurationMetadata();
    metadata.add(ItemMetadata.newProperty("com.example.bravo", "bbb", null, null, null, null, null, null));
    metadata.add(ItemMetadata.newProperty("com.example.bravo", "aaa", null, null, null, null, null,
            new ItemDeprecation(null, null, null, "warning")));
    metadata.add(ItemMetadata.newProperty("com.example.alpha", "ddd", null, null, null, null, null, null));
    metadata.add(ItemMetadata.newProperty("com.example.alpha", "ccc", null, null, null, null, null,
            new ItemDeprecation(null, null, null, "warning")));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    JsonMarshaller marshaller = new JsonMarshaller();
    marshaller.write(metadata, outputStream);
    String json = outputStream.toString();
    assertThat(json).containsSubsequence("\"properties\"", "\"com.example.alpha.ddd\"", "\"com.example.bravo.bbb\"",
            "\"com.example.alpha.ccc\"", "\"com.example.bravo.aaa\"");
  }

  @Test
  void orderingForSameGroupNames() throws IOException {
    ConfigurationMetadata metadata = new ConfigurationMetadata();
    metadata.add(ItemMetadata.newGroup("com.acme.alpha", null, "com.example.Foo", null));
    metadata.add(ItemMetadata.newGroup("com.acme.alpha", null, "com.example.Bar", null));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    JsonMarshaller marshaller = new JsonMarshaller();
    marshaller.write(metadata, outputStream);
    String json = outputStream.toString();
    assertThat(json).containsSubsequence("\"groups\"", "\"name\": \"com.acme.alpha\"",
            "\"sourceType\": \"com.example.Bar\"", "\"name\": \"com.acme.alpha\"",
            "\"sourceType\": \"com.example.Foo\"");
  }

  @Test
  void orderingForSamePropertyNames() throws IOException {
    ConfigurationMetadata metadata = new ConfigurationMetadata();
    metadata.add(ItemMetadata.newProperty("com.example.bravo", "aaa", "java.lang.Boolean", "com.example.Foo", null,
            null, null, null));
    metadata.add(ItemMetadata.newProperty("com.example.bravo", "aaa", "java.lang.Integer", "com.example.Bar", null,
            null, null, null));
    metadata
            .add(ItemMetadata.newProperty("com.example.alpha", "ddd", null, "com.example.Bar", null, null, null, null));
    metadata
            .add(ItemMetadata.newProperty("com.example.alpha", "ccc", null, "com.example.Foo", null, null, null, null));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    JsonMarshaller marshaller = new JsonMarshaller();
    marshaller.write(metadata, outputStream);
    String json = outputStream.toString();
    assertThat(json).containsSubsequence("\"groups\"", "\"properties\"", "\"com.example.alpha.ccc\"",
            "com.example.Foo", "\"com.example.alpha.ddd\"", "com.example.Bar", "\"com.example.bravo.aaa\"",
            "com.example.Bar", "\"com.example.bravo.aaa\"", "com.example.Foo");
  }

  @Test
  void orderingForSameGroupWithNullSourceType() throws IOException {
    ConfigurationMetadata metadata = new ConfigurationMetadata();
    metadata.add(ItemMetadata.newGroup("com.acme.alpha", null, "com.example.Foo", null));
    metadata.add(ItemMetadata.newGroup("com.acme.alpha", null, null, null));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    JsonMarshaller marshaller = new JsonMarshaller();
    marshaller.write(metadata, outputStream);
    String json = outputStream.toString();
    assertThat(json).containsSubsequence("\"groups\"", "\"name\": \"com.acme.alpha\"",
            "\"name\": \"com.acme.alpha\"", "\"sourceType\": \"com.example.Foo\"");
  }

  @Test
  void orderingForSamePropertyNamesWithNullSourceType() throws IOException {
    ConfigurationMetadata metadata = new ConfigurationMetadata();
    metadata.add(ItemMetadata.newProperty("com.example.bravo", "aaa", "java.lang.Boolean", null, null, null, null,
            null));
    metadata.add(ItemMetadata.newProperty("com.example.bravo", "aaa", "java.lang.Integer", "com.example.Bar", null,
            null, null, null));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    JsonMarshaller marshaller = new JsonMarshaller();
    marshaller.write(metadata, outputStream);
    String json = outputStream.toString();
    assertThat(json).containsSubsequence("\"groups\"", "\"properties\"", "\"com.example.bravo.aaa\"",
            "\"java.lang.Boolean\"", "\"com.example.bravo.aaa\"", "\"java.lang.Integer\"", "\"com.example.Bar");
  }

  @Test
  void shouldCheckRootFields() {
    String json = """
            {
            	"groups": [], "properties": [], "hints": [], "dummy": []
            }""";
    assertThatException().isThrownBy(() -> read(json))
            .withMessage("Expected only keys [groups, hints, properties], but found additional keys [dummy]. Path: .");
  }

  @Test
  void shouldCheckGroupFields() {
    String json = """
            {
            	"groups": [
            		{
            			"name": "g",
            			"type": "java.lang.String",
            			"description": "Some description",
            			"sourceType": "java.lang.String",
            			"sourceMethod": "some()",
            			"dummy": "dummy"
            		}
            	], "properties": [], "hints": []
            }""";
    assertThatException().isThrownBy(() -> read(json))
            .withMessage(
                    "Expected only keys [description, name, sourceMethod, sourceType, type], but found additional keys [dummy]. Path: .groups.[0]");
  }

  @Test
  void shouldCheckPropertyFields() {
    String json = """
            {
            	"groups": [], "properties": [
            		{
            			"name": "name",
            			"type": "java.lang.String",
            			"description": "Some description",
            			"sourceType": "java.lang.String",
            			"defaultValue": "value",
            			"deprecation": {
            				"level": "warning",
            				"reason": "some reason",
            				"replacement": "name-new",
            				"since": "v17"
            			},
            			"deprecated": true,
            			"dummy": "dummy"
            		}
            	], "hints": []
            }""";
    assertThatException().isThrownBy(() -> read(json))
            .withMessage(
                    "Expected only keys [defaultValue, deprecated, deprecation, description, name, sourceType, type], but found additional keys [dummy]. Path: .properties.[0]");
  }

  @Test
  void shouldCheckPropertyDeprecationFields() {
    String json = """
            {
            	"groups": [], "properties": [
            		{
            			"name": "name",
            			"type": "java.lang.String",
            			"description": "Some description",
            			"sourceType": "java.lang.String",
            			"defaultValue": "value",
            			"deprecation": {
            				"level": "warning",
            				"reason": "some reason",
            				"replacement": "name-new",
            				"since": "v17",
            				"dummy": "dummy"
            			},
            			"deprecated": true
            		}
            	], "hints": []
            }""";
    assertThatException().isThrownBy(() -> read(json))
            .withMessage(
                    "Expected only keys [level, reason, replacement, since], but found additional keys [dummy]. Path: .properties.[0].deprecation");
  }

  @Test
  void shouldCheckHintFields() {
    String json = """
            {
            	"groups": [], "properties": [], "hints": [
            		{
            			"name": "name",
            			"values": [],
            			"providers": [],
            			"dummy": "dummy"
            		}
            	]
            }""";
    assertThatException().isThrownBy(() -> read(json))
            .withMessage(
                    "Expected only keys [name, providers, values], but found additional keys [dummy]. Path: .hints.[0]");
  }

  @Test
  void shouldCheckHintValueFields() {
    String json = """
            {
            	"groups": [], "properties": [], "hints": [
            		{
            			"name": "name",
            			"values": [
            				{
            					"value": "value",
            					"description": "some description",
            					"dummy": "dummy"
            				}
            			],
            			"providers": []
            		}
            	]
            }""";
    assertThatException().isThrownBy(() -> read(json))
            .withMessage(
                    "Expected only keys [description, value], but found additional keys [dummy]. Path: .hints.[0].values.[0]");
  }

  @Test
  void shouldCheckHintProviderFields() {
    String json = """
            {
            	"groups": [], "properties": [], "hints": [
            		{
            			"name": "name",
            			"values": [],
            			"providers": [
            				{
            					"name": "name",
            					"parameters": {
            						"target": "HttpServlet"
            					},
            					"dummy": "dummy"
            				}
            			]
            		}
            	]
            }""";
    assertThatException().isThrownBy(() -> read(json))
            .withMessage(
                    "Expected only keys [name, parameters], but found additional keys [dummy]. Path: .hints.[0].providers.[0]");
  }

  private void read(String json) throws Exception {
    JsonMarshaller marshaller = new JsonMarshaller();
    marshaller.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
  }

}
