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

package infra.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

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

}