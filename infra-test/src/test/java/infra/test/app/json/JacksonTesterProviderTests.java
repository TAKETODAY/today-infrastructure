/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.test.app.json;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingException;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import infra.core.ResolvableType;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mockConstruction;

class JacksonTesterProviderTests {

  @Test
  void providerParsesBytesAndStreamsAndCreatesMutableContainers() throws Exception {
    Configuration configuration = configuration();
    var provider = configuration.jsonProvider();
    byte[] json = "{\"value\":42}".getBytes(StandardCharsets.UTF_8);
    assertThat(provider.parse(json)).isEqualTo(Map.of("value", 42));
    assertThat(provider.parse(new ByteArrayInputStream(json), "UTF-8")).isEqualTo(Map.of("value", 42));
    assertThat(provider.toJson(Map.of("value", 42))).isEqualTo("{\"value\":42}");
    Object array = provider.createArray();
    provider.setArrayIndex(array, 0, "value");
    assertThat(provider.getArrayIndex(array, 0)).isEqualTo("value");
    Object map = provider.createMap();
    provider.setProperty(map, "value", 42);
    assertThat(provider.getMapValue(map, "value")).isEqualTo(42);
  }

  @Test
  void providerWrapsInvalidJsonAndStreamEncodingErrors() throws Exception {
    var provider = configuration().jsonProvider();
    byte[] invalid = "{".getBytes(StandardCharsets.UTF_8);
    assertThatExceptionOfType(InvalidJsonException.class).isThrownBy(() -> provider.parse("{"));
    assertThatExceptionOfType(InvalidJsonException.class).isThrownBy(() -> provider.parse(invalid));
    assertThatExceptionOfType(InvalidJsonException.class)
            .isThrownBy(() -> provider.parse(new ByteArrayInputStream(invalid), "UTF-8"));
    assertThatExceptionOfType(InvalidJsonException.class)
            .isThrownBy(() -> provider.parse(new ByteArrayInputStream(invalid), "invalid-charset"));
  }

  @Test
  void mappingProviderConvertsClassAndGenericTypesAndPreservesNulls() throws Exception {
    Configuration configuration = configuration();
    var provider = configuration.mappingProvider();
    assertThat(provider.map("42", Integer.class, configuration)).isEqualTo(42);
    assertThat(provider.map(null, Integer.class, configuration)).isNull();
    TypeRef<List<Integer>> integers = new TypeRef<>() { };
    assertThat(provider.map(List.of("1", "2"), integers, configuration)).containsExactly(1, 2);
    assertThat(provider.map(null, integers, configuration)).isNull();
    assertThatExceptionOfType(MappingException.class)
            .isThrownBy(() -> provider.map("not-an-integer", Integer.class, configuration));
    assertThatExceptionOfType(MappingException.class)
            .isThrownBy(() -> provider.map(List.of("not-an-integer"), integers, configuration));
  }

  private Configuration configuration() throws Exception {
    AtomicReference<Configuration> result = new AtomicReference<>();
    try (var construction = mockConstruction(JsonContent.class,
            (mock, context) -> result.set((Configuration) context.arguments().get(3)))) {
      var tester = new JacksonTester<>(getClass(), ResolvableType.forClass(String.class), new JsonMapper());
      tester.write("value");
      assertThat(construction.constructed()).hasSize(1);
    }
    return result.get();
  }

}
