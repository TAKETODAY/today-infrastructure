/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.validation.beanvalidation;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import infra.aop.ProxyMethodInvocation;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.SmartFactoryBean;
import infra.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/29 12:34
 */
@ExtendWith(MockitoExtension.class)
class MethodValidationInterceptorTests {

  @Mock
  private MethodInvocation methodInvocation;

  private MethodValidationInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new MethodValidationInterceptor();
  }

  @Test
  void skipValidationForFactoryBeanMetadataMethods() throws Throwable {
    Method method = FactoryBean.class.getMethod("isSingleton");
    when(methodInvocation.getMethod()).thenReturn(method);
    when(methodInvocation.proceed()).thenReturn(true);

    Object result = interceptor.invoke(methodInvocation);

    assertThat(result).isEqualTo(true);
    verify(methodInvocation).proceed();
  }

  @Test
  void validateMethodArgumentsAndReturnValue() throws Throwable {
    Method method = TestService.class.getMethod("validatedMethod", String.class, Integer.class);
    Object[] args = new Object[] { "test", 5 };
    when(methodInvocation.getThis()).thenReturn(new Object());
    when(methodInvocation.getMethod()).thenReturn(method);
    when(methodInvocation.getArguments()).thenReturn(args);
    when(methodInvocation.proceed()).thenReturn("result");

    Object result = interceptor.invoke(methodInvocation);

    assertThat(result).isEqualTo("result");
  }

  @Test
  void validatesReactiveReturnValues() throws Throwable {
    Method method = TestService.class.getMethod("reactiveMethod");
    when(methodInvocation.getThis()).thenReturn(new Object());
    when(methodInvocation.getMethod()).thenReturn(method);
    when(methodInvocation.proceed()).thenReturn(Mono.just("valid"));
    when(methodInvocation.getArguments()).thenReturn(new Object[0]);

    Object result = interceptor.invoke(methodInvocation);

    assertThat(result).isInstanceOf(Mono.class);
  }

  @Test
  void respectsValidationGroups() throws Throwable {
    Method method = TestService.class.getMethod("groupValidatedMethod", String.class);
    when(methodInvocation.getThis()).thenReturn(new Object());
    when(methodInvocation.getMethod()).thenReturn(method);
    when(methodInvocation.getArguments()).thenReturn(new Object[] { "test" });
    when(methodInvocation.proceed()).thenReturn("result");

    Object result = interceptor.invoke(methodInvocation);

    assertThat(result).isEqualTo("result");
  }

  @Test
  void handlesSmartFactoryBeanMethods() throws Throwable {
    Method method = SmartFactoryBean.class.getMethod("isPrototype");
    when(methodInvocation.getMethod()).thenReturn(method);
    when(methodInvocation.proceed()).thenReturn(true);

    Object result = interceptor.invoke(methodInvocation);

    assertThat(result).isEqualTo(true);
    verify(methodInvocation).proceed();
  }

  @Test
  void validatesCustomGroups() throws Throwable {
    Method method = TestService.class.getMethod("customGroupMethod", String.class);
    when(methodInvocation.getThis()).thenReturn(new Object());
    when(methodInvocation.getMethod()).thenReturn(method);
    when(methodInvocation.getArguments()).thenReturn(new Object[] { "test" });
    when(methodInvocation.proceed()).thenReturn("result");

    Object result = interceptor.invoke(methodInvocation);

    assertThat(result).isEqualTo("result");
  }

  @Test
  void validatesCascadedObjects() throws Throwable {
    Method method = TestService.class.getMethod("cascadedMethod", TestData.class);
    TestData testData = new TestData("test");

    when(methodInvocation.getThis()).thenReturn(new Object());
    when(methodInvocation.getMethod()).thenReturn(method);
    when(methodInvocation.getArguments()).thenReturn(new Object[] { testData });
    when(methodInvocation.proceed()).thenReturn("result");

    Object result = interceptor.invoke(methodInvocation);

    assertThat(result).isEqualTo("result");
  }

  @Test
  void handlesMethodsWithMultipleValidationAnnotations() throws Throwable {
    Method method = TestService.class.getMethod("multiValidatedMethod", String.class, Integer.class);
    Object[] args = new Object[] { "test", 5 };

    when(methodInvocation.getThis()).thenReturn(new Object());
    when(methodInvocation.getMethod()).thenReturn(method);
    when(methodInvocation.getArguments()).thenReturn(args);
    when(methodInvocation.proceed()).thenReturn("result");

    Object result = interceptor.invoke(methodInvocation);

    assertThat(result).isEqualTo("result");
  }

  @Test
  void handlesNullTarget() throws Throwable {
    Method method = TestService.class.getMethod("validatedMethod", String.class, Integer.class);
    Object[] args = new Object[] { "test", 5 };
    Object proxy = new TestService() {
      @Override
      public @NotNull String validatedMethod(@NotNull String arg1, @Max(10) Integer arg2) {
        return "result";
      }

      @Override
      public @NotNull Mono<String> reactiveMethod() {
        return null;
      }

      @Override
      public String groupValidatedMethod(@NotNull String arg) {
        return null;
      }

      @Override
      public String customGroupMethod(@NotNull String arg) {
        return null;
      }

      @Override
      public String cascadedMethod(@Valid TestData data) {
        return null;
      }

      @Override
      public String multiValidatedMethod(@NotNull String arg1, @Max(10) Integer arg2) {
        return null;
      }
    };

    ProxyMethodInvocation proxyInvocation = mock(ProxyMethodInvocation.class);
    when(proxyInvocation.getThis()).thenReturn(null);
    when(proxyInvocation.getMethod()).thenReturn(method);
    when(proxyInvocation.getArguments()).thenReturn(args);
    when(proxyInvocation.getProxy()).thenReturn(proxy);
    when(proxyInvocation.proceed()).thenReturn("result");

    Object result = interceptor.invoke(proxyInvocation);

    assertThat(result).isEqualTo("result");
  }

  @Test
  void defaultConstructorCreatesValidatorWithDefaults() throws Throwable {
    MethodValidationInterceptor interceptor = new MethodValidationInterceptor();
    Method method = TestService.class.getMethod("validatedMethod", String.class, Integer.class);
    Object[] args = new Object[] { "test", 5 };

    when(methodInvocation.getThis()).thenReturn(new Object());
    when(methodInvocation.getMethod()).thenReturn(method);
    when(methodInvocation.getArguments()).thenReturn(args);
    when(methodInvocation.proceed()).thenReturn("result");

    Object result = interceptor.invoke(methodInvocation);

    assertThat(result).isEqualTo("result");
  }

  @Validated
  interface TestService {

    @Validated(Special.class)
    @Valid
    String customGroupMethod(@NotNull String arg);

    String cascadedMethod(@Valid TestData data);

    @Validated(Special.class)
    @Valid
    String multiValidatedMethod(@NotNull String arg1, @Max(10) Integer arg2);

    @NotNull
    String validatedMethod(@NotNull String arg1, @Max(10) Integer arg2);

    @NotNull
    Mono<String> reactiveMethod();

    @Validated(Special.class)
    String groupValidatedMethod(@NotNull String arg);
  }

  class TestData {

    @NotNull
    private final String value;

    TestData(String value) {
      this.value = value;
    }
  }

  interface Special {
  }

}