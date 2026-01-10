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

package infra.web.handler.mvc;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

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
