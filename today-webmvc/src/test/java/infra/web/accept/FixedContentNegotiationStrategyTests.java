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

import java.util.ArrayList;
import java.util.List;

import infra.http.MediaType;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 15:23
 */
class FixedContentNegotiationStrategyTests {

  @Test
  void constructorWithSingleMediaType() {
    MediaType contentType = MediaType.APPLICATION_JSON;
    FixedContentNegotiationStrategy strategy = new FixedContentNegotiationStrategy(contentType);

    List<MediaType> contentTypes = strategy.getContentTypes();
    assertThat(contentTypes).containsExactly(contentType);
  }

  @Test
  void constructorWithMultipleMediaTypes() {
    List<MediaType> contentTypes = List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_XML, MediaType.ALL);
    FixedContentNegotiationStrategy strategy = new FixedContentNegotiationStrategy(contentTypes);

    List<MediaType> result = strategy.getContentTypes();
    assertThat(result).containsExactlyElementsOf(contentTypes);
  }

  @Test
  void constructorWithNullMediaTypesThrowsException() {
    assertThatThrownBy(() -> new FixedContentNegotiationStrategy((List<MediaType>) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'contentTypes' is required");
  }

  @Test
  void getContentTypesReturnsUnmodifiableList() {
    List<MediaType> originalContentTypes = new ArrayList<>(List.of(MediaType.APPLICATION_JSON));
    FixedContentNegotiationStrategy strategy = new FixedContentNegotiationStrategy(originalContentTypes);

    List<MediaType> result = strategy.getContentTypes();

    assertThatThrownBy(() -> result.add(MediaType.TEXT_PLAIN))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void resolveMediaTypesReturnsConfiguredContentTypes() {
    List<MediaType> contentTypes = List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_XML);
    FixedContentNegotiationStrategy strategy = new FixedContentNegotiationStrategy(contentTypes);

    RequestContext request = new MockRequestContext();
    List<MediaType> result = strategy.resolveMediaTypes(request);

    assertThat(result).containsExactlyElementsOf(contentTypes);
  }

  @Test
  void resolveMediaTypesReturnsSameInstance() {
    List<MediaType> contentTypes = List.of(MediaType.APPLICATION_JSON);
    FixedContentNegotiationStrategy strategy = new FixedContentNegotiationStrategy(contentTypes);

    RequestContext request = new MockRequestContext();
    List<MediaType> result1 = strategy.resolveMediaTypes(request);
    List<MediaType> result2 = strategy.resolveMediaTypes(request);

    assertThat(result1).isSameAs(result2);
  }

}