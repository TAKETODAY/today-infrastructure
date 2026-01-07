/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.app.jackson;

import org.junit.jupiter.api.Test;

import infra.app.jackson.NameAndAgeJacksonComponent.Serializer;
import infra.app.jackson.types.NameAndAge;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ObjectValueSerializer}.
 *
 * @author Phillip Webb
 */
class ObjectValueSerializerTests {

  @Test
  void serializeObjectShouldWriteJson() throws Exception {
    Serializer serializer = new Serializer();
    SimpleModule module = new SimpleModule();
    module.addSerializer(NameAndAge.class, serializer);
    JsonMapper mapper = JsonMapper.builder().addModule(module).build();
    String json = mapper.writeValueAsString(NameAndAge.create("infra", 100));
    assertThat(json).isEqualToIgnoringWhitespace("{\"theName\":\"infra\",\"theAge\":100}");
  }

}
