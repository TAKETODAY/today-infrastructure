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

package infra.web;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import infra.core.DefaultAttributeAccessor;
import infra.http.MediaType;
import infra.http.server.PathContainer;
import infra.lang.NullValue;
import infra.util.MultiValueMap;
import infra.web.mock.MockServerHttpExchange;
import infra.web.util.pattern.PathMatchInfo;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/10 23:54
 */
class HandlerMatchingMetadataTests {
  @Test
  void matchingMetadataUsesOnlyTheRequestAttributeContract() {
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    var attributes = new DefaultAttributeAccessor();
    given(request.getAttribute(anyString())).willAnswer(invocation -> attributes.getAttribute(invocation.getArgument(0)));
    doAnswer(invocation -> {
      attributes.setAttribute(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(request).setAttribute(anyString(), any());
    var wrapper = new DelegatingServerHttpExchange(new DelegatingServerHttpExchange(request));
    assertThat(HandlerMatchingMetadata.has(wrapper)).isFalse();
    assertThat(HandlerMatchingMetadata.find(wrapper)).isNull();
    assertThatIllegalStateException().isThrownBy(() -> HandlerMatchingMetadata.obtain(wrapper))
            .withMessage("HandlerMatchingMetadata is required");
    assertThat(attributes.getAttributeNames()).isEmpty();

    var original = new HandlerMatchingMetadata(createExchange());
    HandlerMatchingMetadata.set(request, original);
    assertThat(HandlerMatchingMetadata.has(wrapper)).isTrue();
    assertThat(HandlerMatchingMetadata.obtain(wrapper)).isSameAs(original);

    var replacement = new HandlerMatchingMetadata(createExchange());
    HandlerMatchingMetadata.set(wrapper, replacement);
    assertThat(HandlerMatchingMetadata.obtain(request)).isSameAs(replacement);

    HandlerMatchingMetadata.set(wrapper, null);
    assertThat(HandlerMatchingMetadata.find(request)).isNull();
  }

  @Test
  void constructorWithExchange() {
    ServerHttpExchange request = createExchange();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    assertThat(metadata.getHandler()).isEqualTo(NullValue.INSTANCE);
    assertThat(metadata.getDirectLookupPath()).isEqualTo("/test");
    assertThat(metadata.getLookupPath().value()).isEqualTo("/test");
    assertThat(metadata.getPathVariables()).isEmpty();
    assertThat(metadata.getUriVariables()).isEmpty();
    assertThat(metadata.getMatrixVariables()).isEmpty();
    assertThat(metadata.getProducibleMediaTypes()).isNull();
  }

  @Test
  void constructorWithHandlerAndHttpExchange() {
    Object handler = new Object();
    MockServerHttpExchange request = createExchange();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(handler, request);

    assertThat(metadata.getHandler()).isSameAs(handler);
    assertThat(metadata.getDirectLookupPath()).isEqualTo("/test");
    assertThat(metadata.getLookupPath().value()).isEqualTo("/test");
  }

  private static MockServerHttpExchange createExchange() {
    MockServerHttpExchange mock = new MockServerHttpExchange();
    mock.setRequestURI("/test");
    return mock;
  }

  @Test
  void constructorWithAllParameters() {
    Object handler = new Object();
    String directLookupPath = "/direct";
    PathContainer lookupPath = PathContainer.parsePath("/lookup");
    PathPattern bestMatchingPattern = PathPatternParser.defaultInstance.parse("/pattern");
    PathPatternParser patternParser = new PathPatternParser();

    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(handler, directLookupPath, lookupPath, bestMatchingPattern, patternParser);

    assertThat(metadata.getHandler()).isSameAs(handler);
    assertThat(metadata.getDirectLookupPath()).isEqualTo(directLookupPath);
    assertThat(metadata.getLookupPath()).isSameAs(lookupPath);
    assertThat(metadata.getBestMatchingPattern()).isSameAs(bestMatchingPattern);
    assertThat(metadata.getPatternParser()).isSameAs(patternParser);
  }

  @Test
  void copyConstructor() {
    Object handler = new Object();
    String directLookupPath = "/direct";
    PathContainer lookupPath = PathContainer.parsePath("/lookup");
    PathPattern bestMatchingPattern = PathPatternParser.defaultInstance.parse("/pattern");
    PathPatternParser patternParser = new PathPatternParser();
    Collection<MediaType> producibleMediaTypes = List.of(MediaType.APPLICATION_JSON);

    HandlerMatchingMetadata original = new HandlerMatchingMetadata(handler, directLookupPath, lookupPath, bestMatchingPattern, patternParser);
    original.setProducibleMediaTypes(producibleMediaTypes);

    HandlerMatchingMetadata copy = new HandlerMatchingMetadata(original);

    assertThat(copy.getHandler()).isSameAs(handler);
    assertThat(copy.getDirectLookupPath()).isEqualTo(directLookupPath);
    assertThat(copy.getLookupPath()).isSameAs(lookupPath);
    assertThat(copy.getBestMatchingPattern()).isSameAs(bestMatchingPattern);
    assertThat(copy.getPatternParser()).isSameAs(patternParser);
    assertThat(copy.getProducibleMediaTypes()).isEqualTo(producibleMediaTypes);
  }

  @Test
  void getBestMatchingPatternParsesPatternIfNeeded() {
    MockServerHttpExchange request = createExchange();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    PathPattern pattern = metadata.getBestMatchingPattern();
    assertThat(pattern).isNotNull();
    assertThat(pattern.getPatternString()).isEqualTo("/test");
  }

  @Test
  void getPathWithinMappingReturnsFullLookupPathForExactMatch() {
    MockServerHttpExchange request = createExchange();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    PathContainer pathWithinMapping = metadata.getPathWithinMapping();
    assertThat(pathWithinMapping.value()).isEqualTo("/test");
  }

  @Test
  void getPathWithinMappingReturnsSubPathForPatternMatch() {
    PathPatternParser parser = new PathPatternParser();
    PathPattern pattern = parser.parse("/test/**");
    PathContainer lookupPath = PathContainer.parsePath("/test/sub/path");

    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(new Object(), "/test/**", lookupPath, pattern, parser);

    PathContainer pathWithinMapping = metadata.getPathWithinMapping();
    assertThat(pathWithinMapping.value()).isEqualTo("sub/path");
  }

  @Test
  void getPathMatchInfoExtractsVariablesFromPattern() {
    PathPatternParser parser = new PathPatternParser();
    PathPattern pattern = parser.parse("/test/{id}");
    PathContainer lookupPath = PathContainer.parsePath("/test/123");

    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(new Object(), "/test/{id}", lookupPath, pattern, parser);

    PathMatchInfo matchInfo = metadata.getPathMatchInfo();
    assertThat(matchInfo.getUriVariables()).containsEntry("id", "123");
  }

  @Test
  void getPathVariablesReturnsEmptyMapInitially() {
    MockServerHttpExchange request = createExchange();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    Map<String, Object> pathVariables = metadata.getPathVariables();
    assertThat(pathVariables).isEmpty();
  }

  @Test
  void hasPathVariablesReturnsFalseInitially() {
    MockServerHttpExchange request = createExchange();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    assertThat(metadata.hasPathVariables()).isFalse();
  }

  @Test
  void getUriVariablesReturnsEmptyMapInitially() {
    MockServerHttpExchange request = createExchange();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    Map<String, String> uriVariables = metadata.getUriVariables();
    assertThat(uriVariables).isEmpty();
  }

  @Test
  void getUriVariableReturnsNullForNonExistentVariable() {
    MockServerHttpExchange request = createExchange();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    String value = metadata.getUriVariable("nonexistent");
    assertThat(value).isNull();
  }

  @Test
  void getMatrixVariablesReturnsEmptyMapInitially() {
    MockServerHttpExchange request = createExchange();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    Map<String, MultiValueMap<String, String>> matrixVariables = metadata.getMatrixVariables();
    assertThat(matrixVariables).isEmpty();
  }

  @Test
  void getMatrixVariableReturnsNullForNonExistentMatrixVariable() {
    MockServerHttpExchange request = createExchange();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    MultiValueMap<String, String> matrixVariable = metadata.getMatrixVariable("nonexistent");
    assertThat(matrixVariable).isNull();
  }

  @Test
  void setAndGetProducibleMediaTypes() {
    MockServerHttpExchange request = createExchange();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);
    Collection<MediaType> mediaTypes = List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);

    metadata.setProducibleMediaTypes(mediaTypes);
    assertThat(metadata.getProducibleMediaTypes()).containsExactlyElementsOf(mediaTypes);
  }

  @Test
  void setHandlerUpdatesHandlerReference() {
    MockServerHttpExchange request = createExchange();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);
    Object newHandler = new Object();

    metadata.setHandler(newHandler);
    assertThat(metadata.getHandler()).isSameAs(newHandler);
  }

}
