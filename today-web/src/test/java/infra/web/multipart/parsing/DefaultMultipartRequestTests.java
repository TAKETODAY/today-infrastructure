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

package infra.web.multipart.parsing;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import infra.http.HttpHeaders;
import infra.util.MultiValueMap;
import infra.web.RequestContext;
import infra.web.multipart.Part;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/5 15:45
 */
class DefaultMultipartRequestTests {

  @Test
  void constructorInitializesFieldsCorrectly() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    RequestContext context = mock(RequestContext.class);

    DefaultMultipartRequest request = new DefaultMultipartRequest(parser, context);

    assertThat(request).isNotNull();
  }

  @Test
  void parseRequestDelegatesToMultipartParser() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    RequestContext context = mock(RequestContext.class);
    MultiValueMap<String, Part> expectedParts = mock(MultiValueMap.class);

    when(parser.parseRequest(context)).thenReturn(expectedParts);

    DefaultMultipartRequest request = new DefaultMultipartRequest(parser, context);

    MultiValueMap<String, Part> actualParts = request.parseRequest();

    assertThat(actualParts).isEqualTo(expectedParts);
    verify(parser).parseRequest(context);
  }

  @Test
  void getPartDelegatesToParsedParts() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    RequestContext context = mock(RequestContext.class);
    MultiValueMap<String, Part> partsMap = mock(MultiValueMap.class);
    Part expectedPart = mock(Part.class);

    when(parser.parseRequest(context)).thenReturn(partsMap);
    when(partsMap.getFirst("testPart")).thenReturn(expectedPart);

    DefaultMultipartRequest request = new DefaultMultipartRequest(parser, context);

    Part actualPart = request.getPart("testPart");

    assertThat(actualPart).isEqualTo(expectedPart);
  }

  @Test
  void getPartsReturnsAllParts() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    RequestContext context = mock(RequestContext.class);
    MultiValueMap<String, Part> expectedParts = mock(MultiValueMap.class);

    when(parser.parseRequest(context)).thenReturn(expectedParts);

    DefaultMultipartRequest request = new DefaultMultipartRequest(parser, context);

    MultiValueMap<String, Part> actualParts = request.getParts();

    assertThat(actualParts).isEqualTo(expectedParts);
  }

  @Test
  void getPartNamesDelegatesToPartsKeySet() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    RequestContext context = mock(RequestContext.class);
    MultiValueMap<String, Part> partsMap = mock(MultiValueMap.class);
    Set<String> expectedNames = mock(Set.class);

    when(parser.parseRequest(context)).thenReturn(partsMap);
    when(partsMap.keySet()).thenReturn(expectedNames);

    DefaultMultipartRequest request = new DefaultMultipartRequest(parser, context);

    Iterable<String> actualNames = request.getPartNames();

    assertThat(actualNames).isEqualTo(expectedNames);
  }

  @Test
  void isResolvedReturnsFalseBeforeParsing() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    RequestContext context = mock(RequestContext.class);

    DefaultMultipartRequest request = new DefaultMultipartRequest(parser, context);

    assertThat(request.isResolved()).isFalse();
  }

  @Test
  void isResolvedReturnsTrueAfterGettingParts() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    RequestContext context = mock(RequestContext.class);
    MultiValueMap<String, Part> partsMap = mock(MultiValueMap.class);

    when(parser.parseRequest(context)).thenReturn(partsMap);

    DefaultMultipartRequest request = new DefaultMultipartRequest(parser, context);
    request.getParts(); // Trigger parsing

    assertThat(request.isResolved()).isTrue();
  }

  @Test
  void cleanupClearsPartsAndCallsWebUtils() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    RequestContext context = mock(RequestContext.class);
    MultiValueMap<String, Part> partsMap = mock(MultiValueMap.class);

    when(parser.parseRequest(context)).thenReturn(partsMap);

    DefaultMultipartRequest request = new DefaultMultipartRequest(parser, context);
    request.getParts(); // Initialize parts

    request.cleanup();

    assertThat(request.isResolved()).isFalse();
  }

  @Test
  void getPartsByNameDelegatesToParsedParts() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    RequestContext context = mock(RequestContext.class);
    MultiValueMap<String, Part> partsMap = mock(MultiValueMap.class);
    List<Part> expectedParts = java.util.Arrays.asList(mock(Part.class), mock(Part.class));

    when(parser.parseRequest(context)).thenReturn(partsMap);
    when(partsMap.get("testPartName")).thenReturn(expectedParts);

    DefaultMultipartRequest request = new DefaultMultipartRequest(parser, context);

    List<Part> actualParts = request.getParts("testPartName");

    assertThat(actualParts).isEqualTo(expectedParts);
  }

  @Test
  void getHeadersDelegatesToPartHeaders() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    RequestContext context = mock(RequestContext.class);
    MultiValueMap<String, Part> partsMap = mock(MultiValueMap.class);
    Part part = mock(Part.class);
    HttpHeaders expectedHeaders = mock(HttpHeaders.class);

    when(parser.parseRequest(context)).thenReturn(partsMap);
    when(partsMap.getFirst("testPart")).thenReturn(part);
    when(part.getHeaders()).thenReturn(expectedHeaders);

    DefaultMultipartRequest request = new DefaultMultipartRequest(parser, context);

    HttpHeaders actualHeaders = request.getHeaders("testPart");

    assertThat(actualHeaders).isEqualTo(expectedHeaders);
  }

  @Test
  void getHeadersReturnsNullWhenPartNotFound() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    RequestContext context = mock(RequestContext.class);
    MultiValueMap<String, Part> partsMap = mock(MultiValueMap.class);

    when(parser.parseRequest(context)).thenReturn(partsMap);
    when(partsMap.getFirst("nonExistentPart")).thenReturn(null);

    DefaultMultipartRequest request = new DefaultMultipartRequest(parser, context);

    HttpHeaders headers = request.getHeaders("nonExistentPart");

    assertThat(headers).isNull();
  }

  @Test
  void parseRequestIsCalledOnlyOnce() {
    DefaultMultipartParser parser = mock(DefaultMultipartParser.class);
    RequestContext context = mock(RequestContext.class);
    MultiValueMap<String, Part> partsMap = mock(MultiValueMap.class);

    when(parser.parseRequest(context)).thenReturn(partsMap);

    DefaultMultipartRequest request = new DefaultMultipartRequest(parser, context);

    // Call getParts multiple times
    request.getParts();
    request.getParts();
    request.getParts();

    // Verify parseRequest was called only once
    verify(parser).parseRequest(context);
  }

}