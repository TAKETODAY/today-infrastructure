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

package infra.web.config.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.core.Ordered;
import infra.http.server.RequestPath;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.HandlerInterceptor;
import infra.web.InterceptorChain;
import infra.web.RequestContext;
import infra.web.handler.MappedInterceptor;
import infra.web.i18n.LocaleChangeInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/23 22:31
 */
class InterceptorRegistryTests {

  private InterceptorRegistry registry;

  private final HandlerInterceptor interceptor1 = new LocaleChangeInterceptor();

  private final HandlerInterceptor interceptor2 = new HandlerInterceptor() {
    @Override
    public Object intercept(RequestContext request, InterceptorChain chain) throws Throwable {
      return HandlerInterceptor.super.intercept(request, chain);
    }
  };

  private TestWebRequestInterceptor webInterceptor1;

  private TestWebRequestInterceptor webInterceptor2;

  private final HttpMockRequestImpl request = new HttpMockRequestImpl();

  private final MockHttpResponseImpl response = new MockHttpResponseImpl();

  @BeforeEach
  public void setUp() {
    this.registry = new InterceptorRegistry();
    this.webInterceptor1 = new TestWebRequestInterceptor();
    this.webInterceptor2 = new TestWebRequestInterceptor();
  }

  @Test
  public void addInterceptor() {
    this.registry.addInterceptor(this.interceptor1);
    List<HandlerInterceptor> interceptors = getInterceptorsForPath(null);
    assertThat(interceptors).isEqualTo(Arrays.asList(this.interceptor1));
  }

  @Test
  public void addTwoInterceptors() {
    this.registry.addInterceptor(this.interceptor1);
    this.registry.addInterceptor(this.interceptor2);
    List<HandlerInterceptor> interceptors = getInterceptorsForPath(null);
    assertThat(interceptors).isEqualTo(Arrays.asList(this.interceptor1, this.interceptor2));
  }

  @Test
  public void addInterceptorsWithUrlPatterns() {
    this.registry.addInterceptor(this.interceptor1).addPathPatterns("/path1/**").excludePathPatterns("/path1/secret");
    this.registry.addInterceptor(this.interceptor2).addPathPatterns("/path2");

    assertThat(getInterceptorsForPath("/path1/test")).isEqualTo(Arrays.asList(this.interceptor1));
    assertThat(getInterceptorsForPath("/path2")).isEqualTo(Arrays.asList(this.interceptor2));
    assertThat(getInterceptorsForPath("/path1/secret")).isEqualTo(Collections.emptyList());
  }

  @Test
  public void addInterceptorWithExcludePathPatternOnly() {
    this.registry.addInterceptor(this.interceptor1).excludePathPatterns("/path1/secret");
    this.registry.addInterceptor(this.interceptor2).addPathPatterns("/path2");

    assertThat(getInterceptorsForPath("/path1")).isEqualTo(Collections.singletonList(this.interceptor1));
    assertThat(getInterceptorsForPath("/path2")).isEqualTo(Arrays.asList(this.interceptor1, this.interceptor2));
    assertThat(getInterceptorsForPath("/path1/secret")).isEqualTo(Collections.emptyList());
  }

  @Test
  public void orderedInterceptors() {
    this.registry.addInterceptor(this.interceptor1).order(Ordered.LOWEST_PRECEDENCE);
    this.registry.addInterceptor(this.interceptor2).order(Ordered.HIGHEST_PRECEDENCE);

    List<Object> interceptors = this.registry.getInterceptors();
    assertThat(interceptors.size()).isEqualTo(2);

    assertThat(interceptors.get(0)).isSameAs(this.interceptor2);
    assertThat(interceptors.get(1)).isSameAs(this.interceptor1);
  }

  @Test
  public void nonOrderedInterceptors() {
    this.registry.addInterceptor(this.interceptor1).order(0);
    this.registry.addInterceptor(this.interceptor2).order(0);

    List<Object> interceptors = this.registry.getInterceptors();
    assertThat(interceptors.size()).isEqualTo(2);

    assertThat(interceptors.get(0)).isSameAs(this.interceptor1);
    assertThat(interceptors.get(1)).isSameAs(this.interceptor2);
  }

  private List<HandlerInterceptor> getInterceptorsForPath(String lookupPath) {
    List<HandlerInterceptor> result = new ArrayList<>();
    for (Object interceptor : this.registry.getInterceptors()) {
      if (interceptor instanceof MappedInterceptor mappedInterceptor) {
        if (mappedInterceptor.matches(RequestPath.parse(lookupPath, null))) {
          result.add(mappedInterceptor.getInterceptor());
        }
      }
      else if (interceptor instanceof HandlerInterceptor) {
        result.add((HandlerInterceptor) interceptor);
      }
      else {
        fail("Unexpected interceptor type: " + interceptor.getClass().getName());
      }
    }
    return result;
  }

  private static class TestWebRequestInterceptor implements HandlerInterceptor {

    private boolean preHandleInvoked = false;

    @Override
    public boolean preProcessing(RequestContext request, Object handler) throws Throwable {
      preHandleInvoked = true;
      return true;
    }

    @Override
    public void postProcessing(RequestContext request, Object handler, Object result) throws Throwable {

    }

  }

}
