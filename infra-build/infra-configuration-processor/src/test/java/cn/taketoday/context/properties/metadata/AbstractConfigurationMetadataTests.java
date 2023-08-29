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

import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base for configuration meta-data tests.
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractConfigurationMetadataTests {

  protected void assertSource(ConfigurationMetadataSource actual, String groupId, String type, String sourceType) {
    assertThat(actual).isNotNull();
    assertThat(actual.getGroupId()).isEqualTo(groupId);
    assertThat(actual.getType()).isEqualTo(type);
    assertThat(actual.getSourceType()).isEqualTo(sourceType);
  }

  protected void assertProperty(ConfigurationMetadataProperty actual, String id, String name, Class<?> type,
          Object defaultValue) {
    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(id);
    assertThat(actual.getName()).isEqualTo(name);
    String typeName = (type != null) ? type.getName() : null;
    assertThat(actual.getType()).isEqualTo(typeName);
    assertThat(actual.getDefaultValue()).isEqualTo(defaultValue);
  }

  protected void assertItem(ConfigurationMetadataItem actual, String sourceType) {
    assertThat(actual).isNotNull();
    assertThat(actual.getSourceType()).isEqualTo(sourceType);
  }

  protected InputStream getInputStreamFor(String name) throws IOException {
    Resource r = new ClassPathResource("metadata/configuration-metadata-" + name + ".json");
    return r.getInputStream();
  }

}
