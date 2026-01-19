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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;

import infra.beans.FatalBeanException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.context.support.StaticApplicationContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.HandlerInterceptor;
import infra.web.HandlerMapping;
import infra.web.HandlerMatchingMetadata;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.XmlWebApplicationContext;
import infra.web.view.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class SimpleUrlHandlerMappingTests {
  static final String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = "PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE";
  static final String BEST_MATCHING_HANDLER_ATTRIBUTE = "BEST_MATCHING_HANDLER_ATTRIBUTE";

  @Test
  @SuppressWarnings("resource")
  public void handlerBeanNotFound() {
    MockContextImpl sc = new MockContextImpl("");
    XmlWebApplicationContext root = new XmlWebApplicationContext();
    root.setMockContext(sc);
    root.setConfigLocations("/infra/web/handler/map1.xml");
    root.refresh();

    XmlWebApplicationContext wac = new XmlWebApplicationContext();
    wac.setParent(root);
    wac.setMockContext(sc);
    wac.setNamespace("map2err");
    wac.setConfigLocations("/infra/web/handler/map2err.xml");
    assertThatExceptionOfType(FatalBeanException.class)
            .isThrownBy(wac::refresh)
            .withCauseInstanceOf(NoSuchBeanDefinitionException.class)
            .satisfies(ex -> {
              NoSuchBeanDefinitionException cause = (NoSuchBeanDefinitionException) ex.getCause();
              assertThat(cause.getBeanName()).isEqualTo("mainControlle");
            });
  }

  @Test
  public void testNewlineInRequest() throws Exception {
    Object controller = new Object();
    SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping(Collections.singletonMap("/*/baz", controller));
    mapping.setApplicationContext(new StaticApplicationContext());

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/foo%0a%0dbar/baz");

    HandlerExecutionChain hec = (HandlerExecutionChain) mapping.getHandler(new MockRequestContext(
            null, request, new MockHttpResponseImpl()));
    assertThat(hec).isNotNull();
    assertThat(hec.getRawHandler()).isSameAs(controller);
  }

  @ParameterizedTest
  @ValueSource(strings = { "urlMapping", "urlMappingWithProps", "urlMappingWithPathPatterns" })
  void checkMappings(String beanName) throws Throwable {
    MockContextImpl sc = new MockContextImpl("");
    XmlWebApplicationContext wac = new XmlWebApplicationContext();
    wac.setMockContext(sc);
    wac.setConfigLocations("/infra/web/handler/map2.xml");
    wac.refresh();
    Object bean = wac.getBean("mainController");
    Object otherBean = wac.getBean("otherController");
    Object defaultBean = wac.getBean("starController");
    HandlerMapping hm = (HandlerMapping) wac.getBean(beanName);
    wac.close();

    HttpMockRequestImpl request = PathPatternsTestUtils.initRequest("GET", "/welcome.html");
    HandlerExecutionChain chain = getHandler(hm, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/welcome.html");
    assertThat(request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(bean);

    request = PathPatternsTestUtils.initRequest("GET", "/welcome.xxx");
    chain = getHandler(hm, request);
    assertThat(chain.getRawHandler()).isSameAs(otherBean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("welcome.xxx");
    assertThat(request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(otherBean);

    request = PathPatternsTestUtils.initRequest("GET", "/welcome.x");
    chain = getHandler(hm, request);
    assertThat(chain.getRawHandler()).isSameAs(otherBean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("welcome.x");
    assertThat(request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(otherBean);

    request = PathPatternsTestUtils.initRequest("GET", "/welcome.html");
    chain = getHandler(hm, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = PathPatternsTestUtils.initRequest("GET", "/welcome.html");
    chain = getHandler(hm, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = PathPatternsTestUtils.initRequest("GET", "/show.html");
    chain = getHandler(hm, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = PathPatternsTestUtils.initRequest("GET", "/bookseats.html");
    chain = getHandler(hm, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = PathPatternsTestUtils.initRequest("GET", "/welcome.html");
    chain = getHandler(hm, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = PathPatternsTestUtils.initRequest("GET", "/show.html");
    chain = getHandler(hm, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = PathPatternsTestUtils.initRequest("GET", "/bookseats.html");

    chain = getHandler(hm, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);

    request = PathPatternsTestUtils.initRequest("GET", "/");
    chain = getHandler(hm, request);
    assertThat(chain.getRawHandler()).isSameAs(bean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/");

    request = PathPatternsTestUtils.initRequest("GET", "/somePath");
    chain = getHandler(hm, request);
    assertThat(chain.getRawHandler()).as("Handler is correct bean").isSameAs(defaultBean);
    assertThat(request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).isEqualTo("/somePath");
  }

  private HandlerExecutionChain getHandler(HandlerMapping mapping, HttpMockRequestImpl request) throws Throwable {

    MockRequestContext context = new MockRequestContext(
            null, request, new MockHttpResponseImpl());
    HandlerExecutionChain chain = (HandlerExecutionChain) mapping.getHandler(context);
    HandlerInterceptor[] interceptors = chain.getInterceptors();
    if (interceptors != null) {
      for (HandlerInterceptor interceptor : interceptors) {
        interceptor.preProcessing(context, chain.getRawHandler());
      }
    }

    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, matchingMetadata.getPathWithinMapping().value());
    request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, matchingMetadata.getHandler());
    return chain;
  }

}
