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

package cn.taketoday.buildpack.platform.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SharedObjectMapper}.
 *
 * @author Phillip Webb
 */
class SharedObjectMapperTests {

  @Test
  void getReturnsConfiguredObjectMapper() {
    ObjectMapper mapper = SharedObjectMapper.get();
    assertThat(mapper).isNotNull();
    assertThat(mapper.getRegisteredModuleIds()).contains(new ParameterNamesModule().getTypeId());
    assertThat(SerializationFeature.INDENT_OUTPUT
            .enabledIn(mapper.getSerializationConfig().getSerializationFeatures())).isTrue();
    assertThat(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
            .enabledIn(mapper.getDeserializationConfig().getDeserializationFeatures())).isFalse();
    assertThat(mapper.getSerializationConfig().getPropertyNamingStrategy())
            .isEqualTo(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    assertThat(mapper.getDeserializationConfig().getPropertyNamingStrategy())
            .isEqualTo(PropertyNamingStrategies.LOWER_CAMEL_CASE);
  }

}
