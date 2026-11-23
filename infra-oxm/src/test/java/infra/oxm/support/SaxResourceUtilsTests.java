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

package infra.oxm.support;

import org.junit.jupiter.api.Test;

import infra.core.io.ByteArrayResource;
import infra.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class SaxResourceUtilsTests {

  @Test
  void usesResourceUriWhenAvailable() throws Exception {
    var resource = new ClassPathResource("schema/flight.xsd");
    var source = SaxResourceUtils.createInputSource(resource);
    try (var stream = source.getByteStream()) {
      assertThat(source.getSystemId()).isEqualTo(resource.getURI().toString());
      assertThat(stream.read()).isNotEqualTo(-1);
    }
  }

  @Test
  void resourceWithoutUriStillProvidesContent() throws Exception {
    var source = SaxResourceUtils.createInputSource(new ByteArrayResource(new byte[] { 42 }));
    try (var stream = source.getByteStream()) {
      assertThat(source.getSystemId()).isNull();
      assertThat(stream.read()).isEqualTo(42);
    }
  }
}
