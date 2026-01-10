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

package infra.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 17:48
 */
class ConfigurableObjectInputStreamTests {

  @Test
  void resolveClassWithSpecificClassLoader() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ClassLoader classLoader = getClass().getClassLoader();
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, classLoader);

    assertThatCode(() -> ois.readObject()).doesNotThrowAnyException();
  }

  @Test
  void resolveClassWithNullClassLoader() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, null);

    assertThatCode(ois::readObject).doesNotThrowAnyException();
  }

  @Test
  void constructWithInvalidInputStream() {
    assertThatThrownBy(() -> new ConfigurableObjectInputStream(null, null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void resolveProxyClassThrowsClassNotFoundExceptionWithNonExistingInterface() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, getClass().getClassLoader());

    assertThatThrownBy(() -> ois.resolveProxyClass(new String[] { "non.existing.Interface" }))
            .isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void resolveFallbackIfPossibleThrowsOriginalException() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, null);
    ClassNotFoundException original = new ClassNotFoundException("test");

    assertThatThrownBy(() -> ois.resolveFallbackIfPossible("test", original))
            .isSameAs(original);
  }

  @Test
  void getFallbackClassLoaderReturnsNull() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, null);

    assertThat(ois.getFallbackClassLoader()).isNull();
  }

  @Test
  void resolveProxyClassWithAcceptProxyClassesFalse() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, null, false);

    assertThatThrownBy(() -> ois.resolveProxyClass(new String[] { Runnable.class.getName() }))
            .isInstanceOf(NotSerializableException.class)
            .hasMessage("Not allowed to accept serialized proxy classes");
  }

  @Test
  void resolveProxyClassWithSpecificClassLoader() throws IOException, ClassNotFoundException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, getClass().getClassLoader());

    String[] interfaces = { Runnable.class.getName() };
    Class<?> proxyClass = ois.resolveProxyClass(interfaces);

    assertThat(proxyClass)
            .isNotNull()
            .isInstanceOf(Class.class);
    assertThat(proxyClass.getInterfaces()).contains(Runnable.class);
  }

  @Test
  void resolveProxyClassWithNullClassLoaderFallsBackToDefault() throws IOException, ClassNotFoundException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, null);

    String[] interfaces = { Runnable.class.getName() };
    Class<?> proxyClass = ois.resolveProxyClass(interfaces);

    assertThat(proxyClass)
            .isNotNull()
            .isInstanceOf(Class.class);
    assertThat(proxyClass.getInterfaces()).contains(Runnable.class);
  }

  @Test
  void resolveClassWithMultipleInterfaces() throws IOException, ClassNotFoundException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, getClass().getClassLoader());

    String[] interfaces = { Runnable.class.getName(), AutoCloseable.class.getName() };
    Class<?> proxyClass = ois.resolveProxyClass(interfaces);

    assertThat(proxyClass.getInterfaces())
            .contains(Runnable.class, AutoCloseable.class);
  }

  @Test
  void resolveProxyClassWithInvalidInterfacesThrowsClassNotFoundException() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, getClass().getClassLoader());

    String[] interfaces = {};
    assertThatThrownBy(() -> ois.resolveProxyClass(interfaces))
            .isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void resolveClassWithCustomFallbackClassLoader() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ClassLoader fallbackLoader = new ClassLoader() { };
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, null) {
      @Override
      protected ClassLoader getFallbackClassLoader() {
        return fallbackLoader;
      }
    };

    assertThat(ois.getFallbackClassLoader()).isSameAs(fallbackLoader);
  }

  @Test
  void resolveClassWithValidClassAndClassLoader() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("testString");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, classLoader);

    Object result = ois.readObject();
    assertThat(result).isInstanceOf(String.class).isEqualTo("testString");
  }

  @Test
  void resolveClassUsesFallbackWhenPrimaryClassLoaderFails() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ClassLoader failingClassLoader = new ClassLoader() {
      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
      }
    };

    ClassLoader fallbackClassLoader = Thread.currentThread().getContextClassLoader();
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, failingClassLoader) {
      @Override
      protected Class<?> resolveFallbackIfPossible(String className, ClassNotFoundException ex) throws ClassNotFoundException {
        return ClassUtils.forName(className, fallbackClassLoader);
      }
    };

    assertThatCode(ois::readObject).doesNotThrowAnyException();
  }

  @Test
  void constructorWithDisabledProxyClassesRejectsProxyDeserialization() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, null, false);

    assertThatThrownBy(() -> ois.resolveProxyClass(new String[] { "java.lang.Runnable" }))
            .isInstanceOf(NotSerializableException.class)
            .hasMessage("Not allowed to accept serialized proxy classes");
  }

  @Test
  void getFallbackClassLoaderReturnsNullByDefault() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, null);

    assertThat(ois.getFallbackClassLoader()).isNull();
  }

  @Test
  void resolveFallbackIfPossibleRethrowsOriginalExceptionByDefault() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject("test");
    oos.close();

    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    ConfigurableObjectInputStream ois = new ConfigurableObjectInputStream(in, null);
    ClassNotFoundException originalException = new ClassNotFoundException("test class");

    assertThatThrownBy(() -> ois.resolveFallbackIfPossible("test.ClassName", originalException))
            .isSameAs(originalException);
  }

}