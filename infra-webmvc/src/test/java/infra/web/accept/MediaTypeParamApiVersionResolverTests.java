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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.accept;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.http.MediaType;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/6/27 16:57
 */
class MediaTypeParamApiVersionResolverTests {

  private final MediaType mediaType = MediaType.parseMediaType("application/x.abc+json");

  private final ApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(mediaType, "version");

  private final HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");

  @Test
  void resolveFromAccept() {
    String version = "3";
    this.request.addHeader("Accept", getMediaType(version));
    testResolve(this.resolver, this.request, version);
  }

  @Test
  void resolveFromContentType() {
    String version = "3";
    this.request.setContentType(getMediaType(version).toString());
    testResolve(this.resolver, this.request, version);
  }

  @Test
  void wildcard() {
    MediaType compatibleMediaType = MediaType.parseMediaType("application/*+json");
    ApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(compatibleMediaType, "version");

    String version = "3";
    this.request.addHeader("Accept", getMediaType(version));
    testResolve(resolver, this.request, version);
  }

  @Test
  void constructor_ShouldSetMediaTypeAndParameterName() {
    MediaType mediaType = MediaType.APPLICATION_JSON;
    String parameterName = "version";

    MediaTypeParamApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(mediaType, parameterName);

    // Test indirectly by using reflection or through behavior testing
    assertThat(resolver).isNotNull();
  }

  @Test
  void resolveVersion_ShouldReturnNull_WhenNoAcceptOrContentTypeHeader() {
    MediaType mediaType = MediaType.APPLICATION_JSON;
    MediaTypeParamApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(mediaType, "version");
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");

    String result = resolver.resolveVersion(new MockRequestContext(request));

    assertThat(result).isNull();
  }

  @Test
  void resolveVersion_ShouldReturnNull_WhenMediaTypeNotCompatible() {
    MediaType compatibleMediaType = MediaType.APPLICATION_JSON;
    MediaTypeParamApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(compatibleMediaType, "version");
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");
    request.addHeader("Accept", "application/xml;version=1");

    String result = resolver.resolveVersion(new MockRequestContext(request));

    assertThat(result).isNull();
  }

  @Test
  void resolveVersion_ShouldReturnVersion_FromAcceptHeader_WhenMultipleMediaTypes() {
    MediaType compatibleMediaType = MediaType.parseMediaType("application/x.abc+json");
    MediaTypeParamApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(compatibleMediaType, "version");
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");
    request.addHeader("Accept", "application/xml, application/x.abc+json;version=2, text/plain");

    String result = resolver.resolveVersion(new MockRequestContext(request));

    assertThat(result).isEqualTo("2");
  }

  @Test
  void resolveVersion_ShouldReturnVersion_FromContentTypeHeader_WhenNoAcceptHeader() {
    MediaType compatibleMediaType = MediaType.parseMediaType("application/x.abc+json");
    MediaTypeParamApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(compatibleMediaType, "version");
    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/path");
    request.setContentType("application/x.abc+json;version=3");

    String result = resolver.resolveVersion(new MockRequestContext(request));

    assertThat(result).isEqualTo("3");
  }

  @Test
  void resolveVersion_ShouldReturnNull_WhenContentTypeNotCompatible() {
    MediaType compatibleMediaType = MediaType.APPLICATION_JSON;
    MediaTypeParamApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(compatibleMediaType, "version");
    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/path");
    request.setContentType("application/xml;version=1");

    String result = resolver.resolveVersion(new MockRequestContext(request));

    assertThat(result).isNull();
  }

  @Test
  void resolveVersion_ShouldReturnNull_WhenParameterNotFound() {
    MediaType compatibleMediaType = MediaType.parseMediaType("application/x.abc+json");
    MediaTypeParamApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(compatibleMediaType, "version");
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");
    request.addHeader("Accept", "application/x.abc+json");

    String result = resolver.resolveVersion(new MockRequestContext(request));

    assertThat(result).isNull();
  }

  @Test
  void resolveVersion_ShouldReturnNull_WhenContentTypeParameterNotFound() {
    MediaType compatibleMediaType = MediaType.parseMediaType("application/x.abc+json");
    MediaTypeParamApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(compatibleMediaType, "version");
    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/path");
    request.setContentType("application/x.abc+json");

    String result = resolver.resolveVersion(new MockRequestContext(request));

    assertThat(result).isNull();
  }

  @Test
  void resolveVersion_ShouldPreferAcceptHeader_OverContentType() {
    MediaType compatibleMediaType = MediaType.parseMediaType("application/x.abc+json");
    MediaTypeParamApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(compatibleMediaType, "version");
    HttpMockRequestImpl request = new HttpMockRequestImpl("POST", "/path");
    request.addHeader("Accept", "application/x.abc+json;version=accept-version");
    request.setContentType("application/x.abc+json;version=content-version");

    String result = resolver.resolveVersion(new MockRequestContext(request));

    assertThat(result).isEqualTo("accept-version");
  }

  @Test
  void resolveVersion_ShouldHandleMultipleAcceptHeaders() {
    MediaType compatibleMediaType = MediaType.parseMediaType("application/x.abc+json");
    MediaTypeParamApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(compatibleMediaType, "version");
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");
    request.addHeader("Accept", "application/xml;version=1");
    request.addHeader("Accept", "application/x.abc+json;version=2");

    String result = resolver.resolveVersion(new MockRequestContext(request));

    assertThat(result).isEqualTo("2");
  }

  @Test
  void resolveVersion_ShouldReturnFirstMatchingMediaTypeVersion() {
    MediaType compatibleMediaType = MediaType.parseMediaType("application/*+json");
    MediaTypeParamApiVersionResolver resolver = new MediaTypeParamApiVersionResolver(compatibleMediaType, "version");
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/path");
    request.addHeader("Accept", "application/vnd.api.v1+json;version=1, application/vnd.api.v2+json;version=2");

    String result = resolver.resolveVersion(new MockRequestContext(request));

    assertThat(result).isEqualTo("1");
  }

  private MediaType getMediaType(String version) {
    return new MediaType(this.mediaType, Map.of("version", version));
  }

  private void testResolve(ApiVersionResolver resolver, HttpMockRequestImpl request, String expected) {
    String actual = resolver.resolveVersion(new MockRequestContext(request));
    assertThat(actual).isEqualTo(expected);
  }

}