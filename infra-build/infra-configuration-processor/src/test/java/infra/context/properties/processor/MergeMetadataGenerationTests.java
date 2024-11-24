/*
 * Copyright 2017 - 2023 the original author or authors.
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

package infra.context.properties.processor;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.context.properties.json.JSONArray;
import infra.context.properties.json.JSONObject;
import infra.context.properties.processor.metadata.ConfigurationMetadata;
import infra.context.properties.processor.metadata.ItemDeprecation;
import infra.context.properties.processor.metadata.ItemHint;
import infra.context.properties.processor.metadata.ItemMetadata;
import infra.context.properties.processor.metadata.Metadata;
import infra.context.properties.processor.metadata.TestJsonConverter;
import infra.context.properties.sample.simple.DeprecatedSingleProperty;
import infra.context.properties.sample.simple.SimpleProperties;
import infra.context.properties.sample.specific.SimpleConflictingProperties;
import infra.core.test.tools.CompilationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Metadata generation tests for merging additional metadata.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class MergeMetadataGenerationTests extends AbstractMetadataGenerationTests {

  @Test
  void mergingOfAdditionalProperty() throws Exception {
    ItemMetadata property = ItemMetadata.newProperty(null, "foo", "java.lang.String",
            AdditionalMetadata.class.getName(), null, null, null, null);
    String additionalMetadata = buildAdditionalMetadata(property);
    ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class);
    assertThat(metadata).has(Metadata.withProperty("simple.comparator"));
    assertThat(metadata).has(Metadata.withProperty("foo", String.class).fromSource(AdditionalMetadata.class));
  }

  @Test
  void mergingOfAdditionalPropertyMatchingGroup() throws Exception {
    ItemMetadata property = ItemMetadata.newProperty(null, "simple", "java.lang.String", null, null, null, null,
            null);
    String additionalMetadata = buildAdditionalMetadata(property);
    ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class);
    assertThat(metadata).has(Metadata.withGroup("simple").fromSource(SimpleProperties.class));
    assertThat(metadata).has(Metadata.withProperty("simple", String.class));
  }

  @Test
  void mergeExistingPropertyDefaultValue() throws Exception {
    ItemMetadata property = ItemMetadata.newProperty("simple", "flag", null, null, null, null, true, null);
    String additionalMetadata = buildAdditionalMetadata(property);
    ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class);
    assertThat(metadata).has(Metadata.withProperty("simple.flag", Boolean.class)
            .fromSource(SimpleProperties.class)
            .withDescription("A simple flag.")
            .withDeprecation()
            .withDefaultValue(true));
    assertThat(metadata.getItems()).hasSize(4);
  }

  @Test
  void mergeExistingPropertyWithSeveralCandidates() throws Exception {
    ItemMetadata property = ItemMetadata.newProperty("simple", "flag", Boolean.class.getName(), null, null, null,
            true, null);
    String additionalMetadata = buildAdditionalMetadata(property);
    ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class,
            SimpleConflictingProperties.class);
    assertThat(metadata.getItems()).hasSize(6);
    List<ItemMetadata> items = metadata.getItems()
            .stream()
            .filter((item) -> item.getName().equals("simple.flag"))
            .toList();
    assertThat(items).hasSize(2);
    ItemMetadata matchingProperty = items.stream()
            .filter((item) -> item.getType().equals(Boolean.class.getName()))
            .findFirst()
            .orElse(null);
    assertThat(matchingProperty).isNotNull();
    assertThat(matchingProperty.getDefaultValue()).isEqualTo(true);
    assertThat(matchingProperty.getSourceType()).isEqualTo(SimpleProperties.class.getName());
    assertThat(matchingProperty.getDescription()).isEqualTo("A simple flag.");
    ItemMetadata nonMatchingProperty = items.stream()
            .filter((item) -> item.getType().equals(String.class.getName()))
            .findFirst()
            .orElse(null);
    assertThat(nonMatchingProperty).isNotNull();
    assertThat(nonMatchingProperty.getDefaultValue()).isEqualTo("hello");
    assertThat(nonMatchingProperty.getSourceType()).isEqualTo(SimpleConflictingProperties.class.getName());
    assertThat(nonMatchingProperty.getDescription()).isNull();
  }

  @Test
  void mergeExistingPropertyDescription() throws Exception {
    ItemMetadata property = ItemMetadata.newProperty("simple", "comparator", null, null, null, "A nice comparator.",
            null, null);
    String additionalMetadata = buildAdditionalMetadata(property);
    ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class);
    assertThat(metadata).has(Metadata.withProperty("simple.comparator", "java.util.Comparator<?>")
            .fromSource(SimpleProperties.class)
            .withDescription("A nice comparator."));
    assertThat(metadata.getItems()).hasSize(4);
  }

  @Test
  void mergeExistingPropertyDeprecation() throws Exception {
    ItemMetadata property = ItemMetadata.newProperty("simple", "comparator", null, null, null, null, null,
            new ItemDeprecation("Don't use this.", "simple.complex-comparator", "1.2.3", "error"));
    String additionalMetadata = buildAdditionalMetadata(property);
    ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class);
    assertThat(metadata).has(Metadata.withProperty("simple.comparator", "java.util.Comparator<?>")
            .fromSource(SimpleProperties.class)
            .withDeprecation("Don't use this.", "simple.complex-comparator", "1.2.3", "error"));
    assertThat(metadata.getItems()).hasSize(4);
  }

  @Test
  void mergeExistingPropertyDeprecationOverride() throws Exception {
    ItemMetadata property = ItemMetadata.newProperty("singledeprecated", "name", null, null, null, null, null,
            new ItemDeprecation("Don't use this.", "single.name", "1.2.3"));
    String additionalMetadata = buildAdditionalMetadata(property);
    ConfigurationMetadata metadata = compile(additionalMetadata, DeprecatedSingleProperty.class);
    assertThat(metadata).has(Metadata.withProperty("singledeprecated.name", String.class.getName())
            .fromSource(DeprecatedSingleProperty.class)
            .withDeprecation("Don't use this.", "single.name", "1.2.3"));
    assertThat(metadata.getItems()).hasSize(3);
  }

  @Test
  void mergeExistingPropertyDeprecationOverrideLevel() throws Exception {
    ItemMetadata property = ItemMetadata.newProperty("singledeprecated", "name", null, null, null, null, null,
            new ItemDeprecation(null, null, null, "error"));
    String additionalMetadata = buildAdditionalMetadata(property);
    ConfigurationMetadata metadata = compile(additionalMetadata, DeprecatedSingleProperty.class);
    assertThat(metadata).has(Metadata.withProperty("singledeprecated.name", String.class.getName())
            .fromSource(DeprecatedSingleProperty.class)
            .withDeprecation("renamed", "singledeprecated.new-name", "1.2.3", "error"));
    assertThat(metadata.getItems()).hasSize(3);
  }

  @Test
  void mergeOfInvalidAdditionalMetadata() {
    String metadata = "Hello World";
    assertThatExceptionOfType(CompilationException.class)
            .isThrownBy(() -> compile(metadata, SimpleProperties.class))
            .withMessageContaining("Invalid additional meta-data");
  }

  @Test
  void mergingOfSimpleHint() throws Exception {
    String hints = buildAdditionalHints(ItemHint.newHint("simple.the-name",
            new ItemHint.ValueHint("boot", "Bla bla"), new ItemHint.ValueHint("spring", null)));
    ConfigurationMetadata metadata = compile(hints, SimpleProperties.class);
    assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
            .fromSource(SimpleProperties.class)
            .withDescription("The name of this simple properties.")
            .withDefaultValue("boot")
            .withDeprecation());
    assertThat(metadata)
            .has(Metadata.withHint("simple.the-name").withValue(0, "boot", "Bla bla").withValue(1, "spring", null));
  }

  @Test
  void mergingOfHintWithNonCanonicalName() throws Exception {
    String hints = buildAdditionalHints(
            ItemHint.newHint("simple.theName", new ItemHint.ValueHint("boot", "Bla bla")));
    ConfigurationMetadata metadata = compile(hints, SimpleProperties.class);
    assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
            .fromSource(SimpleProperties.class)
            .withDescription("The name of this simple properties.")
            .withDefaultValue("boot")
            .withDeprecation());
    assertThat(metadata).has(Metadata.withHint("simple.the-name").withValue(0, "boot", "Bla bla"));
  }

  @Test
  void mergingOfHintWithProvider() throws Exception {
    String hints = buildAdditionalHints(new ItemHint("simple.theName", Collections.emptyList(),
            Arrays.asList(new ItemHint.ValueProvider("first", Collections.singletonMap("target", "org.foo")),
                    new ItemHint.ValueProvider("second", null))));
    ConfigurationMetadata metadata = compile(hints, SimpleProperties.class);
    assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
            .fromSource(SimpleProperties.class)
            .withDescription("The name of this simple properties.")
            .withDefaultValue("boot")
            .withDeprecation());
    assertThat(metadata).has(
            Metadata.withHint("simple.the-name").withProvider("first", "target", "org.foo").withProvider("second"));
  }

  @Test
  void mergingOfAdditionalDeprecation() throws Exception {
    String deprecations = buildPropertyDeprecations(
            ItemMetadata.newProperty("simple", "wrongName", "java.lang.String", null, null, null, null,
                    new ItemDeprecation("Lame name.", "simple.the-name", "1.2.3")));
    ConfigurationMetadata metadata = compile(deprecations, SimpleProperties.class);
    assertThat(metadata).has(Metadata.withProperty("simple.wrong-name", String.class)
            .withDeprecation("Lame name.", "simple.the-name", "1.2.3"));
  }

  @Test
  void mergingOfAdditionalMetadata() throws Exception {
    JSONObject property = new JSONObject();
    property.put("name", "foo");
    property.put("type", "java.lang.String");
    property.put("sourceType", AdditionalMetadata.class.getName());
    JSONArray properties = new JSONArray();
    properties.put(property);
    JSONObject json = new JSONObject();
    json.put("properties", properties);
    String additionalMetadata = json.toString();
    ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class);
    assertThat(metadata).has(Metadata.withProperty("simple.comparator"));
    assertThat(metadata).has(Metadata.withProperty("foo", String.class).fromSource(AdditionalMetadata.class));
  }

  private String buildAdditionalMetadata(ItemMetadata... metadata) throws Exception {
    TestJsonConverter converter = new TestJsonConverter();
    JSONObject additionalMetadata = new JSONObject();
    JSONArray properties = new JSONArray();
    for (ItemMetadata itemMetadata : metadata) {
      properties.put(converter.toJsonObject(itemMetadata));
    }
    additionalMetadata.put("properties", properties);
    return additionalMetadata.toString();
  }

  private String buildAdditionalHints(ItemHint... hints) throws Exception {
    TestJsonConverter converter = new TestJsonConverter();
    JSONObject additionalMetadata = new JSONObject();
    additionalMetadata.put("hints", converter.toJsonArray(Arrays.asList(hints)));
    return additionalMetadata.toString();
  }

  private String buildPropertyDeprecations(ItemMetadata... items) throws Exception {
    JSONArray propertiesArray = new JSONArray();
    for (ItemMetadata item : items) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("name", item.getName());
      if (item.getType() != null) {
        jsonObject.put("type", item.getType());
      }
      ItemDeprecation deprecation = item.getDeprecation();
      if (deprecation != null) {
        JSONObject deprecationJson = new JSONObject();
        if (deprecation.getReason() != null) {
          deprecationJson.put("reason", deprecation.getReason());
        }
        if (deprecation.getReplacement() != null) {
          deprecationJson.put("replacement", deprecation.getReplacement());
        }
        if (deprecation.getSince() != null) {
          deprecationJson.put("since", deprecation.getSince());
        }
        jsonObject.put("deprecation", deprecationJson);
      }
      propertiesArray.put(jsonObject);

    }
    JSONObject additionalMetadata = new JSONObject();
    additionalMetadata.put("properties", propertiesArray);
    return additionalMetadata.toString();
  }

  static class AdditionalMetadata {

  }

}
