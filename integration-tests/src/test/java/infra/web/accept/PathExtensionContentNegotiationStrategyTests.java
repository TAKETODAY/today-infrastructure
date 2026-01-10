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

package infra.web.accept;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.io.ClassPathResource;
import infra.http.MediaType;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/7 14:03
 */
class PathExtensionContentNegotiationStrategyTests {

  private final HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();

  private final RequestContext webRequest = new MockRequestContext(null, mockRequest, null);

  private PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();

  @Test
  void resolveMediaTypesFromMapping() throws Exception {
    this.mockRequest.setRequestURI("test.html");

    List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

    assertThat(mediaTypes).isEqualTo(List.of(new MediaType("text", "html")));

    Map<String, MediaType> mapping = Collections.singletonMap("HTML", MediaType.APPLICATION_XHTML_XML);
    this.strategy = new PathExtensionContentNegotiationStrategy(mapping);
    mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

    assertThat(mediaTypes).isEqualTo(List.of(new MediaType("application", "xhtml+xml")));
  }

  @Test
  void resolveMediaTypesFromMediaTypeFactory() throws Exception {
    this.mockRequest.setRequestURI("test.xls");

    List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

    assertThat(mediaTypes).isEqualTo(List.of(new MediaType("application", "vnd.ms-excel")));
  }

  @Test
  void getMediaTypeFilenameWithContextPath() throws Exception {
    this.mockRequest.setRequestURI("/");
    assertThat(this.strategy.resolveMediaTypes(webRequest)).as("Context path should be excluded").isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);

    this.mockRequest.setRequestURI("/project-1.0.0.M3");
    assertThat(this.strategy.resolveMediaTypes(webRequest)).as("Context path should be excluded").isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
  }

  @Test
  void getMediaTypeFilenameWithEncodedURI() throws Exception {
    this.mockRequest.setRequestURI("/quo%20vadis%3f.html");
    List<MediaType> result = this.strategy.resolveMediaTypes(webRequest);

    assertThat(result).as("Invalid content type").isEqualTo(Collections.singletonList(new MediaType("text", "html")));
  }

  @Test
  void resolveMediaTypesIgnoreUnknownExtension() throws Exception {
    this.mockRequest.setRequestURI("test.foobar");

    List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

    assertThat(mediaTypes).isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
  }

  @Test
  void resolveMediaTypesDoNotIgnoreUnknownExtension() {
    this.mockRequest.setRequestURI("test.foobar");

    this.strategy.setIgnoreUnknownExtensions(false);
    assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
            .isThrownBy(() -> this.strategy.resolveMediaTypes(this.webRequest));
  }

  @Test
  void getMediaTypeForResource() {
    assertThatThrownBy(() -> strategy.getMediaTypeForResource(null))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("Resource is required");

    ContentNegotiationManagerFactoryBeanTests.TestMockContext context = new ContentNegotiationManagerFactoryBeanTests.TestMockContext();
    context.getMimeTypes().put("foo", "application/foo");

    strategy = new PathExtensionContentNegotiationStrategy(null);

    ClassPathResource resource = new ClassPathResource("logback.xml");

    MediaType textXml = strategy.getMediaTypeForResource(resource);
    assertThat(textXml).isEqualTo(MediaType.APPLICATION_XML);

  }

}
