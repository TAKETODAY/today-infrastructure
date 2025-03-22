/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.instrument.classloading;

import org.junit.jupiter.api.Test;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import infra.instrument.InstrumentationSavingAgent;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/14 14:05
 */
class InstrumentationLoadTimeWeaverTests {

  @Test
  void instrumentationLoadTimeWeaver() {
    Instrumentation0 inst = new Instrumentation0();
    InstrumentationSavingAgent.premain("", inst);
    assertThat(InstrumentationLoadTimeWeaver.isInstrumentationAvailable()).isTrue();

    InstrumentationLoadTimeWeaver weaver = new InstrumentationLoadTimeWeaver();
    assertThat(weaver.getInstrumentableClassLoader()).isNotNull();
    SimpleLoadTimeWeaverTests.ClassFileTransformer0 transformer = new SimpleLoadTimeWeaverTests.ClassFileTransformer0();
    weaver.addTransformer(transformer);

    assertThat(weaver).extracting("transformers").asList().hasSize(1);
    weaver.removeTransformers();
    assertThat(weaver).extracting("transformers").asList().isEmpty();

    assertThat(weaver.getThrowawayClassLoader()).isNotNull().isInstanceOf(SimpleThrowawayClassLoader.class);
  }

  @Test
  void defaultClassLoaderUsedWhenNoArgumentProvided() {
    InstrumentationLoadTimeWeaver weaver = new InstrumentationLoadTimeWeaver();
    assertThat(weaver.getInstrumentableClassLoader()).isSameAs(ClassUtils.getDefaultClassLoader());
  }

  @Test
  void multipleTransformersCanBeAddedAndRemoved() throws IllegalClassFormatException {
    Instrumentation0 inst = new Instrumentation0();
    InstrumentationSavingAgent.premain("", inst);

    InstrumentationLoadTimeWeaver weaver = new InstrumentationLoadTimeWeaver();
    ClassFileTransformer transformer1 = new SimpleLoadTimeWeaverTests.ClassFileTransformer0();
    ClassFileTransformer transformer2 = new SimpleLoadTimeWeaverTests.ClassFileTransformer0();

    weaver.addTransformer(transformer1);
    weaver.addTransformer(transformer2);
    assertThat(weaver).extracting("transformers").asList().hasSize(2);

    weaver.removeTransformers();
    assertThat(weaver).extracting("transformers").asList().isEmpty();
  }

  @Test
  void transformerOnlyAppliesToTargetClassLoader() throws IllegalClassFormatException {
    ClassLoader mockLoader = mock(ClassLoader.class);
    ClassFileTransformer mockTransformer = mock(ClassFileTransformer.class);
    byte[] input = new byte[] { 1, 2, 3 };
    byte[] output = new byte[] { 4, 5, 6 };

    when(mockTransformer.transform(eq(mockLoader), anyString(), isNull(), isNull(), eq(input)))
            .thenReturn(output);

    InstrumentationLoadTimeWeaver weaver = new InstrumentationLoadTimeWeaver(mockLoader);
    weaver.addTransformer(mockTransformer);

    ClassFileTransformer filteringTransformer = weaver.transformers.get(0);
    byte[] result = filteringTransformer.transform(mockLoader, "Test", null, null, input);

    assertThat(result).isEqualTo(output);

    byte[] unchanged = filteringTransformer.transform(
            mock(ClassLoader.class), "Test", null, null, input);
    assertThat(unchanged).isNull();
  }

  @Test
  void throwawayClassLoaderCreatedWithInstrumentableParent() {
    InstrumentationLoadTimeWeaver weaver = new InstrumentationLoadTimeWeaver();
    ClassLoader throwaway = weaver.getThrowawayClassLoader();

    assertThat(throwaway)
            .isInstanceOf(SimpleThrowawayClassLoader.class)
            .extracting("parent")
            .isSameAs(weaver.getInstrumentableClassLoader());
  }

  @Test
  void instrumentationAvailabilityCheck() {
    Instrumentation0 inst = new Instrumentation0();
    InstrumentationSavingAgent.premain("", inst);

    assertThat(InstrumentationLoadTimeWeaver.isInstrumentationAvailable()).isTrue();

    InstrumentationSavingAgent.premain("", null);
    assertThat(InstrumentationLoadTimeWeaver.isInstrumentationAvailable()).isFalse();
  }

  @Test
  void nullTransformerNotAllowed() {
    InstrumentationLoadTimeWeaver weaver = new InstrumentationLoadTimeWeaver();

    assertThatThrownBy(() -> weaver.addTransformer(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Transformer is required");
  }

  static class Instrumentation0 implements Instrumentation {

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {

    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {

    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer) {
      return false;
    }

    @Override
    public boolean isRetransformClassesSupported() {
      return false;
    }

    @Override
    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {

    }

    @Override
    public boolean isRedefineClassesSupported() {
      return false;
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException, UnmodifiableClassException {

    }

    @Override
    public boolean isModifiableClass(Class<?> theClass) {
      return false;
    }

    @Override
    public Class[] getAllLoadedClasses() {
      return new Class[0];
    }

    @Override
    public Class[] getInitiatedClasses(ClassLoader loader) {
      return new Class[0];
    }

    @Override
    public long getObjectSize(Object objectToSize) {
      return 0;
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {

    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarfile) {

    }

    @Override
    public boolean isNativeMethodPrefixSupported() {
      return false;
    }

    @Override
    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {

    }

    @Override
    public void redefineModule(Module module, Set<Module> extraReads, Map<String, Set<Module>> extraExports, Map<String, Set<Module>> extraOpens, Set<Class<?>> extraUses,
            Map<Class<?>, List<Class<?>>> extraProvides) {

    }

    @Override
    public boolean isModifiableModule(Module module) {
      return false;
    }
  }

}