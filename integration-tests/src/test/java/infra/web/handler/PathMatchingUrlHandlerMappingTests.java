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

package infra.web.handler;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.HandlerInterceptor;
import infra.web.HandlerMapping;
import infra.web.HandlerMatchingMetadata;
import infra.web.mock.ConfigurableWebApplicationContext;
import infra.web.mock.MockRequestContext;
import infra.web.mock.WebApplicationContext;
import infra.web.mock.support.XmlWebApplicationContext;
import infra.web.view.PathPatternsParameterizedTest;

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
    String location = "/infra/web/handler/map3.xml";
    WebApplicationContext wac = initConfig(location);

    SimpleUrlHandlerMapping mapping1 = wac.getBean("urlMapping1", SimpleUrlHandlerMapping.class);
    assertThat(mapping1.getPathPatternHandlerMap()).isNotEmpty();

    SimpleUrlHandlerMapping mapping2 = wac.getBean("urlMapping2", SimpleUrlHandlerMapping.class);
//    assertThat(mapping2.getPathPatternHandlerMap()).isEmpty();

    return Stream.of(Arguments.of(mapping1, wac), Arguments.of(mapping2, wac));
  }

  private static WebApplicationContext initConfig(String... configLocations) {
    MockContextImpl sc = new MockContextImpl("");
    ConfigurableWebApplicationContext context = new XmlWebApplicationContext();
    context.setMockContext(sc);
    context.setConfigLocations(configLocations);
    context.refresh();
    return context;
  }

  @PathPatternsParameterizedTest
  void requestsWithHandlers(HandlerMapping mapping, WebApplicationContext wac) throws Throwable {
    Object bean = wac.getBean("mainController");

    HttpMockRequestImpl req = new HttpMockRequestImpl("GET", "/welcome.html");
    HandlerExecutionChain hec = getHandler(mapping, wac, req);
    assertThat(hec.getRawHandler()).isSameAs(bean);

    req = new HttpMockRequestImpl("GET", "/show.html");
    hec = getHandler(mapping, wac, req);
    assertThat(hec.getRawHandler()).isSameAs(bean);

    req = new HttpMockRequestImpl("GET", "/bookseats.html");
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
    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/pathmatchingTest.html");
    HandlerExecutionChain chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
            .isEqualTo("/pathmatchingTest.html");

    // no match, no forward slash included
    request = new HttpMockRequestImpl("GET", "welcome.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
            .isEqualTo("welcome.html");

    // testing some ????? behavior
    request = new HttpMockRequestImpl("GET", "/pathmatchingAA.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
            .isEqualTo("pathmatchingAA.html");

    // testing some ????? behavior
    request = new HttpMockRequestImpl("GET", "/pathmatchingA.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
            .isEqualTo("/pathmatchingA.html");

    // testing some ????? behavior
    request = new HttpMockRequestImpl("GET", "/administrator/pathmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    // testing simple /**/behavior
    request = new HttpMockRequestImpl("GET", "/administrator/test/pathmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    // this should not match because of the administratorT
    request = new HttpMockRequestImpl("GET", "/administratort/pathmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    // this should match because of *.jsp
    request = new HttpMockRequestImpl("GET", "/bla.jsp");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    // should match because exact pattern is there
    request = new HttpMockRequestImpl("GET", "/administrator/another/bla.xml");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    // should not match, because there's not .gif extension in there
    request = new HttpMockRequestImpl("GET", "/administrator/another/bla.gif");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    // should match because there testlast* in there
    request = new HttpMockRequestImpl("GET", "/administrator/test/testlastbit");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    // but this not, because it's testlast and not testla
    request = new HttpMockRequestImpl("GET", "/administrator/test/testla");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    if (mapping.getPatternParser() != null) {
      request = new HttpMockRequestImpl("GET", "/administrator/testing/longer/bla");
      chain = getHandler(mapping, wac, request);
      assertThat(chain.getRawHandler()).isSameAs(bean);

      request = new HttpMockRequestImpl("GET", "/administrator/testing/longer/test.jsp");
      chain = getHandler(mapping, wac, request);
      assertThat(chain.getRawHandler()).isSameAs(bean);
    }

    request = new HttpMockRequestImpl("GET", "/administrator/testing/longer2/notmatching/notmatching");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new HttpMockRequestImpl("GET", "/shortpattern/testing/toolong");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new HttpMockRequestImpl("GET", "/XXpathXXmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new HttpMockRequestImpl("GET", "/pathXXmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new HttpMockRequestImpl("GET", "/XpathXXmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new HttpMockRequestImpl("GET", "/XXpathmatching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new HttpMockRequestImpl("GET", "/show12.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new HttpMockRequestImpl("GET", "/show123.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new HttpMockRequestImpl("GET", "/show1.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new HttpMockRequestImpl("GET", "/reallyGood-test-is-this.jpeg");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new HttpMockRequestImpl("GET", "/reallyGood-tst-is-this.jpeg");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new HttpMockRequestImpl("GET", "/testing/test.jpeg");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new HttpMockRequestImpl("GET", "/testing/test.jpg");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new HttpMockRequestImpl("GET", "/anotherTest");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = new HttpMockRequestImpl("GET", "/stillAnotherTest");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    // there outofpattern*yeah in the pattern, so this should fail
    request = new HttpMockRequestImpl("GET", "/outofpattern*ye");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new HttpMockRequestImpl("GET", "/test't%20est/path'm%20atching.html");
    chain = getHandler(mapping, wac, request);
    assertThat(chain.getRawHandler()).isSameAs(defaultBean);

    request = new HttpMockRequestImpl("GET", "/test%26t%20est/path%26m%20atching.html");
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
    HttpMockRequestImpl req = new HttpMockRequestImpl("GET", "/goggog.html");
    HandlerExecutionChain hec = getHandler(mapping, wac, req);
    assertThat(hec.getRawHandler()).isSameAs(bean);
  }

  @PathPatternsParameterizedTest
  void mappingExposedInRequest(HandlerMapping mapping, WebApplicationContext wac) throws Throwable {
    Object bean = wac.getBean("mainController");
    HttpMockRequestImpl req = new HttpMockRequestImpl("GET", "/show.html");
    HandlerExecutionChain hec = getHandler(mapping, wac, req);
    assertThat(hec.getRawHandler()).isSameAs(bean);
    assertThat(req.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
            .as("Mapping not exposed").isEqualTo("show.html");
  }

  private HandlerExecutionChain getHandler(
          HandlerMapping mapping, WebApplicationContext wac, HttpMockRequestImpl request)
          throws Throwable {

    MockRequestContext context = new MockRequestContext(wac, request, new MockHttpResponseImpl());
    HandlerExecutionChain chain = (HandlerExecutionChain) mapping.getHandler(context);

    HandlerInterceptor[] interceptors = chain.getInterceptors();
    if (interceptors != null) {
      for (HandlerInterceptor interceptor : interceptors) {
        interceptor.preProcessing(context, chain.getRawHandler());
      }
    }

    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, matchingMetadata.getPathWithinMapping().value());
    return chain;
  }

}
