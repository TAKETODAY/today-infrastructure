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

package infra.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.core.MethodParameter;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.RequestContext;
import infra.web.bind.resolver.UriComponentsBuilderParameterStrategy;
import infra.web.mock.MockRequestContext;
import infra.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 */
public class UriComponentsBuilderMethodArgumentResolverTests {

  private UriComponentsBuilderParameterStrategy resolver;

  private RequestContext webRequest;

  private HttpMockRequestImpl mockRequest;

  private ResolvableMethodParameter builderParam;
  private ResolvableMethodParameter mockBuilderParam;
  private ResolvableMethodParameter intParam;

  @BeforeEach
  public void setup() throws Exception {
    this.resolver = new UriComponentsBuilderParameterStrategy();
    this.mockRequest = new HttpMockRequestImpl();
    this.webRequest = new MockRequestContext(
            null, this.mockRequest, new MockHttpResponseImpl());

    Method method = this.getClass().getDeclaredMethod(
            "handle", UriComponentsBuilder.class, UriComponentsBuilder.class, int.class);
    this.builderParam = new ResolvableMethodParameter(new MethodParameter(method, 0));
    this.mockBuilderParam = new ResolvableMethodParameter(new MethodParameter(method, 1));
    this.intParam = new ResolvableMethodParameter(new MethodParameter(method, 2));
  }

  @Test
  public void supportsParameter() throws Exception {
    assertThat(this.resolver.supportsParameter(this.builderParam)).isTrue();
    assertThat(this.resolver.supportsParameter(this.mockBuilderParam)).isTrue();
    assertThat(this.resolver.supportsParameter(this.intParam)).isFalse();
  }

  @Test
  public void resolveArgument() throws Throwable {
    this.mockRequest.setPathInfo("/accounts");

    Object actual = this.resolver.resolveArgument(webRequest, builderParam);

    assertThat(actual).isNotNull();
    assertThat(actual.getClass()).isEqualTo(UriComponentsBuilder.class);
    assertThat(((UriComponentsBuilder) actual).build().toUriString()).isEqualTo("http://localhost");
  }

  void handle(UriComponentsBuilder builder, UriComponentsBuilder servletBuilder, int value) {
  }

}
