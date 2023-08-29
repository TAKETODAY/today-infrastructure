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

package cn.taketoday.context.properties.metadata;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.context.properties.json.JSONException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JsonReader}.
 *
 * @author Stephane Nicoll
 */
class JsonReaderTests extends AbstractConfigurationMetadataTests {

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private final JsonReader reader = new JsonReader();

  @Test
  void emptyMetadata() throws IOException {
    RawConfigurationMetadata rawMetadata = readFor("empty");
    assertThat(rawMetadata.getSources()).isEmpty();
    assertThat(rawMetadata.getItems()).isEmpty();
  }

  @Test
  void invalidMetadata() {
    assertThatIllegalStateException().isThrownBy(() -> readFor("invalid")).withCauseInstanceOf(JSONException.class);
  }

  @Test
  void emptyGroupName() throws IOException {
    RawConfigurationMetadata rawMetadata = readFor("empty-groups");
    List<ConfigurationMetadataItem> items = rawMetadata.getItems();
    assertThat(items).hasSize(2);

    ConfigurationMetadataItem name = items.get(0);
    assertProperty(name, "name", "name", String.class, null);
    ConfigurationMetadataItem dotTitle = items.get(1);
    assertProperty(dotTitle, "title", "title", String.class, null);
  }

  @Test
  void simpleMetadata() throws IOException {
    RawConfigurationMetadata rawMetadata = readFor("foo");
    List<ConfigurationMetadataSource> sources = rawMetadata.getSources();
    assertThat(sources).hasSize(2);
    List<ConfigurationMetadataItem> items = rawMetadata.getItems();
    assertThat(items).hasSize(4);
    List<ConfigurationMetadataHint> hints = rawMetadata.getHints();
    assertThat(hints).hasSize(1);

    ConfigurationMetadataSource source = sources.get(0);
    assertSource(source, "spring.foo", "org.acme.Foo", "org.acme.config.FooApp");
    assertThat(source.getSourceMethod()).isEqualTo("foo()");
    assertThat(source.getDescription()).isEqualTo("This is Foo.");
    assertThat(source.getShortDescription()).isEqualTo("This is Foo.");

    ConfigurationMetadataItem item = items.get(0);
    assertProperty(item, "spring.foo.name", "name", String.class, null);
    assertItem(item, "org.acme.Foo");
    ConfigurationMetadataItem item2 = items.get(1);
    assertProperty(item2, "spring.foo.description", "description", String.class, "FooBar");
    assertThat(item2.getDescription()).isEqualTo("Foo description.");
    assertThat(item2.getShortDescription()).isEqualTo("Foo description.");
    assertThat(item2.getSourceMethod()).isNull();
    assertItem(item2, "org.acme.Foo");

    ConfigurationMetadataHint hint = hints.get(0);
    assertThat(hint.getId()).isEqualTo("spring.foo.counter");
    assertThat(hint.getValueHints()).hasSize(1);
    ValueHint valueHint = hint.getValueHints().get(0);
    assertThat(valueHint.getValue()).isEqualTo(42);
    assertThat(valueHint.getDescription())
            .isEqualTo("Because that's the answer to any question, choose it. \nReally.");
    assertThat(valueHint.getShortDescription()).isEqualTo("Because that's the answer to any question, choose it.");
    assertThat(hint.getValueProviders()).hasSize(1);
    ValueProvider valueProvider = hint.getValueProviders().get(0);
    assertThat(valueProvider.getName()).isEqualTo("handle-as");
    assertThat(valueProvider.getParameters()).hasSize(1);
    assertThat(valueProvider.getParameters()).containsEntry("target", Integer.class.getName());
  }

  @Test
  void metadataHints() throws IOException {
    RawConfigurationMetadata rawMetadata = readFor("bar");
    List<ConfigurationMetadataHint> hints = rawMetadata.getHints();
    assertThat(hints).hasSize(1);

    ConfigurationMetadataHint hint = hints.get(0);
    assertThat(hint.getId()).isEqualTo("spring.bar.description");
    assertThat(hint.getValueHints()).hasSize(2);
    ValueHint valueHint = hint.getValueHints().get(0);
    assertThat(valueHint.getValue()).isEqualTo("one");
    assertThat(valueHint.getDescription()).isEqualTo("One.");
    ValueHint valueHint2 = hint.getValueHints().get(1);
    assertThat(valueHint2.getValue()).isEqualTo("two");
    assertThat(valueHint2.getDescription()).isNull();

    assertThat(hint.getValueProviders()).hasSize(2);
    ValueProvider valueProvider = hint.getValueProviders().get(0);
    assertThat(valueProvider.getName()).isEqualTo("handle-as");
    assertThat(valueProvider.getParameters()).hasSize(1);
    assertThat(valueProvider.getParameters()).containsEntry("target", String.class.getName());
    ValueProvider valueProvider2 = hint.getValueProviders().get(1);
    assertThat(valueProvider2.getName()).isEqualTo("any");
    assertThat(valueProvider2.getParameters()).isEmpty();
  }

  @Test
  void rootMetadata() throws IOException {
    RawConfigurationMetadata rawMetadata = readFor("root");
    List<ConfigurationMetadataSource> sources = rawMetadata.getSources();
    assertThat(sources).isEmpty();
    List<ConfigurationMetadataItem> items = rawMetadata.getItems();
    assertThat(items).hasSize(2);
    ConfigurationMetadataItem item = items.get(0);
    assertProperty(item, "spring.root.name", "spring.root.name", String.class, null);
  }

  @Test
  void deprecatedMetadata() throws IOException {
    RawConfigurationMetadata rawMetadata = readFor("deprecated");
    List<ConfigurationMetadataItem> items = rawMetadata.getItems();
    assertThat(items).hasSize(5);

    ConfigurationMetadataItem item = items.get(0);
    assertProperty(item, "server.port", "server.port", Integer.class, null);
    assertThat(item.isDeprecated()).isTrue();
    assertThat(item.getDeprecation().getReason()).isEqualTo("Server namespace has moved to spring.server");
    assertThat(item.getDeprecation().getShortReason()).isEqualTo("Server namespace has moved to spring.server");
    assertThat(item.getDeprecation().getReplacement()).isEqualTo("server.spring.port");
    assertThat(item.getDeprecation().getLevel()).isEqualTo(Deprecation.Level.WARNING);

    ConfigurationMetadataItem item2 = items.get(1);
    assertProperty(item2, "server.cluster-name", "server.cluster-name", String.class, null);
    assertThat(item2.isDeprecated()).isTrue();
    assertThat(item2.getDeprecation().getReason()).isNull();
    assertThat(item2.getDeprecation().getShortReason()).isNull();
    assertThat(item2.getDeprecation().getReplacement()).isNull();
    assertThat(item.getDeprecation().getLevel()).isEqualTo(Deprecation.Level.WARNING);

    ConfigurationMetadataItem item3 = items.get(2);
    assertProperty(item3, "spring.server.name", "spring.server.name", String.class, null);
    assertThat(item3.isDeprecated()).isFalse();
    assertThat(item3.getDeprecation()).isNull();

    ConfigurationMetadataItem item4 = items.get(3);
    assertProperty(item4, "spring.server-name", "spring.server-name", String.class, null);
    assertThat(item4.isDeprecated()).isTrue();
    assertThat(item4.getDeprecation().getReason()).isNull();
    assertThat(item2.getDeprecation().getShortReason()).isNull();
    assertThat(item4.getDeprecation().getReplacement()).isEqualTo("spring.server.name");
    assertThat(item4.getDeprecation().getLevel()).isEqualTo(Deprecation.Level.ERROR);

    ConfigurationMetadataItem item5 = items.get(4);
    assertProperty(item5, "spring.server-name2", "spring.server-name2", String.class, null);
    assertThat(item5.isDeprecated()).isTrue();
    assertThat(item5.getDeprecation().getReason()).isNull();
    assertThat(item2.getDeprecation().getShortReason()).isNull();
    assertThat(item5.getDeprecation().getReplacement()).isEqualTo("spring.server.name");
    assertThat(item5.getDeprecation().getLevel()).isEqualTo(Deprecation.Level.WARNING);
  }

  @Test
  void multiGroupsMetadata() throws IOException {
    RawConfigurationMetadata rawMetadata = readFor("multi-groups");
    List<ConfigurationMetadataItem> items = rawMetadata.getItems();
    assertThat(items).hasSize(3);

    ConfigurationMetadataItem item = items.get(0);
    assertThat(item.getName()).isEqualTo("enabled");
    assertThat(item.getSourceType()).isEqualTo("com.example.Retry");

    ConfigurationMetadataItem item2 = items.get(1);
    assertThat(item2.getName()).isEqualTo("enabled");
    assertThat(item2.getSourceType()).isEqualTo("com.example.Retry");

    ConfigurationMetadataItem item3 = items.get(2);
    assertThat(item3.getName()).isEqualTo("enabled");
    assertThat(item3.getSourceType()).isEqualTo("com.example.Retry");
  }

  RawConfigurationMetadata readFor(String path) throws IOException {
    return this.reader.read(getInputStreamFor(path), DEFAULT_CHARSET);
  }

}
