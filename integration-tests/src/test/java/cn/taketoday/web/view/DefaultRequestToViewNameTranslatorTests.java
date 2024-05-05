/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.view;

import java.util.function.Function;
import java.util.stream.Stream;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mock.ServletRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 21:45
 */
class DefaultRequestToViewNameTranslatorTests {

  private static final String VIEW_NAME = "apple";
  private static final String EXTENSION = ".html";

  private final DefaultRequestToViewNameTranslator translator = new DefaultRequestToViewNameTranslator();

  @SuppressWarnings("unused")
  private static Stream<Function<String, ServletRequestContext>> pathPatternsArguments() {
    return PathPatternsTestUtils.requestArguments("");
  }

  @PathPatternsParameterizedTest
  void testGetViewNameLeavesLeadingSlashIfSoConfigured(Function<String, RequestContext> requestFactory) {
    RequestContext request = requestFactory.apply(VIEW_NAME + "/");
    this.translator.setStripLeadingSlash(false);
    assertViewName(request, "/" + VIEW_NAME);
  }

  @PathPatternsParameterizedTest
  void testGetViewNameLeavesTrailingSlashIfSoConfigured(Function<String, RequestContext> requestFactory) {
    RequestContext request = requestFactory.apply(VIEW_NAME + "/");
    this.translator.setStripTrailingSlash(false);
    assertViewName(request, VIEW_NAME + "/");
  }

  @PathPatternsParameterizedTest
  void testGetViewNameLeavesExtensionIfSoConfigured(Function<String, RequestContext> requestFactory) {
    RequestContext request = requestFactory.apply(VIEW_NAME + EXTENSION);
    this.translator.setStripExtension(false);
    assertViewName(request, VIEW_NAME + EXTENSION);
  }

  @PathPatternsParameterizedTest
  void testGetViewNameWithDefaultConfiguration(Function<String, RequestContext> requestFactory) {
    RequestContext request = requestFactory.apply(VIEW_NAME + EXTENSION);
    assertViewName(request, VIEW_NAME);
  }

  @PathPatternsParameterizedTest
  void testGetViewNameWithCustomSeparator(Function<String, RequestContext> requestFactory) {
    RequestContext request = requestFactory.apply(VIEW_NAME + "/fiona" + EXTENSION);
    this.translator.setSeparator("_");
    assertViewName(request, VIEW_NAME + "_fiona");
  }

  @PathPatternsParameterizedTest
  void testGetViewNameWithNoExtension(Function<String, RequestContext> requestFactory) {
    RequestContext request = requestFactory.apply(VIEW_NAME);
    assertViewName(request, VIEW_NAME);
  }

  @PathPatternsParameterizedTest
  void testGetViewNameWithSemicolonContent(Function<String, RequestContext> requestFactory) {
    RequestContext request = requestFactory.apply(VIEW_NAME + ";a=A;b=B");
    assertViewName(request, VIEW_NAME);
  }

  @PathPatternsParameterizedTest
  void testGetViewNameWithPrefix(Function<String, RequestContext> requestFactory) {
    final String prefix = "fiona_";
    RequestContext request = requestFactory.apply(VIEW_NAME);
    this.translator.setPrefix(prefix);
    assertViewName(request, prefix + VIEW_NAME);
  }

  @PathPatternsParameterizedTest
  void testGetViewNameWithNullPrefix(Function<String, RequestContext> requestFactory) {
    RequestContext request = requestFactory.apply(VIEW_NAME);
    this.translator.setPrefix(null);
    assertViewName(request, VIEW_NAME);
  }

  @PathPatternsParameterizedTest
  void testGetViewNameWithSuffix(Function<String, RequestContext> requestFactory) {
    final String suffix = ".fiona";
    RequestContext request = requestFactory.apply(VIEW_NAME);
    this.translator.setSuffix(suffix);
    assertViewName(request, VIEW_NAME + suffix);
  }

  @PathPatternsParameterizedTest
  void testGetViewNameWithNullSuffix(Function<String, RequestContext> requestFactory) {
    RequestContext request = requestFactory.apply(VIEW_NAME);
    this.translator.setSuffix(null);
    assertViewName(request, VIEW_NAME);
  }

  private void assertViewName(RequestContext request, String expectedViewName) {
    String actualViewName = this.translator.getViewName(request);
    assertThat(actualViewName).isNotNull();
    assertThat(actualViewName)
            .as("Did not get the expected viewName from the DefaultRequestToViewNameTranslator.getViewName(..)")
            .isEqualTo(expectedViewName);
  }

}
