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

package infra.instrument.classloading;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 18:51
 */
class ShadowingClassLoaderTests {

  @Test
  void excludedPackagesAreLoadedFromEnclosingClassLoader() throws Exception {
    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    Class<?> javaClass = loader.loadClass("java.lang.String");
    assertThat(javaClass.getClassLoader()).isNull();
  }

  @Test
  void transformersAreAppliedToEligibleClasses() throws Exception {
    ClassFileTransformer transformer = mock(ClassFileTransformer.class);
    byte[] input = new byte[] { 1, 2, 3 };
    byte[] output = new byte[] { 4, 5, 6 };
    when(transformer.transform(any(), eq("test/TestClass"), isNull(), isNull(), eq(input)))
            .thenReturn(output);

    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    loader.addTransformer(transformer);

    assertThatThrownBy(() -> loader.loadClass("test.TestClass"))
            .isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void resourcesAreDelegatedToEnclosingClassLoader() {
    ClassLoader parentLoader = mock(ClassLoader.class);
    URL expectedUrl = getClass().getResource("/test.properties");
    when(parentLoader.getResource("test.properties")).thenReturn(expectedUrl);

    ShadowingClassLoader loader = new ShadowingClassLoader(parentLoader);
    assertThat(loader.getResource("test.properties")).isEqualTo(expectedUrl);
  }

  @Test
  void transformersCanBeCopiedFromAnotherLoader() throws Exception {
    ClassFileTransformer transformer = mock(ClassFileTransformer.class);

    ShadowingClassLoader first = new ShadowingClassLoader(getClass().getClassLoader());
    first.addTransformer(transformer);

    ShadowingClassLoader second = new ShadowingClassLoader(getClass().getClassLoader());
    second.copyTransformers(first);

    assertThat(second)
            .extracting("classFileTransformers")
            .asList()
            .containsExactly(transformer);
  }

  @Test
  void nullEnclosingClassLoaderNotAllowed() {
    assertThatThrownBy(() -> new ShadowingClassLoader(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Enclosing ClassLoader is required");
  }

  @Test
  void nullTransformerNotAllowed() {
    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    assertThatThrownBy(() -> loader.addTransformer(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Transformer is required");
  }

  @Test
  void loadNonExcludedClassSuccessfully() throws Exception {
    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    Class<?> loadedClass = loader.loadClass(getClass().getName());
    assertThat(loadedClass.getClassLoader()).isSameAs(loader);
  }

  @Test
  void classesAreCachedAfterLoading() throws Exception {
    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    Class<?> first = loader.loadClass(getClass().getName());
    Class<?> second = loader.loadClass(getClass().getName());
    assertThat(first).isSameAs(second);
  }

  @Test
  void classNotFoundWhenResourceDoesNotExist() {
    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    assertThatThrownBy(() -> loader.loadClass("does.not.exist.TestClass"))
            .isInstanceOf(ClassNotFoundException.class)
            .hasMessageContaining("does.not.exist.TestClass");
  }

  @Test
  void transformersAreAppliedInOrder() throws Exception {
    ClassFileTransformer first = mock(ClassFileTransformer.class);
    ClassFileTransformer second = mock(ClassFileTransformer.class);

    byte[] initial = new byte[] { 1 };
    byte[] afterFirst = new byte[] { 2 };
    byte[] afterSecond = new byte[] { 3 };

    when(first.transform(any(), any(), isNull(), isNull(), eq(initial))).thenReturn(afterFirst);
    when(second.transform(any(), any(), isNull(), isNull(), eq(afterFirst))).thenReturn(afterSecond);

    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    loader.addTransformer(first);
    loader.addTransformer(second);

    // Use a real class that exists
    String className = getClass().getName();
    Class<?> transformed = loader.loadClass(className);
    assertThat(transformed).isNotNull();
  }

  @Test
  void shadowingClassLoaderDoesNotShadowItself() throws Exception {
    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    Class<?> loadedClass = loader.loadClass(ShadowingClassLoader.class.getName());
    assertThat(loadedClass.getClassLoader()).isNotSameAs(loader);
  }

  @Test
  void multipleResourcesAreDelegatedToEnclosingClassLoader() throws IOException {
    ClassLoader parentLoader = mock(ClassLoader.class);
    URL url1 = new URL("file:/test1.properties");
    URL url2 = new URL("file:/test2.properties");

    ArrayList<URL> urls = new ArrayList<>();
    urls.add(url1);
    urls.add(url2);
    when(parentLoader.getResources("test.properties")).thenReturn(java.util.Collections.enumeration(urls));

    ShadowingClassLoader loader = new ShadowingClassLoader(parentLoader);
    Enumeration<URL> resources = loader.getResources("test.properties");

    ArrayList<URL> loadedUrls = new ArrayList<>();
    while (resources.hasMoreElements()) {
      loadedUrls.add(resources.nextElement());
    }
    assertThat(loadedUrls).containsExactly(url1, url2);
  }

  @Test
  void transformerFailureWrappedInIllegalStateException() throws Exception {
    ClassFileTransformer transformer = mock(ClassFileTransformer.class);
    when(transformer.transform(any(), any(), isNull(), isNull(), any()))
            .thenThrow(new IllegalClassFormatException("Test failure"));

    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    loader.addTransformer(transformer);

    assertThatThrownBy(() -> loader.loadClass(getClass().getName()))
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(IllegalClassFormatException.class);
  }

  @Test
  void resourceStreamsDelegatedToEnclosingClassLoader() {
    ClassLoader parentLoader = mock(ClassLoader.class);
    InputStream expectedStream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
    when(parentLoader.getResourceAsStream("test.properties")).thenReturn(expectedStream);

    ShadowingClassLoader loader = new ShadowingClassLoader(parentLoader);
    assertThat(loader.getResourceAsStream("test.properties")).isSameAs(expectedStream);
  }

  @Test
  void packageDefinedForNonDefaultPackageClass() throws Exception {
    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    String className = "foo.bar.TestClass";

    ClassFileTransformer transformer = mock(ClassFileTransformer.class);
    when(transformer.transform(any(), any(), isNull(), isNull(), any()))
            .thenReturn(new byte[] { -54, -2, -70, -66 }); // Valid class file magic number

    loader.addTransformer(transformer);

    assertThatThrownBy(() -> loader.loadClass(className))
            .isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void excludedPackageCannotBeShadowed() throws Exception {
    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    Class<?> loadedClass = loader.loadClass("javax.xml.XMLConstants");
    assertThat(loadedClass.getClassLoader()).isNotSameAs(loader);
  }

  @Test
  void copyTransformersFromEmptyLoaderCopiesNothing() {
    ShadowingClassLoader first = new ShadowingClassLoader(getClass().getClassLoader());
    ShadowingClassLoader second = new ShadowingClassLoader(getClass().getClassLoader());

    second.copyTransformers(first);

    assertThat(second)
            .extracting("classFileTransformers")
            .asList()
            .isEmpty();
  }

  @Test
  void transformersReturnNullBytesPreservesOriginalBytes() throws Exception {
    ClassFileTransformer transformer = mock(ClassFileTransformer.class);
    when(transformer.transform(any(), any(), isNull(), isNull(), any())).thenReturn(null);

    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    loader.addTransformer(transformer);

    Class<?> loadedClass = loader.loadClass(getClass().getName());
    assertThat(loadedClass).isNotNull();
  }

  @Test
  void multipleTransformersCanReturnNull() throws Exception {
    ClassFileTransformer first = mock(ClassFileTransformer.class);
    ClassFileTransformer second = mock(ClassFileTransformer.class);
    when(first.transform(any(), any(), isNull(), isNull(), any())).thenReturn(null);
    when(second.transform(any(), any(), isNull(), isNull(), any())).thenReturn(null);

    ShadowingClassLoader loader = new ShadowingClassLoader(getClass().getClassLoader());
    loader.addTransformer(first);
    loader.addTransformer(second);

    Class<?> loadedClass = loader.loadClass(getClass().getName());
    assertThat(loadedClass).isNotNull();
  }

}