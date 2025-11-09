/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
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