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

package infra.web.view;

import java.util.function.Function;
import java.util.stream.Stream;

import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

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
  private static Stream<Function<String, MockRequestContext>> pathPatternsArguments() {
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
