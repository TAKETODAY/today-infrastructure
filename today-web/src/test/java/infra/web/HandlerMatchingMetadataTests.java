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

package infra.web;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import infra.http.MediaType;
import infra.http.server.PathContainer;
import infra.lang.NullValue;
import infra.util.MultiValueMap;
import infra.web.mock.MockRequestContext;
import infra.web.util.pattern.PathMatchInfo;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/10 23:54
 */
class HandlerMatchingMetadataTests {
  @Test
  void constructorWithRequestContext() {
    RequestContext request = createContext();
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
  void constructorWithHandlerAndRequestContext() {
    Object handler = new Object();
    MockRequestContext request = createContext();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(handler, request);

    assertThat(metadata.getHandler()).isSameAs(handler);
    assertThat(metadata.getDirectLookupPath()).isEqualTo("/test");
    assertThat(metadata.getLookupPath().value()).isEqualTo("/test");
  }

  private static MockRequestContext createContext() {
    MockRequestContext mock = new MockRequestContext();
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
    MockRequestContext request = createContext();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    PathPattern pattern = metadata.getBestMatchingPattern();
    assertThat(pattern).isNotNull();
    assertThat(pattern.getPatternString()).isEqualTo("/test");
  }

  @Test
  void getPathWithinMappingReturnsFullLookupPathForExactMatch() {
    MockRequestContext request = createContext();
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
    MockRequestContext request = createContext();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    Map<String, Object> pathVariables = metadata.getPathVariables();
    assertThat(pathVariables).isEmpty();
  }

  @Test
  void hasPathVariablesReturnsFalseInitially() {
    MockRequestContext request = createContext();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    assertThat(metadata.hasPathVariables()).isFalse();
  }

  @Test
  void getUriVariablesReturnsEmptyMapInitially() {
    MockRequestContext request = createContext();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    Map<String, String> uriVariables = metadata.getUriVariables();
    assertThat(uriVariables).isEmpty();
  }

  @Test
  void getUriVariableReturnsNullForNonExistentVariable() {
    MockRequestContext request = createContext();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    String value = metadata.getUriVariable("nonexistent");
    assertThat(value).isNull();
  }

  @Test
  void getMatrixVariablesReturnsEmptyMapInitially() {
    MockRequestContext request = createContext();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    Map<String, MultiValueMap<String, String>> matrixVariables = metadata.getMatrixVariables();
    assertThat(matrixVariables).isEmpty();
  }

  @Test
  void getMatrixVariableReturnsNullForNonExistentMatrixVariable() {
    MockRequestContext request = createContext();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);

    MultiValueMap<String, String> matrixVariable = metadata.getMatrixVariable("nonexistent");
    assertThat(matrixVariable).isNull();
  }

  @Test
  void setAndGetProducibleMediaTypes() {
    MockRequestContext request = createContext();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);
    Collection<MediaType> mediaTypes = List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);

    metadata.setProducibleMediaTypes(mediaTypes);
    assertThat(metadata.getProducibleMediaTypes()).containsExactlyElementsOf(mediaTypes);
  }

  @Test
  void setHandlerUpdatesHandlerReference() {
    MockRequestContext request = createContext();
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(request);
    Object newHandler = new Object();

    metadata.setHandler(newHandler);
    assertThat(metadata.getHandler()).isSameAs(newHandler);
  }

}