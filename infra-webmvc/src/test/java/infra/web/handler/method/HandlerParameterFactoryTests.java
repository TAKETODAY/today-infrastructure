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

package infra.web.handler.method;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import infra.core.MethodParameter;
import infra.core.ParameterNameDiscoverer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2026/11/5 10:00
 */
class HandlerParameterFactoryTests {

  private final HandlerParameterFactory factory = new HandlerParameterFactory();

  @Test
  void shouldRejectNullParameterNameDiscoverer() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new HandlerParameterFactory(null))
            .withMessageContaining("parameterNameDiscoverer is required");
  }

  @Test
  void shouldReturnEmptyArrayForMethodWithoutParameters() throws NoSuchMethodException {
    HandlerMethod handlerMethod = handlerMethod("noParameters");

    HandlerParameter[] parameters = factory.createArray(handlerMethod);

    assertThat(parameters).isEmpty();
    assertThat(factory.createArray(handlerMethod)).isSameAs(parameters);
  }

  @Test
  void shouldWrapEveryParameterInDeclarationOrder() throws NoSuchMethodException {
    HandlerMethod handlerMethod = handlerMethod("twoParameters", String.class, int.class);

    HandlerParameter[] parameters = factory.createArray(handlerMethod);

    assertThat(parameters).hasSize(2);
    assertThat(parameters[0].getMethodParameter()).isSameAs(handlerMethod.getParameters()[0]);
    assertThat(parameters[0].getParameterIndex()).isZero();
    assertThat(parameters[0].getParameterType()).isEqualTo(String.class);
    assertThat(parameters[1].getMethodParameter()).isSameAs(handlerMethod.getParameters()[1]);
    assertThat(parameters[1].getParameterIndex()).isEqualTo(1);
    assertThat(parameters[1].getParameterType()).isEqualTo(int.class);
  }

  @Test
  void shouldUseSharedParameterNameDiscovererByDefault() throws NoSuchMethodException {
    HandlerParameter[] parameters = factory.createArray(handlerMethod("twoParameters", String.class, int.class));

    assertThat(parameters[0].getParameterName()).isEqualTo("name");
    assertThat(parameters[1].getParameterName()).isEqualTo("age");
  }

  @Test
  void shouldInitializeParameterNameDiscoveryWithConfiguredDiscoverer() throws NoSuchMethodException {
    var discoverer = new RecordingParameterNameDiscoverer("first", "second");
    var factory = new HandlerParameterFactory(discoverer);
    HandlerMethod handlerMethod = handlerMethod("twoParameters", String.class, int.class);

    HandlerParameter[] parameters = factory.createArray(handlerMethod);

    assertThat(parameters[0].getParameterName()).isEqualTo("first");
    assertThat(parameters[1].getParameterName()).isEqualTo("second");
    assertThat(discoverer.executables).containsExactly(handlerMethod.getMethod(), handlerMethod.getMethod());
  }

  @Test
  void shouldUseOverriddenCreateParameterInCreateArray() throws NoSuchMethodException {
    var factory = new HandlerParameterFactory() {
      @Override
      public HandlerParameter createParameter(MethodParameter parameter) {
        return new CustomHandlerParameter(parameter);
      }
    };

    HandlerParameter[] parameters = factory.createArray(handlerMethod("twoParameters", String.class, int.class));

    assertThat(parameters)
            .hasSize(2)
            .allMatch(CustomHandlerParameter.class::isInstance);
  }

  @Test
  void shouldNotCacheArraysCreatedByCreateArray() throws NoSuchMethodException {
    HandlerMethod handlerMethod = handlerMethod("twoParameters", String.class, int.class);

    HandlerParameter[] first = factory.createArray(handlerMethod);
    HandlerParameter[] second = factory.createArray(handlerMethod);

    assertThat(second)
            .isNotSameAs(first)
            .containsExactly(first);
    assertThat(factory.getParameters(handlerMethod)).isNotSameAs(first);
  }

  @Test
  void shouldReuseParametersForTheSameUnderlyingMethod() throws NoSuchMethodException {
    HandlerMethod handlerMethod = handlerMethod("twoParameters", String.class, int.class);
    HandlerMethod otherBeanSameMethod = new HandlerMethod(new Object(), handlerMethod.getMethod());

    HandlerParameter[] parameters = factory.getParameters(handlerMethod);

    assertThat(parameters).hasSize(2);
    assertThat(factory.getParameters(handlerMethod)).isSameAs(parameters);
    assertThat(factory.getParameters(otherBeanSameMethod)).isSameAs(parameters);
  }

  @Test
  void shouldCacheParametersPerUnderlyingMethod() throws NoSuchMethodException {
    HandlerParameter[] twoParameters = factory.getParameters(handlerMethod("twoParameters", String.class, int.class));
    HandlerParameter[] oneParameter = factory.getParameters(handlerMethod("plain", String.class));

    assertThat(twoParameters).hasSize(2).isNotSameAs(oneParameter);
    assertThat(oneParameter).hasSize(1);
    assertThat(oneParameter[0].getParameterType()).isEqualTo(String.class);
    assertThat(factory.getParameters(handlerMethod("twoParameters", String.class, int.class)))
            .isSameAs(twoParameters);
    assertThat(factory.getParameters(handlerMethod("plain", String.class))).isSameAs(oneParameter);
  }

  @Test
  void shouldReturnEmptyArrayForMethodWithoutParametersWhenCached() throws NoSuchMethodException {
    HandlerMethod handlerMethod = handlerMethod("noParameters");

    HandlerParameter[] parameters = factory.getParameters(handlerMethod);

    assertThat(parameters).isEmpty();
    assertThat(factory.getParameters(handlerMethod)).isSameAs(parameters);
  }

  @Test
  void shouldCreateParameterFromMethodParameter() throws NoSuchMethodException {
    Method method = HandlerParameterFactoryTests.class.getDeclaredMethod("plain", String.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    HandlerParameter parameter = factory.createParameter(methodParameter);

    assertThat(parameter.getMethodParameter()).isSameAs(methodParameter);
    assertThat(parameter.getParameterType()).isEqualTo(String.class);
  }

  @Test
  @SuppressWarnings("NullAway")
  void shouldRejectNullMethodParameter() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.createParameter(null))
            .withMessageContaining("parameter is required");
  }

  private HandlerMethod handlerMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
    Method method = HandlerParameterFactoryTests.class.getDeclaredMethod(name, parameterTypes);
    return new HandlerMethod(this, method);
  }

  public void noParameters() {
  }

  public void plain(String value) {
  }

  public void twoParameters(String name, int age) {
  }

  private static final class RecordingParameterNameDiscoverer extends ParameterNameDiscoverer {

    private final String[] names;

    private final List<Executable> executables = new ArrayList<>();

    private RecordingParameterNameDiscoverer(String... names) {
      this.names = names;
    }

    @Override
    protected String[] doGet(Executable executable) {
      executables.add(executable);
      return names;
    }
  }

  private static final class CustomHandlerParameter extends HandlerParameter {

    private CustomHandlerParameter(MethodParameter parameter) {
      super(parameter);
    }
  }

}
