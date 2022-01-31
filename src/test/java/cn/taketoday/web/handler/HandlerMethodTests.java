/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.handler;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.util.MediaType;
import cn.taketoday.web.MockRequestContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.Produce;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.ParameterResolvingRegistryResolvableParameterFactory;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.servlet.StandardWebServletApplicationContext;
import cn.taketoday.web.view.ReturnValueHandlers;
import cn.taketoday.web.view.template.DefaultTemplateRenderer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/4/29 22:04
 * @since 3.0
 */
public class HandlerMethodTests {

  public void method(String name) {

  }

  @Produce(MediaType.APPLICATION_JSON_VALUE)
  public void produce(String name) {

  }

  @ResponseStatus(HttpStatus.CREATED)
  public void responseStatus() {

  }

  static final class HandlerMethodRequestContext extends MockRequestContext {
    final Map<String, String[]> params;

    HandlerMethodRequestContext(Map<String, String[]> params) {
      this.params = params;
    }

    @Override
    public Map<String, String[]> doGetParameters() {
      return params;
    }
  }

  @Test
  public void testResponseStatus() throws Throwable {
    final Method method = HandlerMethodTests.class.getDeclaredMethod("responseStatus");
    final HandlerMethod handlerMethod = HandlerMethod.from(method);
    final HandlerMethodRequestContext context = new HandlerMethodRequestContext(null);

    final StandardApplicationContext applicationContext = getApplicationContext();
    setResultHandlers(handlerMethod, applicationContext);
  }

  @Test
  public void testSimple() throws Throwable {
    final Method method = HandlerMethodTests.class.getDeclaredMethod("method", String.class);
    final HandlerMethod handlerMethod = HandlerMethod.from(method);

    assertThat(handlerMethod.getMethod()).isEqualTo(method);
    assertThat(handlerMethod.getParameters()).isNull();
    assertThat(handlerMethod.getReturnType()).isEqualTo(method.getReturnType()).isEqualTo(void.class);
    assertThat(handlerMethod.getContentType()).isNull();

    // produce
    final Method produce = HandlerMethodTests.class.getDeclaredMethod("produce", String.class);
    final HandlerMethod produceMethod = HandlerMethod.from(produce);
    assertThat(produceMethod.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

    final StandardApplicationContext applicationContext = getApplicationContext();

    final ParameterResolvingRegistryResolvableParameterFactory methodParameterBuilder
            = new ParameterResolvingRegistryResolvableParameterFactory();
    final ParameterResolvingRegistry resolversRegistry = methodParameterBuilder.getResolvingRegistry();
    resolversRegistry.setApplicationContext(applicationContext);

    resolversRegistry.registerDefaultParameterResolvers();
    final ResolvableMethodParameter[] parameters = methodParameterBuilder.createArray(produce);
    produceMethod.setParameters(parameters);

    setResultHandlers(produceMethod, applicationContext);

    assertThat(produceMethod).isNotEqualTo(handlerMethod);
    assertThat(produceMethod).hasToString("HandlerMethodTests#produce(String name)");
  }

  private StandardApplicationContext getApplicationContext() {
    final StandardApplicationContext applicationContext = new StandardWebServletApplicationContext();
    applicationContext.scan("cn.taketoday.web.handler");
    applicationContext.refresh();
    return applicationContext;
  }

  private void setResultHandlers(HandlerMethod produceMethod, StandardApplicationContext applicationContext) {
    final ReturnValueHandlers resultHandlers = new ReturnValueHandlers();
    final DefaultTemplateRenderer viewResolver = new DefaultTemplateRenderer();
    resultHandlers.setApplicationContext(applicationContext);
    resultHandlers.registerDefaultHandlers(viewResolver);
  }

  static class HandlerInterceptor0 implements HandlerInterceptor {

    final List<String> testList;

    HandlerInterceptor0(List<String> testList) {
      this.testList = testList;
    }

    @Override
    public boolean beforeProcess(RequestContext context, Object handler) throws Throwable {
      testList.add(context.getContentType());
      return true;
    }

    @Override
    public void afterProcess(RequestContext context, Object handler, Object result) throws Throwable {

    }
  }

  //
  @ResponseBody
  static class TestController {

    @ResponseBody
    public void method(String name) { }

    @ResponseBody(value = false)
    public void responseBodyFalse(String name) { }

    public void classLevelResponseBodyTrue(String name) { }

  }

  @Test
  void isResponseBody() throws Exception {
    Method method = TestController.class.getDeclaredMethod("method", String.class);
    HandlerMethod handlerMethod = HandlerMethod.from(method);

    assertThat(handlerMethod.isResponseBody()).isTrue();

    Method responseBodyFalseMethod = TestController.class.getDeclaredMethod("responseBodyFalse", String.class);
    HandlerMethod responseBodyFalse = HandlerMethod.from(responseBodyFalseMethod);
    assertThat(responseBodyFalse.isResponseBody()).isFalse();

    Method classLevelResponseBodyTrueMethod = TestController.class.getDeclaredMethod("classLevelResponseBodyTrue", String.class);
    HandlerMethod classLevelResponseBodyTrue = HandlerMethod.from(classLevelResponseBodyTrueMethod);
    assertThat(classLevelResponseBodyTrue.isResponseBody()).isTrue();

  }

}
