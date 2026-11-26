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
import infra.web.ServerHttpExchange;
import infra.web.filter.ForwardedHeaderFilter;
import infra.web.handler.method.support.UriComponentsBuilderParameterStrategy;
import infra.web.mock.MockServerHttpExchange;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.util.HttpExchangeUriComponentsBuilder;
import infra.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 */
public class UriComponentsBuilderMethodArgumentResolverTests {

  private UriComponentsBuilderParameterStrategy resolver;

  private ServerHttpExchange webRequest;

  private MockRequest mockRequest;

  private HandlerParameter builderParam;
  private HandlerParameter mockBuilderParam;
  private HandlerParameter intParam;

  @BeforeEach
  public void setup() throws Exception {
    this.resolver = new UriComponentsBuilderParameterStrategy();
    this.mockRequest = new MockRequest();
    this.webRequest = new MockServerHttpExchange(
            null, this.mockRequest, new MockResponse());

    Method method = this.getClass().getDeclaredMethod(
            "handle", UriComponentsBuilder.class, HttpExchangeUriComponentsBuilder.class, int.class);
    this.builderParam = new HandlerParameter(new MethodParameter(method, 0));
    this.mockBuilderParam = new HandlerParameter(new MethodParameter(method, 1));
    this.intParam = new HandlerParameter(new MethodParameter(method, 2));
  }

  @Test
  public void supportsParameter() throws Exception {
    assertThat(this.resolver.supportsParameter(this.builderParam)).isTrue();
    assertThat(this.resolver.supportsParameter(this.mockBuilderParam)).isTrue();
    assertThat(this.resolver.supportsParameter(this.intParam)).isFalse();
  }

  @Test
  public void resolveArgument() throws Throwable {
    Object actual = this.resolver.resolveArgument(webRequest, builderParam);

    assertThat(actual).isNotNull();
    assertThat(actual.getClass()).isEqualTo(HttpExchangeUriComponentsBuilder.class);
    assertThat(((UriComponentsBuilder) actual).build().toUriString()).isEqualTo("http://localhost");
  }

  void handle(UriComponentsBuilder builder, HttpExchangeUriComponentsBuilder contextBuilder, int value) {
  }

  @Test
  void resolveContextBuilderWithPathAndQuery() {
    this.mockRequest.setScheme("https");
    this.mockRequest.setServerPort(443);
    this.mockRequest.setRequestURI("/items/a%2Fb.json");
    this.mockRequest.setQueryString("q=a%2Bb");

    HttpExchangeUriComponentsBuilder builder = this.resolver.resolveArgument(this.webRequest, this.mockBuilderParam);

    assertThat(builder.build().toUriString()).isEqualTo("https://localhost/items/a%2Fb.json?q=a%2Bb");
    assertThat(builder.removePathExtension()).isEqualTo("json");
  }

  @Test
  void resolveArgumentAfterForwardedHeaderFilter() throws Exception {
    this.mockRequest.setRequestURI("/items/a%2Fb");
    this.mockRequest.setQueryString("q=a%2Bb");
    this.mockRequest.addHeader("Forwarded", "proto=https;host=example.org");
    this.mockRequest.addHeader("X-Forwarded-Prefix", "/edge");
    ForwardedHeaderFilter filter = new ForwardedHeaderFilter(true);
    filter.setUseForwardedPrefix(true);
    filter.doFilter(this.webRequest, context -> {
      UriComponentsBuilder builder = (UriComponentsBuilder) this.resolver.resolveArgument(context, this.builderParam);
      assertThat(builder.build().toUriString()).isEqualTo("https://example.org/edge/items/a%2Fb?q=a%2Bb");
    });
  }

}
