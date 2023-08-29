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

package cn.taketoday.context.properties.processor;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.properties.processor.metadata.ConfigurationMetadata;
import cn.taketoday.context.properties.processor.metadata.Metadata;
import cn.taketoday.context.properties.sample.lombok.LombokAccessLevelOverwriteDataProperties;
import cn.taketoday.context.properties.sample.lombok.LombokAccessLevelOverwriteDefaultProperties;
import cn.taketoday.context.properties.sample.lombok.LombokAccessLevelOverwriteExplicitProperties;
import cn.taketoday.context.properties.sample.lombok.LombokAccessLevelProperties;
import cn.taketoday.context.properties.sample.lombok.LombokExplicitProperties;
import cn.taketoday.context.properties.sample.lombok.LombokInnerClassProperties;
import cn.taketoday.context.properties.sample.lombok.LombokInnerClassWithGetterProperties;
import cn.taketoday.context.properties.sample.lombok.LombokSimpleDataProperties;
import cn.taketoday.context.properties.sample.lombok.LombokSimpleProperties;
import cn.taketoday.context.properties.sample.lombok.LombokSimpleValueProperties;
import cn.taketoday.context.properties.sample.lombok.SimpleLombokPojo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for Lombok classes.
 *
 * @author Stephane Nicoll
 */
class LombokMetadataGenerationTests extends AbstractMetadataGenerationTests {

  @Test
  void lombokDataProperties() {
    ConfigurationMetadata metadata = compile(LombokSimpleDataProperties.class);
    assertSimpleLombokProperties(metadata, LombokSimpleDataProperties.class, "data");
  }

  @Test
  void lombokValueProperties() {
    ConfigurationMetadata metadata = compile(LombokSimpleValueProperties.class);
    assertSimpleLombokProperties(metadata, LombokSimpleValueProperties.class, "value");
  }

  @Test
  void lombokSimpleProperties() {
    ConfigurationMetadata metadata = compile(LombokSimpleProperties.class);
    assertSimpleLombokProperties(metadata, LombokSimpleProperties.class, "simple");
  }

  @Test
  void lombokExplicitProperties() {
    ConfigurationMetadata metadata = compile(LombokExplicitProperties.class);
    assertSimpleLombokProperties(metadata, LombokExplicitProperties.class, "explicit");
    assertThat(metadata.getItems()).hasSize(6);
  }

  @Test
  void lombokAccessLevelProperties() {
    ConfigurationMetadata metadata = compile(LombokAccessLevelProperties.class);
    assertAccessLevelLombokProperties(metadata, LombokAccessLevelProperties.class, "accesslevel", 2);
  }

  @Test
  void lombokAccessLevelOverwriteDataProperties() {
    ConfigurationMetadata metadata = compile(LombokAccessLevelOverwriteDataProperties.class);
    assertAccessLevelOverwriteLombokProperties(metadata, LombokAccessLevelOverwriteDataProperties.class,
            "accesslevel.overwrite.data");
  }

  @Test
  void lombokAccessLevelOverwriteExplicitProperties() {
    ConfigurationMetadata metadata = compile(LombokAccessLevelOverwriteExplicitProperties.class);
    assertAccessLevelOverwriteLombokProperties(metadata, LombokAccessLevelOverwriteExplicitProperties.class,
            "accesslevel.overwrite.explicit");
  }

  @Test
  void lombokAccessLevelOverwriteDefaultProperties() {
    ConfigurationMetadata metadata = compile(LombokAccessLevelOverwriteDefaultProperties.class);
    assertAccessLevelOverwriteLombokProperties(metadata, LombokAccessLevelOverwriteDefaultProperties.class,
            "accesslevel.overwrite.default");
  }

  @Test
  void lombokInnerClassProperties() {
    ConfigurationMetadata metadata = compile(LombokInnerClassProperties.class);
    assertThat(metadata).has(Metadata.withGroup("config").fromSource(LombokInnerClassProperties.class));
    assertThat(metadata).has(Metadata.withGroup("config.first")
            .ofType(LombokInnerClassProperties.Foo.class)
            .fromSource(LombokInnerClassProperties.class));
    assertThat(metadata).has(Metadata.withProperty("config.first.name"));
    assertThat(metadata).has(Metadata.withProperty("config.first.bar.name"));
    assertThat(metadata).has(Metadata.withGroup("config.second", LombokInnerClassProperties.Foo.class)
            .fromSource(LombokInnerClassProperties.class));
    assertThat(metadata).has(Metadata.withProperty("config.second.name"));
    assertThat(metadata).has(Metadata.withProperty("config.second.bar.name"));
    assertThat(metadata).has(Metadata.withGroup("config.third")
            .ofType(SimpleLombokPojo.class)
            .fromSource(LombokInnerClassProperties.class));
    // For some reason the annotation processor resolves a type for SimpleLombokPojo
    // that is resolved (compiled) and the source annotations are gone. Because we
    // don't see the @Data annotation anymore, no field is harvested. What is crazy is
    // that a sample project works fine so this seems to be related to the unit test
    // environment for some reason. assertThat(metadata,
    // containsProperty("config.third.value"));
    assertThat(metadata).has(Metadata.withProperty("config.fourth"));
    assertThat(metadata).isNotEqualTo(Metadata.withGroup("config.fourth"));
  }

  @Test
  void lombokInnerClassWithGetterProperties() {
    ConfigurationMetadata metadata = compile(LombokInnerClassWithGetterProperties.class);
    assertThat(metadata).has(Metadata.withGroup("config").fromSource(LombokInnerClassWithGetterProperties.class));
    assertThat(metadata).has(Metadata.withGroup("config.first")
            .ofType(LombokInnerClassWithGetterProperties.Foo.class)
            .fromSourceMethod("getFirst()")
            .fromSource(LombokInnerClassWithGetterProperties.class));
    assertThat(metadata).has(Metadata.withProperty("config.first.name"));
    assertThat(metadata.getItems()).hasSize(3);
  }

  private void assertSimpleLombokProperties(ConfigurationMetadata metadata, Class<?> source, String prefix) {
    assertThat(metadata).has(Metadata.withGroup(prefix).fromSource(source));
    assertThat(metadata).doesNotHave(Metadata.withProperty(prefix + ".id"));
    assertThat(metadata).has(Metadata.withProperty(prefix + ".name", String.class)
            .fromSource(source)
            .withDescription("Name description."));
    assertThat(metadata).has(Metadata.withProperty(prefix + ".description"));
    assertThat(metadata).has(Metadata.withProperty(prefix + ".counter"));
    assertThat(metadata)
            .has(Metadata.withProperty(prefix + ".number").fromSource(source).withDefaultValue(0).withDeprecation());
    assertThat(metadata).has(Metadata.withProperty(prefix + ".items"));
    assertThat(metadata).doesNotHave(Metadata.withProperty(prefix + ".ignored"));
  }

  private void assertAccessLevelOverwriteLombokProperties(ConfigurationMetadata metadata, Class<?> source,
          String prefix) {
    assertAccessLevelLombokProperties(metadata, source, prefix, 7);
  }

  private void assertAccessLevelLombokProperties(ConfigurationMetadata metadata, Class<?> source, String prefix,
          int countNameFields) {
    assertThat(metadata).has(Metadata.withGroup(prefix).fromSource(source));
    for (int i = 0; i < countNameFields; i++) {
      assertThat(metadata).has(Metadata.withProperty(prefix + ".name" + i, String.class));
    }
    assertThat(metadata.getItems()).hasSize(1 + countNameFields);
  }

}
