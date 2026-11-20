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

package infra.mock.http;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MockHttpMessageTests {

  @Test
  void inputMessagePreservesBytesStreamAndMutableHeaders() throws IOException {
    MockHttpInputMessage message = new MockHttpInputMessage(new byte[] { 1, 2 });
    assertThat(message.getBody().readAllBytes()).containsExactly(1, 2);
    message.getHeaders().set("X-Test", "value");
    assertThat(message.getHeaders().getFirst("X-Test")).isEqualTo("value");
    InputStream stream = new ByteArrayInputStream(new byte[0]);
    assertThat(new MockHttpInputMessage(stream).getBody()).isSameAs(stream);
  }

  @Test
  void inputMessageRejectsMissingBody() {
    assertThatIllegalArgumentException().isThrownBy(() -> new MockHttpInputMessage((byte[]) null))
            .withMessage("Byte array is required");
    assertThatIllegalArgumentException().isThrownBy(() -> new MockHttpInputMessage((InputStream) null))
            .withMessage("InputStream is required");
  }

  @Test
  void outputMessageExposesBytesAndDecodedContent() throws IOException {
    MockHttpOutputMessage message = new MockHttpOutputMessage();
    assertThat(message.getBodyAsBytes()).isEmpty();
    assertThat(message.getBodyAsString()).isEmpty();
    message.getHeaders().set("X-Test", "value");
    assertThat(message.getHeaders().getFirst("X-Test")).isEqualTo("value");
    message.getBody().write("你好".getBytes(StandardCharsets.UTF_8));
    assertThat(message.getBodyAsBytes()).isEqualTo("你好".getBytes(StandardCharsets.UTF_8));
    assertThat(message.getBodyAsString()).isEqualTo("你好");
    MockHttpOutputMessage latin = new MockHttpOutputMessage();
    latin.getBody().write(new byte[] { (byte) 0xe9 });
    assertThat(latin.getBodyAsString(StandardCharsets.ISO_8859_1)).isEqualTo("é");
  }

}
