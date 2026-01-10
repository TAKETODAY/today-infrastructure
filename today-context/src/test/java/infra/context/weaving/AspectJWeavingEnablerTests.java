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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.core.Ordered;
import infra.instrument.classloading.LoadTimeWeaver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 19:33
 */
class AspectJWeavingEnablerTests {

  @Test
  void highestPrecedenceOrder() {
    AspectJWeavingEnabler enabler = new AspectJWeavingEnabler();
    assertThat(enabler.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
  }

  @Test
  void postProcessWithoutWeaverThrowsException() {
    AspectJWeavingEnabler enabler = new AspectJWeavingEnabler();
    ConfigurableBeanFactory factory = mock(ConfigurableBeanFactory.class);

    assertThatIllegalStateException()
            .isThrownBy(() -> enabler.postProcessBeanFactory(factory))
            .withMessage("No LoadTimeWeaver available");
  }

  @Test
  void enableAspectJWeavingWithExplicitWeaver() {
    LoadTimeWeaver mockWeaver = mock(LoadTimeWeaver.class);
    ClassLoader mockLoader = mock(ClassLoader.class);

    AspectJWeavingEnabler.enableAspectJWeaving(mockWeaver, mockLoader);

    verify(mockWeaver).addTransformer(any(ClassFileTransformer.class));
  }

  @Test
  void aspectJClassesAreBypassedByTransformer() throws IllegalClassFormatException {
    ClassFileTransformer mockDelegate = mock(ClassFileTransformer.class);
    var transformer = new AspectJWeavingEnabler.AspectJClassBypassingClassFileTransformer(mockDelegate);

    byte[] input = new byte[] { 1, 2, 3 };
    byte[] result = transformer.transform(null, "org.aspectj.SomeClass", null, null, input);

    assertThat(result).isSameAs(input);
    verifyNoInteractions(mockDelegate);
  }

  @Test
  void nonAspectJClassesAreTransformed() throws IllegalClassFormatException {
    ClassFileTransformer mockDelegate = mock(ClassFileTransformer.class);
    byte[] input = new byte[] { 1, 2, 3 };
    byte[] expected = new byte[] { 4, 5, 6 };
    when(mockDelegate.transform(any(), anyString(), any(), any(), any())).thenReturn(expected);

    var transformer = new AspectJWeavingEnabler.AspectJClassBypassingClassFileTransformer(mockDelegate);
    byte[] result = transformer.transform(null, "com.example.SomeClass", null, null, input);

    assertThat(result).isSameAs(expected);
  }

  @Test
  void weaverAndClassLoaderCanBeSet() {
    AspectJWeavingEnabler enabler = new AspectJWeavingEnabler();
    LoadTimeWeaver mockWeaver = mock(LoadTimeWeaver.class);
    ClassLoader mockLoader = mock(ClassLoader.class);

    enabler.setLoadTimeWeaver(mockWeaver);
    enabler.setBeanClassLoader(mockLoader);

    ConfigurableBeanFactory factory = mock(ConfigurableBeanFactory.class);
    enabler.postProcessBeanFactory(factory);

    verify(mockWeaver).addTransformer(any(ClassFileTransformer.class));
  }

  @Test
  void delegateReturningNullIsHandled() throws IllegalClassFormatException {
    ClassFileTransformer mockDelegate = mock(ClassFileTransformer.class);
    byte[] input = new byte[] { 1, 2, 3 };
    when(mockDelegate.transform(any(), anyString(), any(), any(), any())).thenReturn(null);

    var transformer = new AspectJWeavingEnabler.AspectJClassBypassingClassFileTransformer(mockDelegate);
    byte[] result = transformer.transform(null, "com.example.Test", null, null, input);

    assertThat(result).isNull();
  }

  @Test
  void delegateThrowingExceptionIsHandled() throws IllegalClassFormatException {
    ClassFileTransformer mockDelegate = mock(ClassFileTransformer.class);
    when(mockDelegate.transform(any(), anyString(), any(), any(), any()))
            .thenThrow(new IllegalClassFormatException("Invalid class format"));

    var transformer = new AspectJWeavingEnabler.AspectJClassBypassingClassFileTransformer(mockDelegate);

    assertThatExceptionOfType(IllegalClassFormatException.class)
            .isThrownBy(() -> transformer.transform(null, "com.example.Test", null, null, new byte[0]))
            .withMessage("Invalid class format");
  }

  @Test
  void aspectjPackageVariantsAreBypassedByTransformer() throws IllegalClassFormatException {
    ClassFileTransformer mockDelegate = mock(ClassFileTransformer.class);
    var transformer = new AspectJWeavingEnabler.AspectJClassBypassingClassFileTransformer(mockDelegate);
    byte[] input = new byte[] { 1, 2, 3 };

    String[] aspectjPaths = {
            "org.aspectj.internal.Class",
            "org/aspectj/internal/Class",
            "org.aspectj.weaver.Class",
            "org/aspectj/weaver/Class",
            "org.aspectj.runtime.Class",
            "org/aspectj/runtime/Class"
    };

    for (String path : aspectjPaths) {
      byte[] result = transformer.transform(null, path, null, null, input);
      assertThat(result).as("Testing path: " + path).isSameAs(input);
    }

    verifyNoInteractions(mockDelegate);
  }

}