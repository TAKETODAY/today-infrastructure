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

package infra.web.bind.resolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.Principal;

import infra.core.MethodParameter;
import infra.mock.api.MockRequest;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.MockRequestContext;
import infra.web.mock.bind.resolver.PrincipalMethodArgumentResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/4/8 13:25
 */
class PrincipalMethodArgumentResolverTests {

  private final PrincipalMethodArgumentResolver resolver = new PrincipalMethodArgumentResolver();

  private final HttpMockRequestImpl mockRequest = new HttpMockRequestImpl("GET", "");

  private final MockRequestContext webRequest = new MockRequestContext(
          null, mockRequest, new MockHttpResponseImpl());

  private Method method;

  @BeforeEach
  void setup() throws Exception {
    method = getClass().getMethod("supportedParams", MockRequest.class, Principal.class);
  }

  @Test
  void principal() throws Throwable {
    Principal principal = () -> "Foo";
    mockRequest.setUserPrincipal(principal);

    MethodParameter principalParameter = new MethodParameter(method, 1);

    ResolvableMethodParameter resolvable = new ResolvableMethodParameter(principalParameter);
    assertThat(resolver.supportsParameter(resolvable)).as("Principal not supported").isTrue();

    Object result = resolver.resolveArgument(webRequest, resolvable);
    assertThat(result).as("Invalid result").isSameAs(principal);
  }

  @Test
  void principalAsNull() throws Throwable {
    MethodParameter principalParameter = new MethodParameter(method, 1);
    ResolvableMethodParameter resolvable = new ResolvableMethodParameter(principalParameter);
    assertThat(resolver.supportsParameter(resolvable)).as("Principal not supported").isTrue();

    Object result = resolver.resolveArgument(webRequest, resolvable);
    assertThat(result).as("Invalid result").isNull();
  }

  @Test
  void annotatedPrincipal() throws Exception {
    Principal principal = () -> "Foo";
    mockRequest.setUserPrincipal(principal);
    Method principalMethod = getClass().getMethod("supportedParamsWithAnnotatedPrincipal", Principal.class);

    MethodParameter principalParameter = new MethodParameter(principalMethod, 0);
    assertThat(resolver.supportsParameter(new ResolvableMethodParameter(principalParameter))).isTrue();
  }

  @SuppressWarnings("unused")
  public void supportedParams(MockRequest p0, Principal p1) { }

  @Target({ ElementType.PARAMETER })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface AuthenticationPrincipal { }

  @SuppressWarnings("unused")
  public void supportedParamsWithAnnotatedPrincipal(@AuthenticationPrincipal Principal p) { }

}