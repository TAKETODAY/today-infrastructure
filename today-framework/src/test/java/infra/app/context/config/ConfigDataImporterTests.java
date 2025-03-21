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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import infra.context.properties.bind.Binder;
import infra.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link ConfigDataImporter}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class ConfigDataImporterTests {

  @Mock
  private ConfigDataLocationResolvers resolvers;

  @Mock
  private ConfigDataLoaders loaders;

  @Mock
  private Binder binder;

  @Mock
  private ConfigDataLocationResolverContext locationResolverContext;

  @Mock
  private ConfigDataLoaderContext loaderContext;

  @Mock
  private ConfigDataActivationContext activationContext;

  @Mock
  private Profiles profiles;

  @BeforeEach
  void setup() {
    activationContext = new ConfigDataActivationContext(null, profiles);
  }

  @Test
  void loadImportsResolvesAndLoadsLocations() throws Exception {
    ConfigDataLocation location1 = ConfigDataLocation.valueOf("test1");
    ConfigDataLocation location2 = ConfigDataLocation.valueOf("test2");
    TestResource resource1 = new TestResource("r1");
    TestResource resource2 = new TestResource("r2");
    ConfigData configData1 = new ConfigData(Collections.singleton(new MockPropertySource()));
    ConfigData configData2 = new ConfigData(Collections.singleton(new MockPropertySource()));
    given(this.resolvers.resolve(this.locationResolverContext, location1, this.profiles))
            .willReturn(Collections.singletonList(new ConfigDataResolutionResult(location1, resource1, false)));
    given(this.resolvers.resolve(this.locationResolverContext, location2, this.profiles))
            .willReturn(Collections.singletonList(new ConfigDataResolutionResult(location2, resource2, false)));
    given(this.loaders.load(this.loaderContext, resource1)).willReturn(configData1);
    given(this.loaders.load(this.loaderContext, resource2)).willReturn(configData2);
    ConfigDataImporter importer = new ConfigDataImporter(ConfigDataNotFoundAction.FAIL, this.resolvers, this.loaders);
    Collection<ConfigData> loaded = importer.resolveAndLoad(this.activationContext, this.locationResolverContext,
            this.loaderContext, Arrays.asList(location1, location2)).values();
    assertThat(loaded).containsExactly(configData2, configData1);
  }

  @Test
  void loadImportsWhenAlreadyImportedLocationSkipsLoad() throws Exception {
    ConfigDataLocation location1 = ConfigDataLocation.valueOf("test1");
    ConfigDataLocation location2 = ConfigDataLocation.valueOf("test2");
    ConfigDataLocation location3 = ConfigDataLocation.valueOf("test3");
    List<ConfigDataLocation> locations1and2 = Arrays.asList(location1, location2);
    List<ConfigDataLocation> locations2and3 = Arrays.asList(location2, location3);
    TestResource resource1 = new TestResource("r1");
    TestResource resource2 = new TestResource("r2");
    TestResource resource3 = new TestResource("r3");
    ConfigData configData1 = new ConfigData(Collections.singleton(new MockPropertySource()));
    ConfigData configData2 = new ConfigData(Collections.singleton(new MockPropertySource()));
    ConfigData configData3 = new ConfigData(Collections.singleton(new MockPropertySource()));
    given(this.resolvers.resolve(this.locationResolverContext, location1, this.profiles))
            .willReturn(Collections.singletonList(new ConfigDataResolutionResult(location1, resource1, false)));
    given(this.resolvers.resolve(this.locationResolverContext, location2, this.profiles))
            .willReturn(Collections.singletonList(new ConfigDataResolutionResult(location2, resource2, false)));
    given(this.resolvers.resolve(this.locationResolverContext, location3, this.profiles))
            .willReturn(Collections.singletonList(new ConfigDataResolutionResult(location3, resource3, false)));
    given(this.loaders.load(this.loaderContext, resource1)).willReturn(configData1);
    given(this.loaders.load(this.loaderContext, resource2)).willReturn(configData2);
    given(this.loaders.load(this.loaderContext, resource3)).willReturn(configData3);
    ConfigDataImporter importer = new ConfigDataImporter(ConfigDataNotFoundAction.FAIL,
            this.resolvers, this.loaders);
    Collection<ConfigData> loaded1and2 = importer.resolveAndLoad(this.activationContext,
            this.locationResolverContext, this.loaderContext, locations1and2).values();
    Collection<ConfigData> loaded2and3 = importer.resolveAndLoad(this.activationContext,
            this.locationResolverContext, this.loaderContext, locations2and3).values();
    assertThat(loaded1and2).containsExactly(configData2, configData1);
    assertThat(loaded2and3).containsExactly(configData3);
  }

  static class TestResource extends ConfigDataResource {

    private final String name;

    TestResource(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }

  }

}
