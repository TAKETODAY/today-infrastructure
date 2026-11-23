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

package infra.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.lang.reflect.Field;

import infra.core.ResolvableType;
import infra.test.context.bean.override.BeanOverrideStrategy;
import infra.beans.factory.config.BeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MockitoHandlerSettingsTests {

  @Test
  void abstractHandlersDefaultNullResetAndCompareTheirOwnIdentity() {
    var handler = new AbstractMockitoBeanOverrideHandler((Field) null, ResolvableType.forClass(Runnable.class),
            null, "", BeanOverrideStrategy.REPLACE, null) {
      @Override
      protected Object createOverrideInstance(String beanName, BeanDefinition definition, Object instance) {
        return Mockito.mock(Runnable.class);
      }
    };
    assertThat(handler).isEqualTo(handler).isNotEqualTo(new Object());
    assertThat(handler.getReset()).isEqualTo(MockReset.AFTER);
    assertThat(MockReset.apply(null, Mockito.withSettings())).isNotNull();
    Object untagged = Mockito.mock(Runnable.class,
            Mockito.withSettings().invocationListeners(report -> { }));
    assertThat(MockReset.get(untagged)).isEqualTo(MockReset.NONE);
  }

  @Test
  void spyHandlersRetainExistingSpiesAndDelegateToJdkProxies() {
    var handler = new MockitoSpyBeanOverrideHandler(ResolvableType.forClass(Runnable.class),
            Spies.class.getAnnotation(MockitoSpyBean.class));
    assertThatIllegalArgumentException().isThrownBy(() -> handler.createOverrideInstance("missing", null, null))
            .withMessageContaining("requires an existing bean instance");
    Runnable target = Mockito.mock(Runnable.class);
    Runnable proxy = (Runnable) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Runnable.class },
            (object, method, arguments) -> method.invoke(target, arguments));
    Runnable spy = (Runnable) handler.createOverrideInstance("", null, proxy);
    spy.run();
    Mockito.verify(target).run();
    Mockito.verify(spy).run();
    Runnable existingSpy = Mockito.spy(new Runnable() {
      @Override
      public void run() { }
    });
    assertThat(handler.createOverrideInstance("existing", null, existingSpy)).isSameAs(existingSpy);
  }

  @Test
  void equalityIncludesEveryMockSettingButNotAnnotationIdentity() {
    MockitoBeanOverrideHandler baseline = handler(Default.class);
    assertThat(baseline).isEqualTo(baseline).isEqualTo(handler(Default.class))
            .isNotEqualTo(null).isNotEqualTo(new Object());
    for (Class<?> type : new Class<?>[] { SerializableMock.class, CustomAnswer.class, ExtraInterface.class,
            BeforeReset.class, Named.class, Replacement.class }) {
      MockitoBeanOverrideHandler different = handler(type);
      assertThat(baseline).as(type.getSimpleName()).isNotEqualTo(different);
      assertThat(different).isNotEqualTo(baseline);
    }
    assertThat(baseline).hasSameHashCodeAs(handler(Default.class));
  }

  @Test
  void createdMockHonorsNameExtraInterfacesSerializationAndReset() {
    MockitoBeanOverrideHandler handler = handler(SerializableMock.class);
    Object mock = handler.createOverrideInstance("", null, null);
    assertThat(mock).isInstanceOf(Runnable.class).isInstanceOf(Serializable.class);
    assertThat(Mockito.mockingDetails(mock).getMockCreationSettings().isSerializable()).isTrue();
    assertThat(handler.createOverrideInstance("custom", null, null)).satisfies(named ->
            assertThat(Mockito.mockingDetails(named).getMockCreationSettings().getMockName().toString()).isEqualTo("custom"));
    Object extra = handler(ExtraInterface.class).createOverrideInstance("extra", null, null);
    assertThat(extra).isInstanceOf(AutoCloseable.class);
    assertThat(handler(BeforeReset.class).getReset()).isEqualTo(MockReset.BEFORE);
    assertThat(handler.toString()).contains("serializable", "true");
  }

  private MockitoBeanOverrideHandler handler(Class<?> type) {
    return new MockitoBeanOverrideHandler(ResolvableType.forClass(Runnable.class), type.getAnnotation(MockitoBean.class));
  }

  @MockitoBean(types = Runnable.class)
  static class Default { }

  @MockitoBean(types = Runnable.class, serializable = true)
  static class SerializableMock { }

  @MockitoBean(types = Runnable.class, answers = Answers.RETURNS_SMART_NULLS)
  static class CustomAnswer { }

  @MockitoBean(types = Runnable.class, extraInterfaces = AutoCloseable.class)
  static class ExtraInterface { }

  @MockitoBean(types = Runnable.class, reset = MockReset.BEFORE)
  static class BeforeReset { }

  @MockitoBean(types = Runnable.class, name = "named")
  static class Named { }

  @MockitoBean(types = Runnable.class, enforceOverride = true)
  static class Replacement { }

  @MockitoSpyBean(types = Runnable.class)
  static class Spies { }

}
