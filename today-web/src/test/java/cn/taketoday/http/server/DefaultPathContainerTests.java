/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.http.server;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.server.PathContainer.Element;
import cn.taketoday.http.server.PathContainer.Options;
import cn.taketoday.http.server.PathContainer.PathSegment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link DefaultPathContainer}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class DefaultPathContainerTests {

  @Test
  void pathSegment() {
    // basic
    testPathSegment("cars", "cars", emptyMap());

    // empty
    testPathSegment("", "", emptyMap());

    // spaces
    testPathSegment("%20%20", "  ", emptyMap());
    testPathSegment("%20a%20", " a ", emptyMap());
  }

  @Test
  void pathSegmentParams() {
    // basic
    DefaultMultiValueMap<String, String> params = emptyMap();
    params.add("colors", "red");
    params.add("colors", "blue");
    params.add("colors", "green");
    params.add("year", "2012");
    testPathSegment("cars;colors=red,blue,green;year=2012", "cars", params);

    // trailing semicolon
    params = emptyMap();
    params.add("p", "1");
    testPathSegment("path;p=1;", "path", params);

    // params with spaces
    params = emptyMap();
    params.add("param name", "param value");
    testPathSegment("path;param%20name=param%20value;%20", "path", params);

    // empty params
    params = emptyMap();
    params.add("p", "1");
    testPathSegment("path;;;%20;%20;p=1;%20", "path", params);
  }

  @Test
  void pathSegmentParamsAreImmutable() {
    assertPathSegmentParamsAreImmutable("cars", emptyMap(), Options.HTTP_PATH);

    DefaultMultiValueMap<String, String> params = emptyMap();
    params.add("colors", "red");
    params.add("colors", "blue");
    params.add("colors", "green");
    assertPathSegmentParamsAreImmutable(";colors=red,blue,green", params, Options.HTTP_PATH);

    assertPathSegmentParamsAreImmutable(";colors=red,blue,green", emptyMap(), Options.MESSAGE_ROUTE);
  }

  private void assertPathSegmentParamsAreImmutable(String path, DefaultMultiValueMap<String, String> params, Options options) {
    PathContainer container = PathContainer.parsePath(path, options);
    assertThat(container.elements()).hasSize(1);

    PathSegment segment = (PathSegment) container.elements().get(0);
    MultiValueMap<String, String> segmentParams = segment.parameters();
    assertThat(segmentParams).isEqualTo(params);
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> segmentParams.add("enigma", "boom"));
  }

  private void testPathSegment(String rawValue, String valueToMatch, MultiValueMap<String, String> params) {
    PathContainer container = PathContainer.parsePath(rawValue);

    if ("".equals(rawValue)) {
      assertThat(container.elements()).isEmpty();
      return;
    }

    assertThat(container.elements()).hasSize(1);
    PathSegment segment = (PathSegment) container.elements().get(0);

    assertThat(segment.value()).as("value: '" + rawValue + "'").isEqualTo(rawValue);
    assertThat(segment.valueToMatch()).as("valueToMatch: '" + rawValue + "'").isEqualTo(valueToMatch);
    assertThat(segment.parameters()).as("params: '" + rawValue + "'").isEqualTo(params);
  }

  @Test
  void path() {
    // basic
    testPath("/a/b/c", "/a/b/c", "/", "a", "/", "b", "/", "c");

    // root path
    testPath("/", "/", "/");

    // empty path
    testPath("", "");
    testPath("%20%20", "%20%20", "%20%20");

    // trailing slash
    testPath("/a/b/", "/a/b/", "/", "a", "/", "b", "/");
    testPath("/a/b//", "/a/b//", "/", "a", "/", "b", "/", "/");

    // extra slashes and spaces
    testPath("/%20", "/%20", "/", "%20");
    testPath("//%20/%20", "//%20/%20", "/", "/", "%20", "/", "%20");
  }

  private void testPath(String input, String value, String... expectedElements) {
    PathContainer path = PathContainer.parsePath(input, Options.HTTP_PATH);

    assertThat(path.value()).as("value: '" + input + "'").isEqualTo(value);
    assertThat(path.elements()).map(Element::value).as("elements: " + input)
            .containsExactly(expectedElements);
  }

  @Test
  void subPath() {
    // basic
    PathContainer path = PathContainer.parsePath("/a/b/c");
    assertThat(path.subPath(0)).isSameAs(path);
    assertThat(path.subPath(2).value()).isEqualTo("/b/c");
    assertThat(path.subPath(4).value()).isEqualTo("/c");

    // root path
    path = PathContainer.parsePath("/");
    assertThat(path.subPath(0).value()).isEqualTo("/");

    // trailing slash
    path = PathContainer.parsePath("/a/b/");
    assertThat(path.subPath(2).value()).isEqualTo("/b/");
  }

  @Test
    // gh-23310
  void pathWithCustomSeparator() {
    PathContainer path = PathContainer.parsePath("a.b%2Eb.c", Options.MESSAGE_ROUTE);

    Stream<String> decodedSegments = path.elements().stream()
            .filter(PathSegment.class::isInstance)
            .map(PathSegment.class::cast)
            .map(PathSegment::valueToMatch);

    assertThat(decodedSegments).containsExactly("a", "b.b", "c");
  }

  private static DefaultMultiValueMap<String, String> emptyMap() {
    return new DefaultMultiValueMap<>();
  }

}
