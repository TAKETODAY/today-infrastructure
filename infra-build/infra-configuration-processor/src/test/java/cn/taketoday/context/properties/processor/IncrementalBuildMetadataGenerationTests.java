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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.properties.processor.metadata.ConfigurationMetadata;
import cn.taketoday.context.properties.processor.metadata.Metadata;
import cn.taketoday.context.properties.sample.incremental.BarProperties;
import cn.taketoday.context.properties.sample.incremental.FooProperties;
import cn.taketoday.context.properties.sample.incremental.RenamedBarProperties;
import cn.taketoday.context.properties.sample.simple.ClassWithNestedProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for incremental builds.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class IncrementalBuildMetadataGenerationTests extends AbstractMetadataGenerationTests {

  @Test
  void incrementalBuild() throws Exception {
    TestProject project = new TestProject(FooProperties.class, BarProperties.class);
    ConfigurationMetadata metadata = project.compile();
    assertThat(metadata)
            .has(Metadata.withProperty("foo.counter").fromSource(FooProperties.class).withDefaultValue(0));
    assertThat(metadata)
            .has(Metadata.withProperty("bar.counter").fromSource(BarProperties.class).withDefaultValue(0));
    metadata = project.compile();
    assertThat(metadata)
            .has(Metadata.withProperty("foo.counter").fromSource(FooProperties.class).withDefaultValue(0));
    assertThat(metadata)
            .has(Metadata.withProperty("bar.counter").fromSource(BarProperties.class).withDefaultValue(0));
    project.addSourceCode(BarProperties.class, BarProperties.class.getResourceAsStream("BarProperties.snippet"));
    metadata = project.compile();
    assertThat(metadata).has(Metadata.withProperty("bar.extra"));
    assertThat(metadata).has(Metadata.withProperty("foo.counter").withDefaultValue(0));
    assertThat(metadata).has(Metadata.withProperty("bar.counter").withDefaultValue(0));
    project.revert(BarProperties.class);
    metadata = project.compile();
    assertThat(metadata).isNotEqualTo(Metadata.withProperty("bar.extra"));
    assertThat(metadata).has(Metadata.withProperty("foo.counter").withDefaultValue(0));
    assertThat(metadata).has(Metadata.withProperty("bar.counter").withDefaultValue(0));
  }

  @Test
  void incrementalBuildAnnotationRemoved() {
    TestProject project = new TestProject(FooProperties.class, BarProperties.class);
    ConfigurationMetadata metadata = project.compile();
    assertThat(metadata).has(Metadata.withProperty("foo.counter").withDefaultValue(0));
    assertThat(metadata).has(Metadata.withProperty("bar.counter").withDefaultValue(0));
    project.replaceText(BarProperties.class, "@ConfigurationProperties", "//@ConfigurationProperties");
    project.replaceText(FooProperties.class, "@ConfigurationProperties", "//@ConfigurationProperties");
    metadata = project.compile();
    assertThat(metadata).isNull();
  }

  @Test
  @Disabled("gh-26271")
  void incrementalBuildTypeRenamed() {
    TestProject project = new TestProject(FooProperties.class, BarProperties.class);
    ConfigurationMetadata metadata = project.compile();
    assertThat(metadata)
            .has(Metadata.withProperty("foo.counter").fromSource(FooProperties.class).withDefaultValue(0));
    assertThat(metadata)
            .has(Metadata.withProperty("bar.counter").fromSource(BarProperties.class).withDefaultValue(0));
    assertThat(metadata).doesNotHave(Metadata.withProperty("bar.counter").fromSource(RenamedBarProperties.class));
    project.delete(BarProperties.class);
    project.add(RenamedBarProperties.class);
    metadata = project.compile();
    assertThat(metadata)
            .has(Metadata.withProperty("foo.counter").fromSource(FooProperties.class).withDefaultValue(0));
    assertThat(metadata)
            .doesNotHave(Metadata.withProperty("bar.counter").fromSource(BarProperties.class).withDefaultValue(0));
    assertThat(metadata)
            .has(Metadata.withProperty("bar.counter").withDefaultValue(0).fromSource(RenamedBarProperties.class));
  }

  @Test
  void incrementalBuildDoesNotDeleteItems() {
    TestProject project = new TestProject(ClassWithNestedProperties.class, FooProperties.class);
    ConfigurationMetadata initialMetadata = project.compile();
    ConfigurationMetadata updatedMetadata = project.compile();
    assertThat(initialMetadata.getItems()).isEqualTo(updatedMetadata.getItems());
  }

}
