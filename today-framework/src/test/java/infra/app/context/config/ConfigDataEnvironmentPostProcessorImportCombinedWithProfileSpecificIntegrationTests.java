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

package infra.app.context.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.app.Application;
import infra.app.ApplicationType;
import infra.app.context.config.ConfigData.Option;
import infra.app.context.config.ConfigData.Options;
import infra.context.ConfigurableApplicationContext;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ConfigDataEnvironmentPostProcessor} config data imports
 * that are combined with profile-specific files.
 *
 * @author Phillip Webb
 */
class ConfigDataEnvironmentPostProcessorImportCombinedWithProfileSpecificIntegrationTests {

  private Application application;

  @TempDir
  public File temp;

  @BeforeEach
  void setup() {
    this.application = new Application(ConfigDataEnvironmentPostProcessorIntegrationTests.Config.class);
    this.application.setApplicationType(ApplicationType.NORMAL);
  }

  @Test
  void testWithoutProfile() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.name=configimportwithprofilespecific");
    String value = context.getEnvironment().getProperty("prop");
    assertThat(value).isEqualTo("fromicwps1");
  }

  @Test
  void testWithProfile() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.name=configimportwithprofilespecific", "--infra.profiles.active=prod");
    String value = context.getEnvironment().getProperty("prop");
    assertThat(value).isEqualTo("fromicwps2");
  }

  static class LocationResolver implements ConfigDataLocationResolver<Resource> {

    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
      return location.hasPrefix("icwps:");
    }

    @Override
    public List<Resource> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
      return Collections.emptyList();
    }

    @Override
    public List<Resource> resolveProfileSpecific(ConfigDataLocationResolverContext context,
            ConfigDataLocation location, Profiles profiles) {
      return Collections.singletonList(new Resource(profiles));
    }

  }

  static class Loader implements ConfigDataLoader<Resource> {

    @Override
    public ConfigData load(ConfigDataLoaderContext context, Resource resource) throws IOException {
      List<PropertySource<?>> propertySources = new ArrayList<>();
      Map<PropertySource<?>, Options> propertySourceOptions = new HashMap<>();
      propertySources.add(new MapPropertySource("icwps1", Collections.singletonMap("prop", "fromicwps1")));
      if (resource.profiles.isAccepted("prod")) {
        MapPropertySource profileSpecificPropertySource = new MapPropertySource("icwps2",
                Collections.singletonMap("prop", "fromicwps2"));
        propertySources.add(profileSpecificPropertySource);
        propertySourceOptions.put(profileSpecificPropertySource, Options.of(Option.PROFILE_SPECIFIC));
      }
      return new ConfigData(propertySources, propertySourceOptions::get);
    }

  }

  private static class Resource extends ConfigDataResource {

    private final Profiles profiles;

    Resource(Profiles profiles) {
      this.profiles = profiles;
    }

    @Override
    public String toString() {
      return "icwps:";
    }

  }

}
