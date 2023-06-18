/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.test.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.Function;

import cn.taketoday.lang.Nullable;

/**
 * {@link ClassLoader} implementation to support
 * {@link CompileWithForkedClassLoader @CompileWithForkedClassLoader}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 4.0
 */
final class CompileWithForkedClassLoaderClassLoader extends ClassLoader {

  private final ClassLoader testClassLoader;

  private Function<String, byte[]> classResourceLookup = name -> null;

  public CompileWithForkedClassLoaderClassLoader(ClassLoader testClassLoader) {
    super(testClassLoader.getParent());
    this.testClassLoader = testClassLoader;
  }

  // Invoked reflectively by DynamicClassLoader
  @SuppressWarnings("unused")
  void setClassResourceLookup(Function<String, byte[]> classResourceLookup) {
    this.classResourceLookup = classResourceLookup;
  }

  // Invoked reflectively by DynamicClassLoader
  @SuppressWarnings("unused")
  Class<?> defineDynamicClass(String name, byte[] b, int off, int len) {
    return super.defineClass(name, b, off, len);
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    if (name.startsWith("org.junit") || name.startsWith("org.testng")) {
      return Class.forName(name, false, this.testClassLoader);
    }
    return super.loadClass(name);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    byte[] bytes = findClassBytes(name);
    return (bytes != null) ? defineClass(name, bytes, 0, bytes.length, null) : super.findClass(name);
  }

  @Nullable
  private byte[] findClassBytes(String name) {
    byte[] bytes = this.classResourceLookup.apply(name);
    if (bytes != null) {
      return bytes;
    }
    String resourceName = name.replace(".", "/") + ".class";
    InputStream stream = this.testClassLoader.getResourceAsStream(resourceName);
    if (stream != null) {
      try (stream) {
        return stream.readAllBytes();
      }
      catch (IOException ex) {
        // ignore
      }
    }
    return null;
  }

  @Override
  protected Enumeration<URL> findResources(String name) throws IOException {
    return this.testClassLoader.getResources(name);
  }

  @Override
  @Nullable
  protected URL findResource(String name) {
    return this.testClassLoader.getResource(name);
  }

}
