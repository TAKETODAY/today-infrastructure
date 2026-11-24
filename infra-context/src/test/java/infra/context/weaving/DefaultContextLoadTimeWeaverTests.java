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

package infra.context.weaving;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import infra.instrument.InstrumentationSavingAgent;
import infra.instrument.classloading.LoadTimeWeaver;
import infra.instrument.classloading.SimpleInstrumentableClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 16:30
 */
class DefaultContextLoadTimeWeaverTests {

  @Test
  void addTransformerBeforeInitializationThrowsException() {
    DefaultContextLoadTimeWeaver weaver = new DefaultContextLoadTimeWeaver();
    ClassFileTransformer transformer = mock(ClassFileTransformer.class);

    assertThatIllegalStateException()
            .isThrownBy(() -> weaver.addTransformer(transformer))
            .withMessage("Not initialized");
  }

  @Test
  void getInstrumentableClassLoaderBeforeInitializationThrowsException() {
    DefaultContextLoadTimeWeaver weaver = new DefaultContextLoadTimeWeaver();

    assertThatIllegalStateException()
            .isThrownBy(weaver::getInstrumentableClassLoader)
            .withMessage("Not initialized");
  }

  @Test
  void getThrowawayClassLoaderBeforeInitializationThrowsException() {
    DefaultContextLoadTimeWeaver weaver = new DefaultContextLoadTimeWeaver();

    assertThatIllegalStateException()
            .isThrownBy(weaver::getThrowawayClassLoader)
            .withMessage("Not initialized");
  }

  @Test
  void nullClassLoaderThrowsException() {
    DefaultContextLoadTimeWeaver weaver = new DefaultContextLoadTimeWeaver();
    assertThatThrownBy(() -> weaver.setBeanClassLoader(null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void prefersTheServerSpecificWeaverAndDelegatesClassLoaderAccess() {
    var serverWeaver = mock(LoadTimeWeaver.class);
    var weaver = new DefaultContextLoadTimeWeaver() {
      @Override
      protected LoadTimeWeaver createServerSpecificLoadTimeWeaver(ClassLoader classLoader) {
        return serverWeaver;
      }
    };
    var instrumentable = getClass().getClassLoader();
    var throwaway = new ClassLoader(instrumentable) { };
    var transformer = mock(ClassFileTransformer.class);
    given(serverWeaver.getInstrumentableClassLoader()).willReturn(instrumentable);
    given(serverWeaver.getThrowawayClassLoader()).willReturn(throwaway);

    weaver.setBeanClassLoader(instrumentable);
    weaver.addTransformer(transformer);

    then(serverWeaver).should().addTransformer(transformer);
    assertThat(weaver.getInstrumentableClassLoader()).isSameAs(instrumentable);
    assertThat(weaver.getThrowawayClassLoader()).isSameAs(throwaway);
  }

  @Test
  void usesReflectiveClassLoaderHooksWithoutAnAgent() {
    try (var agent = mockStatic(InstrumentationSavingAgent.class)) {
      agent.when(InstrumentationSavingAgent::getInstrumentation).thenReturn(null);
      var loader = new SimpleInstrumentableClassLoader(getClass().getClassLoader());
      var weaver = new DefaultContextLoadTimeWeaver(loader);

      assertThat(weaver.getInstrumentableClassLoader()).isSameAs(loader);
      assertThat(weaver.getThrowawayClassLoader()).isNotSameAs(loader);
      assertThatIllegalStateException().isThrownBy(() -> new DefaultContextLoadTimeWeaver(getClass().getClassLoader()))
              .withMessageContaining("Specify a custom LoadTimeWeaver");
    }
  }

  @Test
  void removesTransformersRegisteredThroughTheAgentWhenDestroyed() {
    var instrumentation = mock(Instrumentation.class);
    try (var agent = mockStatic(InstrumentationSavingAgent.class)) {
      agent.when(InstrumentationSavingAgent::getInstrumentation).thenReturn(instrumentation);
      var loader = getClass().getClassLoader();
      var weaver = new DefaultContextLoadTimeWeaver(loader);
      weaver.addTransformer(mock(ClassFileTransformer.class));
      assertThat(weaver.getInstrumentableClassLoader()).isSameAs(loader);

      weaver.destroy();

      var registered = ArgumentCaptor.forClass(ClassFileTransformer.class);
      then(instrumentation).should().addTransformer(registered.capture());
      then(instrumentation).should().removeTransformer(registered.getValue());
      weaver.destroy();
      then(instrumentation).shouldHaveNoMoreInteractions();
    }
  }

}
