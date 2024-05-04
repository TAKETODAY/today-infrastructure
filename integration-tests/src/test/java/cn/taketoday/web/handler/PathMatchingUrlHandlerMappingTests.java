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

package cn.taketoday.web.handler;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.servlet.ConfigurableWebApplicationContext;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.WebApplicationContext;
import cn.taketoday.web.servlet.support.XmlWebApplicationContext;
import cn.taketoday.web.view.PathPatternsParameterizedTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class PathMatchingUrlHandlerMappingTests {
  static final String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = "PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE";

  @SuppressWarnings("unused")
  static Stream<?> pathPatternsArguments() {
    String location = "/cn/taketoday/web/handler/map3.xml";
    WebApplicationContext wac = initConfig(location);

    SimpleUrlHandlerMapping mapping1 = wac.getBean("urlMapping1", SimpleUrlHandlerMapping.class);
    assertThat(mapping1.getPathPatternHandlerMap()).isNotEmpty();

    SimpleUrlHandlerMapping mapping2 = wac.getBean("urlMapping2", SimpleUrlHandlerMapping.class);
//    assertThat(mapping2.getPathPatternHandlerMap()).isEmpty();

    return Stream.of(Arguments.of(mapping1, wac), Arguments.of(mapping2, wac));
  }

  private static WebApplicationContext initConfig(String... configLocations) {
    MockServletContext sc = new MockServletContext("");
    ConfigurableWebApplicationContext context = new XmlWebApplicationContext();
    context.setServletContext(sc);
    context.setConfigLocations(configLocations);
    context.refresh();
    return context;
  }

  @PathPatternsParameterizedTest
  void requestsWithHandlers(HandlerMapping mapping, WebApplicationContext wac) throws Throwable {
    Object bean = wac.getBean("mainController");

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/welcome.html");
    HandlerExecutionChain hec = getHandler(mapping, wac, req);
    assertThat(hec.getRawHandler()).isSameAs(bean);

    req = new MockHttpServletRequest("GET", "/show.html");
    hec = getHandler(mapping, wac, req);
    assertThat(hec.getRawHandler()).isSameAs(bean);

    req = new MockHttpServletRequest("GET", "/bookseats.html");
    hec = getHandler(mapping, wac, req);
    assertThat(hec.getRawHandler()).isSameAs(bean);
  }

  @PathPatternsParameterizedTest
  void actualPathMatching(SimpleUrlHandlerMapping mapping, WebApplicationContext wac) throws Throwable {
    // there a couple of mappings defined with which we can test the
    // path matching, let's do that...

    Object bean = wac.getBean("mainController");
    Object defaultBean = wac.getBean("starController");

    // testing some normal behavior
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/pathmatchingTest.html");
    HandlerExecutionChain chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
            .isEqualTo("/pathmatchingTest.html");

    // no match, no forward slash included
    request = new MockHttpServletRequest("GET", "welcome.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
            .isEqualTo("welcome.html");

    // testing some ????? behavior
    request = new MockHttpServletRequest("GET", "/pathmatchingAA.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
            .isEqualTo("pathmatchingAA.html");

    // testing some ????? behavior
    request = new MockHttpServletRequest("GET", "/pathmatchingA.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
            .isEqualTo("/pathmatchingA.html");

    // testing some ????? behavior
    request = new MockHttpServletRequest("GET", "/administrator/pathmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    // testing simple /**/behavior
    request = new MockHttpServletRequest("GET", "/administrator/test/pathmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    // this should not match because of the administratorT
    request = new MockHttpServletRequest("GET", "/administratort/pathmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    // this should match because of *.jsp
    request = new MockHttpServletRequest("GET", "/bla.jsp");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    // should match because exact pattern is there
    request = new MockHttpServletRequest("GET", "/administrator/another/bla.xml");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    // should not match, because there's not .gif extension in there
    request = new MockHttpServletRequest("GET", "/administrator/another/bla.gif");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    // should match because there testlast* in there
    request = new MockHttpServletRequest("GET", "/administrator/test/testlastbit");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    // but this not, because it's testlast and not testla
    request = new MockHttpServletRequest("GET", "/administrator/test/testla");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    if (mapping.getPatternParser() != null) {
      request = new MockHttpServletRequest("GET", "/administrator/testing/longer/bla");
      chain = getHandler(mapping, wac, request);
      assertThat(chain.getRawHandler()).isSameAs(bean);

      request = new MockHttpServletRequest("GET", "/administrator/testing/longer/test.jsp");
      chain = getHandler(mapping, wac, request);
      assertThat(chain.getRawHandler()).isSameAs(bean);
    }

    request = new MockHttpServletRequest("GET", "/administrator/testing/longer2/notmatching/notmatching");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new MockHttpServletRequest("GET", "/shortpattern/testing/toolong");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new MockHttpServletRequest("GET", "/XXpathXXmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new MockHttpServletRequest("GET", "/pathXXmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new MockHttpServletRequest("GET", "/XpathXXmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new MockHttpServletRequest("GET", "/XXpathmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new MockHttpServletRequest("GET", "/show12.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new MockHttpServletRequest("GET", "/show123.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new MockHttpServletRequest("GET", "/show1.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new MockHttpServletRequest("GET", "/reallyGood-test-is-this.jpeg");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new MockHttpServletRequest("GET", "/reallyGood-tst-is-this.jpeg");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new MockHttpServletRequest("GET", "/testing/test.jpeg");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new MockHttpServletRequest("GET", "/testing/test.jpg");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new MockHttpServletRequest("GET", "/anotherTest");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new MockHttpServletRequest("GET", "/stillAnotherTest");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    // there outofpattern*yeah in the pattern, so this should fail
    request = new MockHttpServletRequest("GET", "/outofpattern*ye");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new MockHttpServletRequest("GET", "/test't%20est/path'm%20atching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new MockHttpServletRequest("GET", "/test%26t%20est/path%26m%20atching.html");
    chain = getHandler(mapping, wac, request);
    if (!mapping.getPathPatternHandlerMap().isEmpty()) {
      assertThat(chain.getRawHandler())
              .as("PathPattern always matches to encoded paths.")
              .isSameAs(bean);
    }
    else {
      assertThat(chain.getRawHandler())
              .as("PathMatcher should not match encoded pattern with urlDecode=true")
              .isSameAs(defaultBean);
    }
  }

  @PathPatternsParameterizedTest
  void defaultMapping(HandlerMapping mapping, WebApplicationContext wac) throws Throwable {
    Object bean = wac.getBean("starController");
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/goggog.html");
    HandlerExecutionChain hec = getHandler(mapping, wac, req);
    assertThat(hec.getRawHandler()).isSameAs(bean);
  }

  @PathPatternsParameterizedTest
  void mappingExposedInRequest(HandlerMapping mapping, WebApplicationContext wac) throws Throwable {
    Object bean = wac.getBean("mainController");
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/show.html");
    HandlerExecutionChain hec = getHandler(mapping, wac, req);
    assertThat(hec.getRawHandler()).isSameAs(bean);
    assertThat(req.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
            .as("Mapping not exposed").isEqualTo("show.html");
  }

  private HandlerExecutionChain getHandler(
          HandlerMapping mapping, WebApplicationContext wac, MockHttpServletRequest request)
          throws Throwable {

    ServletRequestContext context = new ServletRequestContext(wac, request, new MockHttpServletResponse());
    HandlerExecutionChain chain = (HandlerExecutionChain) mapping.getHandler(context);

    HandlerInterceptor[] interceptors = chain.getInterceptors();
    if (interceptors != null) {
      for (HandlerInterceptor interceptor : interceptors) {
        interceptor.beforeProcess(context, chain.getRawHandler());
      }
    }

    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, matchingMetadata.getPathWithinMapping().value());
    return chain;
  }

}
