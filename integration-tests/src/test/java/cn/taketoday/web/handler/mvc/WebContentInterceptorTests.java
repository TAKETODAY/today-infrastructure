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

package cn.taketoday.web.handler.mvc;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 16:21
 */
class WebContentInterceptorTests {

  private final MockHttpResponseImpl response = new MockHttpResponseImpl();

  private final HttpMockRequestImpl mockRequest = new HttpMockRequestImpl();

  private final WebContentInterceptor interceptor = new WebContentInterceptor();

  private final Object handler = new Object();
  RequestContext context = new MockRequestContext(null, mockRequest, response);

  Function<String, RequestContext> requestFactory = path -> {
    HttpMockRequestImpl servletRequest = new HttpMockRequestImpl("GET", path);
    return new MockRequestContext(null, servletRequest, response);
  };

  @Test
  void cacheResourcesConfiguration() throws Exception {
    interceptor.setCacheSeconds(10);
    interceptor.beforeProcess(context, handler);

    context.flush();
    Iterable<String> cacheControlHeaders = response.getHeaders("Cache-Control");
    assertThat(cacheControlHeaders).contains("max-age=10");
  }

  @Test
  void mappedCacheConfigurationOverridesGlobal() throws Exception {
    Properties mappings = new Properties();
    mappings.setProperty("/*/*handle.vm", "-1");

    interceptor.setCacheSeconds(10);
    interceptor.setCacheMappings(mappings);

    RequestContext request = requestFactory.apply("/example/adminhandle.vm");
    interceptor.beforeProcess(request, handler);

    List<String> cacheControlHeaders = response.getHeaders("Cache-Control");
    assertThat(cacheControlHeaders).isEmpty();

    request = requestFactory.apply("/example/bingo.html");
    interceptor.beforeProcess(request, handler);

    request.requestCompleted();

    cacheControlHeaders = response.getHeaders("Cache-Control");
    assertThat(cacheControlHeaders).contains("max-age=10");
  }

  @Test
  void preventCacheConfiguration() throws Exception {
    interceptor.setCacheSeconds(0);
    RequestContext requestContext = requestFactory.apply("/");
    interceptor.beforeProcess(requestContext, handler);
    requestContext.requestCompleted();
    Iterable<String> cacheControlHeaders = response.getHeaders("Cache-Control");
    assertThat(cacheControlHeaders).contains("no-store");
  }

  @Test
  void emptyCacheConfiguration() throws Exception {
    interceptor.setCacheSeconds(-1);
    interceptor.beforeProcess(requestFactory.apply("/"), handler);

    Iterable<String> expiresHeaders = response.getHeaders("Expires");
    assertThat(expiresHeaders).isEmpty();
    Iterable<String> cacheControlHeaders = response.getHeaders("Cache-Control");
    assertThat(cacheControlHeaders).isEmpty();
  }

}
