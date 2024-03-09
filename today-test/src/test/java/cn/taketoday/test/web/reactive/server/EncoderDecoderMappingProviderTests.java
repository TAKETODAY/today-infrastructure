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

package cn.taketoday.test.web.reactive.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.TypeRef;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import cn.taketoday.http.codec.json.Jackson2JsonDecoder;
import cn.taketoday.http.codec.json.Jackson2JsonEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/9 20:46
 */
class EncoderDecoderMappingProviderTests {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final EncoderDecoderMappingProvider mappingProvider = new EncoderDecoderMappingProvider(
          new Jackson2JsonEncoder(objectMapper), new Jackson2JsonDecoder(objectMapper));

  @Test
  void mapType() {
    Data data = this.mappingProvider.map(jsonData("test", 42), Data.class, Configuration.defaultConfiguration());
    assertThat(data).isEqualTo(new Data("test", 42));
  }

  @Test
  void mapGenericType() {
    List<?> jsonData = List.of(jsonData("first", 1), jsonData("second", 2), jsonData("third", 3));
    List<Data> data = this.mappingProvider.map(jsonData, new TypeRef<List<Data>>() { }, Configuration.defaultConfiguration());
    assertThat(data).containsExactly(new Data("first", 1), new Data("second", 2), new Data("third", 3));
  }

  private Map<String, Object> jsonData(String name, int counter) {
    return Map.of("name", name, "counter", counter);
  }

  record Data(String name, int counter) { }

}