/*
 * Copyright 2002-present the original author or authors.
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

package infra.messaging.rsocket.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import infra.core.MethodParameter;
import infra.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetadataArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class MetadataArgumentResolverTests extends RSocketServiceArgumentResolverTestSupport {

  @Override
  protected RSocketServiceArgumentResolver initResolver() {
    return new MetadataArgumentResolver();
  }

  @Test
  void metadata() {
    MethodParameter param1 = initMethodParameter(Service.class, "execute", 0);
    MethodParameter param2 = initMethodParameter(Service.class, "execute", 1);
    MethodParameter param3 = initMethodParameter(Service.class, "execute", 2);
    MethodParameter param4 = initMethodParameter(Service.class, "execute", 3);

    assertThat(execute("foo", param1)).isTrue();
    assertThat(execute(MimeType.APPLICATION_JSON, param2)).isTrue();

    assertThat(execute("bar", param3)).isTrue();
    assertThat(execute(MimeType.APPLICATION_XML, param4)).isTrue();

    Map<Object, MimeType> expected = new LinkedHashMap<>();
    expected.put("foo", MimeType.APPLICATION_JSON);
    expected.put("bar", MimeType.APPLICATION_XML);

    assertThat(getRequestValues().getMetadata()).containsExactlyEntriesOf(expected);
  }

  @SuppressWarnings("unused")
  private interface Service {

    void execute(String metadata1, MimeType mimeType1, String metadata2, MimeType mimeType2);

    void executeNotAnnotated(String foo, String bar);

  }

}
