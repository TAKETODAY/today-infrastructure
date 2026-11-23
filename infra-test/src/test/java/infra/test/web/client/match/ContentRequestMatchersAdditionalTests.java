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

package infra.test.web.client.match;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import infra.core.io.ByteArrayResource;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.converter.FormHttpMessageConverter;
import infra.mock.http.client.MockClientHttpRequest;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ContentRequestMatchersAdditionalTests {

  private final ContentRequestMatchers matchers = new ContentRequestMatchers();

  @Test
  void compatibleContentTypeRequiresHeaderAndCompatibleMediaType() throws Exception {
    MockClientHttpRequest request = new MockClientHttpRequest();
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.contentType("text/plain").match(request));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.contentTypeCompatibleWith("text/plain").match(request));
    request.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
    matchers.contentTypeCompatibleWith("text/*").match(request);
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.contentTypeCompatibleWith(MediaType.APPLICATION_JSON).match(request));
  }

  @Test
  void xmlAndJsonMatchersReportContentAndParseFailures() throws Exception {
    MockClientHttpRequest request = new MockClientHttpRequest();
    request.getBody().write("<root><value>one</value></root>".getBytes(StandardCharsets.UTF_8));
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> matchers.xml("<root><value>two</value></root>").match(request));

    MockClientHttpRequest malformed = new MockClientHttpRequest();
    malformed.getBody().write("not-json".getBytes(StandardCharsets.UTF_8));
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> matchers.json("{}").match(malformed))
            .withMessageContaining("Failed to parse");
  }

  @Test
  void multipartMatchesResourcesBinaryEntitiesAndSubset() throws Exception {
    byte[] bytes = { 1, 2, 3 };
    ByteArrayResource resource = new ByteArrayResource(bytes) {
      @Override
      public String getName() {
        return "sample.bin";
      }
    };
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("name", "你好");
    parts.add("file", resource);
    MockClientHttpRequest request = multipart(parts);
    matchers.multipartData(parts).match(request);
    matchers.multipartData(parts, StandardCharsets.ISO_8859_1).match(request);
    matchers.multipartDataContains(Map.of("name", "你好")).match(request);
    MultiValueMap<String, Object> expected = new LinkedMultiValueMap<>();
    expected.add("name", "你好");
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    expected.add("file", new HttpEntity<>(bytes, headers));
    matchers.multipartData(expected).match(request);
    expected.set("file", bytes);
    matchers.multipartData(expected).match(request);
  }

  @Test
  void multipartRejectsWrongCountMissingPartsAndUnsupportedExpectedValues() throws Exception {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("name", "one");
    parts.add("name", "two");
    MockClientHttpRequest request = multipart(parts);
    matchers.multipartDataContains(Map.of("name", "one")).match(request);
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.multipartDataContains(Map.of("missing", "value")).match(request));
    MultiValueMap<String, Object> expected = new LinkedMultiValueMap<>();
    expected.add("name", "one");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.multipartData(expected).match(request));
    expected.add("name", "two");
    expected.add("name", "three");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.multipartData(expected).match(request));
    assertThatIllegalArgumentException().isThrownBy(() -> matchers.multipartDataContains(Map.of("name", 42)).match(request))
            .withMessageContaining("Unexpected multipart value");
  }

  @Test
  void formDataFailuresDistinguishMissingPartsAndDifferentValueCounts() throws Exception {
    var request = new MockClientHttpRequest();
    request.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    request.getBody().write("name=one&name=two".getBytes(StandardCharsets.UTF_8));
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> matchers.formDataContains(Map.of("missing", "value")).match(request));
    var expected = new LinkedMultiValueMap<String, String>();
    expected.add("name", "one");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.formData(expected).match(request));
    expected.add("name", "two");
    expected.add("name", "three");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> matchers.formData(expected).match(request));
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> matchers.formDataContains(Map.of("name", "different")).match(request));
  }

  @Test
  void multipartRejectsNullExpectationsAndBinaryExpectationsForTextParts() throws Exception {
    var parts = new LinkedMultiValueMap<String, Object>();
    parts.add("name", "one");
    var request = multipart(parts);
    var expected = new LinkedMultiValueMap<String, Object>();
    expected.add("name", null);
    assertThatIllegalArgumentException().isThrownBy(() -> matchers.multipartData(expected).match(request))
            .withMessageContaining("Unexpected multipart value: null");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> matchers.multipartDataContains(Map.of("name",
                    new HttpEntity<>(new byte[] { 1 }, HttpHeaders.forWritable()))).match(request))
            .withMessageContaining("not an HttpEntity");
  }

  private MockClientHttpRequest multipart(MultiValueMap<String, Object> parts) throws Exception {
    MockClientHttpRequest request = new MockClientHttpRequest();
    new FormHttpMessageConverter().write(parts, MediaType.MULTIPART_FORM_DATA, request);
    return request;
  }

}
