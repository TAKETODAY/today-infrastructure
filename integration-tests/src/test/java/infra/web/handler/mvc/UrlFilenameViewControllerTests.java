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

package infra.web.handler.mvc;

import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.stream.Stream;

import infra.web.handler.mvc.UrlFilenameViewController;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.RedirectModel;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.StaticWebApplicationContext;
import infra.web.view.ModelAndView;
import infra.web.view.PathPatternsParameterizedTest;
import infra.web.view.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 17:08
 */
class UrlFilenameViewControllerTests {

  @SuppressWarnings("unused")
  private static Stream<Function<String, MockRequestContext>> pathPatternsArguments() {
    return PathPatternsTestUtils.requestArguments();
  }

  @PathPatternsParameterizedTest
  void withPlainFilename(Function<String, RequestContext> requestFactory) throws Throwable {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    RequestContext request = requestFactory.apply("/index");
    ModelAndView mv = (ModelAndView) controller.handleRequest(request);
    assertThat(mv.getViewName()).isEqualTo("index");
    assertThat(mv.getModel().isEmpty()).isTrue();
  }

  @PathPatternsParameterizedTest
  void withFilenamePlusExtension(Function<String, RequestContext> requestFactory) throws Throwable {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    RequestContext request = requestFactory.apply("/index.html");
    ModelAndView mv = getModelAndView(controller, request);
    assertThat(mv.getViewName()).isEqualTo("index");
    assertThat(mv.getModel().isEmpty()).isTrue();
  }

  private static ModelAndView getModelAndView(UrlFilenameViewController controller, RequestContext request) throws Throwable {
    return (ModelAndView) controller.handleRequest(request);
  }

  @PathPatternsParameterizedTest
  void withFilenameAndMatrixVariables(Function<String, RequestContext> requestFactory) throws Throwable {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    RequestContext request = requestFactory.apply("/index;a=A;b=B");
    ModelAndView mv = getModelAndView(controller, request);
    assertThat(mv.getViewName()).isEqualTo("index");
    assertThat(mv.getModel().isEmpty()).isTrue();
  }

  @PathPatternsParameterizedTest
  void withPrefixAndSuffix(Function<String, RequestContext> requestFactory) throws Throwable {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    controller.setPrefix("mypre_");
    controller.setSuffix("_mysuf");
    RequestContext request = requestFactory.apply("/index.html");
    ModelAndView mv = getModelAndView(controller, request);
    assertThat(mv.getViewName()).isEqualTo("mypre_index_mysuf");
    assertThat(mv.getModel().isEmpty()).isTrue();
  }

  @PathPatternsParameterizedTest
  void withPrefix(Function<String, RequestContext> requestFactory) throws Throwable {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    controller.setPrefix("mypre_");
    RequestContext request = requestFactory.apply("/index.html");
    ModelAndView mv = getModelAndView(controller, request);
    assertThat(mv.getViewName()).isEqualTo("mypre_index");
    assertThat(mv.getModel().isEmpty()).isTrue();
  }

  @PathPatternsParameterizedTest
  void withSuffix(Function<String, RequestContext> requestFactory) throws Throwable {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    controller.setSuffix("_mysuf");
    RequestContext request = requestFactory.apply("/index.html");
    ModelAndView mv = getModelAndView(controller, request);
    assertThat(mv.getViewName()).isEqualTo("index_mysuf");
    assertThat(mv.getModel().isEmpty()).isTrue();
  }

  @PathPatternsParameterizedTest
  void multiLevel(Function<String, RequestContext> requestFactory) throws Throwable {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    RequestContext request = requestFactory.apply("/docs/cvs/commit.html");
    ModelAndView mv = getModelAndView(controller, request);
    assertThat(mv.getViewName()).isEqualTo("docs/cvs/commit");
    assertThat(mv.getModel().isEmpty()).isTrue();
  }

  @PathPatternsParameterizedTest
  void multiLevelWithMapping(Function<String, RequestContext> requestFactory) throws Throwable {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    RequestContext request = requestFactory.apply("/cvs/commit.html");
    ModelAndView mv = getModelAndView(controller, request);
    assertThat(mv.getViewName()).isEqualTo("cvs/commit");
    assertThat(mv.getModel().isEmpty()).isTrue();
  }

  @PathPatternsParameterizedTest
  void multiLevelMappingWithFallback(Function<String, RequestContext> requestFactory) throws Throwable {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    RequestContext request = requestFactory.apply("/docs/cvs/commit.html");
    exposePathInMapping(request, "/docs/cvs/commit.html");
    ModelAndView mv = getModelAndView(controller, request);
    assertThat(mv.getViewName()).isEqualTo("docs/cvs/commit");
    assertThat(mv.getModel().isEmpty()).isTrue();
  }

  @Test
  void withContextMapping() throws Throwable {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/docs/cvs/commit.html");
    StaticWebApplicationContext wac = new StaticWebApplicationContext();
    wac.refresh();
    MockRequestContext context = new MockRequestContext(wac, request, new MockHttpResponseImpl());
    ModelAndView mv = getModelAndView(controller, context);
    assertThat(mv.getViewName()).isEqualTo("docs/cvs/commit");
    assertThat(mv.getModel().isEmpty()).isTrue();
  }

  @Test
  void settingPrefixToNullCausesEmptyStringToBeUsed() {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    controller.setPrefix(null);
    assertThat(controller.getPrefix())
            .as("For setPrefix(..) with null, the empty string must be used instead.")
            .isNotNull();
    assertThat(controller.getPrefix())
            .as("For setPrefix(..) with null, the empty string must be used instead.")
            .isEqualTo("");
  }

  @Test
  void settingSuffixToNullCausesEmptyStringToBeUsed() {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    controller.setSuffix(null);
    assertThat(controller.getSuffix())
            .as("For setPrefix(..) with null, the empty string must be used instead.")
            .isNotNull();
    assertThat(controller.getSuffix())
            .as("For setPrefix(..) with null, the empty string must be used instead.")
            .isEqualTo("");
  }

  /**
   * This is the expected behavior, and it now has a test to prove it.
   * https://opensource.atlassian.com/projects/spring/browse/SPR-2789
   */
  @PathPatternsParameterizedTest
  void nestedPathisUsedAsViewName_InBreakingChangeFromSpring12Line(
          Function<String, RequestContext> requestFactory) throws Throwable {

    UrlFilenameViewController controller = new UrlFilenameViewController();
    RequestContext request = requestFactory.apply("/products/view.html");
    ModelAndView mv = getModelAndView(controller, request);
    assertThat(mv.getViewName()).isEqualTo("products/view");
    assertThat(mv.getModel().isEmpty()).isTrue();
  }

  @PathPatternsParameterizedTest
  void withFlashAttributes(Function<String, RequestContext> requestFactory) throws Throwable {
    UrlFilenameViewController controller = new UrlFilenameViewController();
    RequestContext request = requestFactory.apply("/index");
    request.setAttribute(RedirectModel.INPUT_ATTRIBUTE, new RedirectModel("name", "value"));
    ModelAndView mv = getModelAndView(controller, request);
    assertThat(mv.getViewName()).isEqualTo("index");
    assertThat(mv.getModel().size()).isEqualTo(1);
    assertThat(mv.getModel().get("name")).isEqualTo("value");
  }

  private void exposePathInMapping(RequestContext request, String mapping) {

  }

}
