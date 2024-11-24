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

import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.app.context.config.ConfigData;
import infra.app.context.config.ConfigDataLoaderContext;
import infra.app.context.config.ConfigDataLocation;
import infra.app.context.config.StandardConfigDataLoader;
import infra.app.context.config.StandardConfigDataReference;
import infra.app.context.config.StandardConfigDataResource;
import infra.core.env.PropertySource;
import infra.core.io.ClassPathResource;
import infra.app.env.PropertiesPropertySourceLoader;
import infra.app.env.YamlPropertySourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StandardConfigDataLoader}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class StandardConfigDataLoaderTests {

  private StandardConfigDataLoader loader = new StandardConfigDataLoader();

  private ConfigDataLoaderContext loaderContext = mock(ConfigDataLoaderContext.class);

  @Test
  void loadWhenLocationResultsInMultiplePropertySourcesAddsAllToConfigData() throws IOException {
    ClassPathResource resource = new ClassPathResource("configdata/yaml/application.yml");
    StandardConfigDataReference reference = new StandardConfigDataReference(
            ConfigDataLocation.valueOf("classpath:configdata/yaml/application.yml"), null,
            "classpath:configdata/yaml/application", null, "yml", new YamlPropertySourceLoader());
    StandardConfigDataResource location = new StandardConfigDataResource(reference, resource);
    ConfigData configData = this.loader.load(this.loaderContext, location);
    assertThat(configData.getPropertySources().size()).isEqualTo(2);
    PropertySource<?> source1 = configData.getPropertySources().get(0);
    PropertySource<?> source2 = configData.getPropertySources().get(1);
    assertThat(source1.getName())
            .isEqualTo("Config resource 'class path resource [configdata/yaml/application.yml]' "
                    + "via location 'classpath:configdata/yaml/application.yml' (document #0)");
    assertThat(source1.getProperty("foo")).isEqualTo("bar");
    assertThat(source2.getName())
            .isEqualTo("Config resource 'class path resource [configdata/yaml/application.yml]' "
                    + "via location 'classpath:configdata/yaml/application.yml' (document #1)");
    assertThat(source2.getProperty("hello")).isEqualTo("world");
  }

  @Test
  void loadWhenPropertySourceIsEmptyAddsNothingToConfigData() throws IOException {
    ClassPathResource resource = new ClassPathResource("config/0-empty/testproperties.properties");
    StandardConfigDataReference reference = new StandardConfigDataReference(
            ConfigDataLocation.valueOf("classpath:config/0-empty/testproperties.properties"), null,
            "config/0-empty/testproperties", null, "properties", new PropertiesPropertySourceLoader());
    StandardConfigDataResource location = new StandardConfigDataResource(reference, resource);
    ConfigData configData = this.loader.load(this.loaderContext, location);
    assertThat(configData.getPropertySources().size()).isEqualTo(0);
  }

}
