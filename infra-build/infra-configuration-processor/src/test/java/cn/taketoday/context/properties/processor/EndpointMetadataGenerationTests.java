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

import java.time.Duration;

import cn.taketoday.context.properties.processor.metadata.ConfigurationMetadata;
import cn.taketoday.context.properties.processor.metadata.Metadata;
import cn.taketoday.context.properties.sample.endpoint.CamelCaseEndpoint;
import cn.taketoday.context.properties.sample.endpoint.CustomPropertiesEndpoint;
import cn.taketoday.context.properties.sample.endpoint.DisabledEndpoint;
import cn.taketoday.context.properties.sample.endpoint.EnabledEndpoint;
import cn.taketoday.context.properties.sample.endpoint.SimpleEndpoint;
import cn.taketoday.context.properties.sample.endpoint.SpecificEndpoint;
import cn.taketoday.context.properties.sample.endpoint.incremental.IncrementalEndpoint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata generation tests for Actuator endpoints.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class EndpointMetadataGenerationTests extends AbstractMetadataGenerationTests {

  @Test
  void simpleEndpoint() {
    ConfigurationMetadata metadata = compile(SimpleEndpoint.class);
    assertThat(metadata).has(Metadata.withGroup("management.endpoint.simple").fromSource(SimpleEndpoint.class));
    assertThat(metadata).has(enabledFlag("simple", true));
    assertThat(metadata).has(cacheTtl("simple"));
    assertThat(metadata.getItems()).hasSize(3);
  }

  @Test
  void disableEndpoint() {
    ConfigurationMetadata metadata = compile(DisabledEndpoint.class);
    assertThat(metadata).has(Metadata.withGroup("management.endpoint.disabled").fromSource(DisabledEndpoint.class));
    assertThat(metadata).has(enabledFlag("disabled", false));
    assertThat(metadata.getItems()).hasSize(2);
  }

  @Test
  void enabledEndpoint() {
    ConfigurationMetadata metadata = compile(EnabledEndpoint.class);
    assertThat(metadata).has(Metadata.withGroup("management.endpoint.enabled").fromSource(EnabledEndpoint.class));
    assertThat(metadata).has(enabledFlag("enabled", true));
    assertThat(metadata.getItems()).hasSize(2);
  }

  @Test
  void customPropertiesEndpoint() {
    ConfigurationMetadata metadata = compile(CustomPropertiesEndpoint.class);
    assertThat(metadata)
            .has(Metadata.withGroup("management.endpoint.customprops").fromSource(CustomPropertiesEndpoint.class));
    assertThat(metadata).has(Metadata.withProperty("management.endpoint.customprops.name")
            .ofType(String.class)
            .withDefaultValue("test"));
    assertThat(metadata).has(enabledFlag("customprops", true));
    assertThat(metadata).has(cacheTtl("customprops"));
    assertThat(metadata.getItems()).hasSize(4);
  }

  @Test
  void specificEndpoint() {
    ConfigurationMetadata metadata = compile(SpecificEndpoint.class);
    assertThat(metadata).has(Metadata.withGroup("management.endpoint.specific").fromSource(SpecificEndpoint.class));
    assertThat(metadata).has(enabledFlag("specific", true));
    assertThat(metadata).has(cacheTtl("specific"));
    assertThat(metadata.getItems()).hasSize(3);
  }

  @Test
  void camelCaseEndpoint() {
    ConfigurationMetadata metadata = compile(CamelCaseEndpoint.class);
    assertThat(metadata)
            .has(Metadata.withGroup("management.endpoint.pascal-case").fromSource(CamelCaseEndpoint.class));
    assertThat(metadata).has(enabledFlag("PascalCase", "pascal-case", true));
    assertThat(metadata.getItems()).hasSize(2);
  }

  @Test
  void incrementalEndpointBuildChangeGeneralEnabledFlag() {
    TestProject project = new TestProject(IncrementalEndpoint.class);
    ConfigurationMetadata metadata = project.compile();
    assertThat(metadata)
            .has(Metadata.withGroup("management.endpoint.incremental").fromSource(IncrementalEndpoint.class));
    assertThat(metadata).has(enabledFlag("incremental", true));
    assertThat(metadata).has(cacheTtl("incremental"));
    assertThat(metadata.getItems()).hasSize(3);
    project.replaceText(IncrementalEndpoint.class, "id = \"incremental\"",
            "id = \"incremental\", enableByDefault = false");
    metadata = project.compile();
    assertThat(metadata)
            .has(Metadata.withGroup("management.endpoint.incremental").fromSource(IncrementalEndpoint.class));
    assertThat(metadata).has(enabledFlag("incremental", false));
    assertThat(metadata).has(cacheTtl("incremental"));
    assertThat(metadata.getItems()).hasSize(3);
  }

  @Test
  void incrementalEndpointBuildChangeCacheFlag() {
    TestProject project = new TestProject(IncrementalEndpoint.class);
    ConfigurationMetadata metadata = project.compile();
    assertThat(metadata)
            .has(Metadata.withGroup("management.endpoint.incremental").fromSource(IncrementalEndpoint.class));
    assertThat(metadata).has(enabledFlag("incremental", true));
    assertThat(metadata).has(cacheTtl("incremental"));
    assertThat(metadata.getItems()).hasSize(3);
    project.replaceText(IncrementalEndpoint.class, "@Nullable String param", "String param");
    metadata = project.compile();
    assertThat(metadata)
            .has(Metadata.withGroup("management.endpoint.incremental").fromSource(IncrementalEndpoint.class));
    assertThat(metadata).has(enabledFlag("incremental", true));
    assertThat(metadata.getItems()).hasSize(2);
  }

  @Test
  void incrementalEndpointBuildEnableSpecificEndpoint() {
    TestProject project = new TestProject(SpecificEndpoint.class);
    ConfigurationMetadata metadata = project.compile();
    assertThat(metadata).has(Metadata.withGroup("management.endpoint.specific").fromSource(SpecificEndpoint.class));
    assertThat(metadata).has(enabledFlag("specific", true));
    assertThat(metadata).has(cacheTtl("specific"));
    assertThat(metadata.getItems()).hasSize(3);
    project.replaceText(SpecificEndpoint.class, "enableByDefault = true", "enableByDefault = false");
    metadata = project.compile();
    assertThat(metadata).has(Metadata.withGroup("management.endpoint.specific").fromSource(SpecificEndpoint.class));
    assertThat(metadata).has(enabledFlag("specific", false));
    assertThat(metadata).has(cacheTtl("specific"));
    assertThat(metadata.getItems()).hasSize(3);
  }

  private Metadata.MetadataItemCondition enabledFlag(String endpointId, String endpointSuffix, Boolean defaultValue) {
    return Metadata.withEnabledFlag("management.endpoint." + endpointSuffix + ".enabled")
            .withDefaultValue(defaultValue)
            .withDescription(String.format("Whether to enable the %s endpoint.", endpointId));
  }

  private Metadata.MetadataItemCondition enabledFlag(String endpointId, Boolean defaultValue) {
    return enabledFlag(endpointId, endpointId, defaultValue);
  }

  private Metadata.MetadataItemCondition cacheTtl(String endpointId) {
    return Metadata.withProperty("management.endpoint." + endpointId + ".cache.time-to-live")
            .ofType(Duration.class)
            .withDefaultValue("0ms")
            .withDescription("Maximum time that a response can be cached.");
  }

}
