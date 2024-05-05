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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;

import cn.taketoday.beans.FatalBeanException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.mock.web.MockContextImpl;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.mock.support.XmlWebApplicationContext;
import cn.taketoday.web.view.PathPatternsTestUtils;

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
    root.setConfigLocations("/cn/taketoday/web/handler/map1.xml");
    root.refresh();

    XmlWebApplicationContext wac = new XmlWebApplicationContext();
    wac.setParent(root);
    wac.setMockContext(sc);
    wac.setNamespace("map2err");
    wac.setConfigLocations("/cn/taketoday/web/handler/map2err.xml");
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
    wac.setConfigLocations("/cn/taketoday/web/handler/map2.xml");
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
        interceptor.beforeProcess(context, chain.getRawHandler());
      }
    }

    HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
    request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, matchingMetadata.getPathWithinMapping().value());
    request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, matchingMetadata.getHandler());
    return chain;
  }

}
