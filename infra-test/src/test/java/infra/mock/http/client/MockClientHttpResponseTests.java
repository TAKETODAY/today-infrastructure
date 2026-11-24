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

package infra.mock.http.client;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.http.HttpStatus;
import infra.http.HttpStatusCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MockClientHttpResponseTests {

  @Test
  void byteBodyWithKnownAndCustomStatus() throws IOException {
    MockClientHttpResponse known = new MockClientHttpResponse(new byte[] { 1, 2 }, HttpStatus.CREATED);
    assertThat(known.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(known.getRawStatusCode()).isEqualTo(201);
    assertThat(known.getStatusText()).isEqualTo("Created");
    assertThat(known.getBody().readAllBytes()).containsExactly(1, 2);

    MockClientHttpResponse custom = new MockClientHttpResponse(new byte[0], 599);
    assertThat(custom.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(599));
    assertThat(custom.getRawStatusCode()).isEqualTo(599);
    assertThat(custom.getStatusText()).isEmpty();
  }

  @Test
  void streamBodyIsPreservedForBothStatusConstructors() throws IOException {
    ByteArrayInputStream body = new ByteArrayInputStream(new byte[] { 3 });
    assertThat(new MockClientHttpResponse(body, HttpStatus.OK).getBody()).isSameAs(body);
    MockClientHttpResponse custom = new MockClientHttpResponse(body, 499);
    assertThat(custom.getBody()).isSameAs(body);
    assertThat(custom.getStatusText()).isEmpty();
  }

  @Test
  void rejectsMissingStatus() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new MockClientHttpResponse(new byte[0], null))
            .withMessage("HttpStatusCode is required");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new MockClientHttpResponse(new ByteArrayInputStream(new byte[0]), null))
            .withMessage("HttpStatus is required");
  }

  @Test
  void closeClosesBodyAndIgnoresIOException() {
    AtomicBoolean closed = new AtomicBoolean();
    ByteArrayInputStream body = new ByteArrayInputStream(new byte[0]) {
      @Override
      public void close() throws IOException {
        closed.set(true);
        throw new IOException("close failed");
      }
    };
    assertThatCode(() -> new MockClientHttpResponse(body, 200).close()).doesNotThrowAnyException();
    assertThat(closed).isTrue();
    assertThatCode(() -> new MockClientHttpResponse(new byte[0], 200).close()).doesNotThrowAnyException();
  }

}
