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

package infra.test.context.support;

import org.junit.jupiter.api.Test;

import infra.beans.factory.InitializingBean;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.test.context.TestContext;
import infra.test.context.aot.AotTestContextInitializers;
import jakarta.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DependencyInjectionAotTests {

  @Test
  void aotInjectionSupportsAutowiredResourceInitializationAndReinjection() throws Exception {
    try (var initializers = mockConstruction(AotTestContextInitializers.class,
            (mock, context) -> given(mock.isSupportedTestClass(Injected.class)).willReturn(true));
            GenericApplicationContext application = new GenericApplicationContext()) {
      application.registerBean("message", String.class, () -> "injected");
      application.refresh();
      Injected instance = new Injected();
      TestContext context = context(application, instance);
      DependencyInjectionTestExecutionListener listener = new DependencyInjectionTestExecutionListener();
      listener.prepareTestInstance(context);
      assertThat(instance.autowired).isEqualTo("injected");
      assertThat(instance.resource).isEqualTo("injected");
      assertThat(instance.initializations).isEqualTo(1);
      given(context.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE)).willReturn(true);
      listener.beforeTestMethod(context);
      assertThat(instance.initializations).isEqualTo(2);
      verify(context, times(2)).removeAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE);
      assertThat(initializers.constructed()).hasSize(1);
    }
  }

  @Test
  void aotInjectionRejectsNonGenericApplicationContext() {
    try (var initializers = mockConstruction(AotTestContextInitializers.class,
            (mock, context) -> given(mock.isSupportedTestClass(Injected.class)).willReturn(true))) {
      TestContext context = context(mock(ApplicationContext.class), new Injected());
      DependencyInjectionTestExecutionListener listener = new DependencyInjectionTestExecutionListener();
      assertThatIllegalStateException().isThrownBy(() -> listener.prepareTestInstance(context))
              .withMessageStartingWith("AOT ApplicationContext must be a GenericApplicationContext");
      assertThat(initializers.constructed()).hasSize(1);
    }
  }

  private TestContext context(ApplicationContext application, Injected instance) {
    TestContext context = mock(TestContext.class);
    org.mockito.BDDMockito.<Class<?>>given(context.getTestClass()).willReturn(Injected.class);
    given(context.getTestInstance()).willReturn(instance);
    given(context.getApplicationContext()).willReturn(application);
    return context;
  }

  static class Injected implements InitializingBean {
    @Autowired
    String autowired;

    @Resource(name = "message")
    String resource;

    int initializations;

    @Override
    public void afterPropertiesSet() {
      initializations++;
    }
  }

}
