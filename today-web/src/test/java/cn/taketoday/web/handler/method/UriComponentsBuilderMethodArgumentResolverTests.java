/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.resolver.UriComponentsBuilderParameterStrategy;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.support.ServletUriComponentsBuilder;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 */
public class UriComponentsBuilderMethodArgumentResolverTests {

  private UriComponentsBuilderParameterStrategy resolver;

  private RequestContext webRequest;

  private MockHttpServletRequest servletRequest;

  private ResolvableMethodParameter builderParam;
  private ResolvableMethodParameter servletBuilderParam;
  private ResolvableMethodParameter intParam;

  @BeforeEach
  public void setup() throws Exception {
    this.resolver = new UriComponentsBuilderParameterStrategy();
    this.servletRequest = new MockHttpServletRequest();
    this.webRequest = new ServletRequestContext(
            null, this.servletRequest, new MockHttpServletResponse());

    Method method = this.getClass().getDeclaredMethod(
            "handle", UriComponentsBuilder.class, ServletUriComponentsBuilder.class, int.class);
    this.builderParam = new ResolvableMethodParameter(new MethodParameter(method, 0));
    this.servletBuilderParam = new ResolvableMethodParameter(new MethodParameter(method, 1));
    this.intParam = new ResolvableMethodParameter(new MethodParameter(method, 2));
  }

  @Test
  public void supportsParameter() throws Exception {
    assertThat(this.resolver.supportsParameter(this.builderParam)).isTrue();
    assertThat(this.resolver.supportsParameter(this.servletBuilderParam)).isTrue();
    assertThat(this.resolver.supportsParameter(this.intParam)).isFalse();
  }

  @Test
  public void resolveArgument() throws Throwable {
    this.servletRequest.setContextPath("/myapp");
    this.servletRequest.setServletPath("/main");
    this.servletRequest.setPathInfo("/accounts");

    Object actual = this.resolver.resolveArgument(webRequest, builderParam);

    assertThat(actual).isNotNull();
    assertThat(actual.getClass()).isEqualTo(ServletUriComponentsBuilder.class);
    assertThat(((ServletUriComponentsBuilder) actual).build().toUriString()).isEqualTo("http://localhost/myapp/main");
  }

  void handle(UriComponentsBuilder builder, ServletUriComponentsBuilder servletBuilder, int value) {
  }

}
