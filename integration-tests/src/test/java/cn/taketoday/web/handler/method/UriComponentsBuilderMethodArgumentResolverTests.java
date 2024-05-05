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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.resolver.UriComponentsBuilderParameterStrategy;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.util.UriComponentsBuilder;

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
