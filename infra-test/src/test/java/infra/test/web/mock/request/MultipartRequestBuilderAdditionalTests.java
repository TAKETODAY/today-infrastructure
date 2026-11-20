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

package infra.test.web.mock.request;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.web.mock.DefaultMockContext;
import infra.web.mock.MockMemoryFilePart;
import infra.web.mock.MockMemoryPart;
import infra.web.mock.MultipartMockRequest;
import infra.web.multipart.Part;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class MultipartRequestBuilderAdditionalTests {

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void partsUseDeclaredCharsetAndDefaultCharset(boolean legacy) {
    MockMemoryPart latin = new MockMemoryPart("latin", new byte[] { (byte) 0xe9 });
    latin.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
    MockMemoryPart plain = new MockMemoryPart("plain", "你好".getBytes(StandardCharsets.UTF_8));
    plain.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    MockMemoryPart file = new MockMemoryPart("file", "name.txt", "file".getBytes(StandardCharsets.UTF_8));
    AbstractMockRequestBuilder<?> builder = builder(legacy, latin, plain, file);
    MultipartMockRequest request = (MultipartMockRequest) builder.buildRequest(new DefaultMockContext());
    assertThat(request.getParameter("latin")).isEqualTo("é");
    assertThat(request.getParameter("plain")).isEqualTo("你好");
    assertThat(request.getMultipartRequest().getParts()).containsKeys("latin", "plain", "file");
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void readFailuresRetainPartNameAndCause(boolean legacy) throws Exception {
    Part part = mock(Part.class);
    IOException failure = new IOException("stream unavailable");
    given(part.getName()).willReturn("broken");
    given(part.getInputStream()).willThrow(failure);
    AbstractMockRequestBuilder<?> builder = builder(legacy, part);
    assertThatIllegalStateException().isThrownBy(() -> builder.buildRequest(new DefaultMockContext()))
            .withMessage("Failed to read content for part broken").withCause(failure);
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void mergeRetainsChildPartsAndAppendsParentFiles(boolean legacy) {
    AbstractMockRequestBuilder<?> parent = builder(legacy, new MockMemoryPart("shared", new byte[] { 'p' }),
            new MockMemoryPart("inherited", new byte[] { 'i' }));
    AbstractMockRequestBuilder<?> child = builder(legacy, new MockMemoryPart("shared", new byte[] { 'c' }));
    addFiles(parent);
    assertThat(child.merge(null)).isSameAs(child);
    assertThat(child.merge(parent)).isSameAs(child);
    MultipartMockRequest request = (MultipartMockRequest) child.buildRequest(new DefaultMockContext());
    assertThat(request.getParameter("shared")).isEqualTo("c");
    assertThat(request.getParameter("inherited")).isEqualTo("i");
    assertThat(request.getMultipartRequest().getParts()).containsKeys("first", "second");
    assertThatIllegalArgumentException().isThrownBy(() -> child.merge("invalid")).withMessageContaining("Cannot merge");
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void emptyPartsAreRejectedAndPlainParentIsAccepted(boolean legacy) {
    assertThatIllegalArgumentException().isThrownBy(() -> builder(legacy));
    AbstractMockRequestBuilder<?> builder = builder(legacy, new MockMemoryPart("name", new byte[0]));
    assertThat(builder.merge(new MockHttpRequestBuilder(HttpMethod.GET, "/").header("X-Parent", "value")))
            .isSameAs(builder);
    assertThat(builder.buildRequest(new DefaultMockContext()).getHeader("X-Parent")).isEqualTo("value");
  }

  private AbstractMockRequestBuilder<?> builder(boolean legacy, Part... parts) {
    return legacy ? new MockMultipartHttpRequestBuilder("/upload").part(parts)
            : new MultipartBuilder().part(parts).uri("/upload");
  }

  private void addFiles(AbstractMockRequestBuilder<?> builder) {
    MockMemoryFilePart file = new MockMemoryFilePart("second", new byte[] { 2 });
    if (builder instanceof MockMultipartHttpRequestBuilder legacy) {
      legacy.file("first", new byte[] { 1 }).file(file);
    }
    else {
      ((MultipartBuilder) builder).file("first", new byte[] { 1 }).file(file);
    }
  }

  static class MultipartBuilder extends AbstractMultipartMockRequestBuilder<MultipartBuilder> {
    MultipartBuilder() {
      super(HttpMethod.POST);
    }
  }

}
