/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/8 13:48
 */
class ParameterResolutionDelegateTests {

  @Test
  public void isAutowirablePreconditions() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    ParameterResolutionDelegate.isAutowirable(null, 0))
            .withMessageContaining("Parameter must not be null");
  }

  @Test
  public void annotatedParametersInMethodAreCandidatesForAutowiring() throws Exception {
    Method method = getClass().getDeclaredMethod("autowirableMethod", String.class, String.class, String.class, String.class);
    assertAutowirableParameters(method);
  }

  @Test
  public void annotatedParametersInTopLevelClassConstructorAreCandidatesForAutowiring() throws Exception {
    Constructor<?> constructor = AutowirableClass.class.getConstructor(String.class, String.class, String.class, String.class);
    assertAutowirableParameters(constructor);
  }

  @Test
  public void annotatedParametersInInnerClassConstructorAreCandidatesForAutowiring() throws Exception {
    Class<?> innerClass = AutowirableClass.InnerAutowirableClass.class;
    assertThat(ClassUtils.isInnerClass(innerClass)).isTrue();
    Constructor<?> constructor = innerClass.getConstructor(AutowirableClass.class, String.class, String.class);
    assertAutowirableParameters(constructor);
  }

  private void assertAutowirableParameters(Executable executable) {
    int startIndex = (executable instanceof Constructor)
                             && ClassUtils.isInnerClass(executable.getDeclaringClass()) ? 1 : 0;
    Parameter[] parameters = executable.getParameters();
    for (int parameterIndex = startIndex; parameterIndex < parameters.length; parameterIndex++) {
      Parameter parameter = parameters[parameterIndex];
      assertThat(ParameterResolutionDelegate.isAutowirable(parameter, parameterIndex)).as("Parameter " + parameter + " must be autowirable").isTrue();
    }
  }

  @Test
  public void nonAnnotatedParametersInTopLevelClassConstructorAreNotCandidatesForAutowiring() throws Exception {
    Constructor<?> notAutowirableConstructor = AutowirableClass.class.getConstructor(String.class);

    Parameter[] parameters = notAutowirableConstructor.getParameters();
    for (int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++) {
      Parameter parameter = parameters[parameterIndex];
      assertThat(ParameterResolutionDelegate.isAutowirable(parameter, parameterIndex)).as("Parameter " + parameter + " must not be autowirable").isFalse();
    }
  }

  @Test
  public void resolveDependencyPreconditionsForParameter() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    ParameterResolutionDelegate.resolveDependency(null, 0, null, mock(AutowireCapableBeanFactory.class)))
            .withMessageContaining("Parameter must not be null");
  }

  @Test
  public void resolveDependencyPreconditionsForContainingClass() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    ParameterResolutionDelegate.resolveDependency(getParameter(), 0, null, null))
            .withMessageContaining("Containing class must not be null");
  }

  @Test
  public void resolveDependencyPreconditionsForBeanFactory() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    ParameterResolutionDelegate.resolveDependency(getParameter(), 0, getClass(), null))
            .withMessageContaining("AutowireCapableBeanFactory must not be null");
  }

  private Parameter getParameter() throws NoSuchMethodException {
    Method method = getClass().getDeclaredMethod("autowirableMethod", String.class, String.class, String.class, String.class);
    return method.getParameters()[0];
  }

  @Test
  public void resolveDependencyForAnnotatedParametersInTopLevelClassConstructor() throws Exception {
    Constructor<?> constructor = AutowirableClass.class.getConstructor(String.class, String.class, String.class, String.class);

    AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
    // Configure the mocked BeanFactory to return the DependencyDescriptor for convenience and
    // to avoid using an ArgumentCaptor.
    given(beanFactory.resolveDependency(any(), isNull())).willAnswer(invocation -> invocation.getArgument(0));

    Parameter[] parameters = constructor.getParameters();
    for (int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++) {
      Parameter parameter = parameters[parameterIndex];
      DependencyDescriptor intermediateDependencyDescriptor = (DependencyDescriptor) ParameterResolutionDelegate.resolveDependency(
              parameter, parameterIndex, AutowirableClass.class, beanFactory);
      assertThat(intermediateDependencyDescriptor.getAnnotatedElement()).isEqualTo(constructor);
      assertThat(intermediateDependencyDescriptor.getMethodParameter().getParameter()).isEqualTo(parameter);
    }
  }

  void autowirableMethod(
          @Autowired String firstParameter,
          @Qualifier("someQualifier") String secondParameter,
          @Value("${someValue}") String thirdParameter,
          @Autowired(required = false) String fourthParameter) {
  }

  public static class AutowirableClass {

    public AutowirableClass(@Autowired String firstParameter,
            @Qualifier("someQualifier") String secondParameter,
            @Value("${someValue}") String thirdParameter,
            @Autowired(required = false) String fourthParameter) {
    }

    public AutowirableClass(String notAutowirableParameter) {
    }

    public class InnerAutowirableClass {

      public InnerAutowirableClass(@Autowired String firstParameter,
              @Qualifier("someQualifier") String secondParameter) {
      }
    }
  }

}
