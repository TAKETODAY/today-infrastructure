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

package infra.messaging.rsocket;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.core.ParameterizedTypeReference;
import infra.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataExtractorRegistryTests {

  @Test
  void defaultRegistrationUsesExplicitOrMimeTypeKey() {
    MimeType mimeType = new MimeType("application", "test");
    RecordingRegistry registry = new RecordingRegistry();

    registry.metadataToExtract(mimeType, String.class, "named");
    registry.classMapper.accept("value", registry.values);
    assertThat(registry.values).containsEntry("named", "value");

    registry.values.clear();
    registry.metadataToExtract(mimeType, String.class, (String) null);
    registry.classMapper.accept("value", registry.values);
    assertThat(registry.values).containsEntry(mimeType.toString(), "value");
  }

  @Test
  void parameterizedRegistrationUsesExplicitOrMimeTypeKey() {
    MimeType mimeType = new MimeType("application", "test");
    RecordingRegistry registry = new RecordingRegistry();
    ParameterizedTypeReference<List<String>> type = new ParameterizedTypeReference<>() { };

    registry.metadataToExtract(mimeType, type, "named");
    registry.parameterizedMapper.accept(List.of("value"), registry.values);
    assertThat(registry.values).containsEntry("named", List.of("value"));

    registry.values.clear();
    registry.metadataToExtract(mimeType, type, (String) null);
    registry.parameterizedMapper.accept(List.of("value"), registry.values);
    assertThat(registry.values).containsEntry(mimeType.toString(), List.of("value"));
  }

  private static class RecordingRegistry implements MetadataExtractorRegistry {

    private final Map<String, Object> values = new HashMap<>();

    private java.util.function.BiConsumer<String, Map<String, Object>> classMapper;

    private java.util.function.BiConsumer<List<String>, Map<String, Object>> parameterizedMapper;

    @Override
    public <T> void metadataToExtract(MimeType mimeType, Class<T> targetType,
            java.util.function.BiConsumer<T, Map<String, Object>> mapper) {
      @SuppressWarnings("unchecked")
      java.util.function.BiConsumer<String, Map<String, Object>> typed =
              (java.util.function.BiConsumer<String, Map<String, Object>>) (java.util.function.BiConsumer<?, ?>) mapper;
      this.classMapper = typed;
    }

    @Override
    public <T> void metadataToExtract(MimeType mimeType, ParameterizedTypeReference<T> type,
            java.util.function.BiConsumer<T, Map<String, Object>> mapper) {
      @SuppressWarnings("unchecked")
      java.util.function.BiConsumer<List<String>, Map<String, Object>> typed =
              (java.util.function.BiConsumer<List<String>, Map<String, Object>>) (java.util.function.BiConsumer<?, ?>) mapper;
      this.parameterizedMapper = typed;
    }
  }

}
