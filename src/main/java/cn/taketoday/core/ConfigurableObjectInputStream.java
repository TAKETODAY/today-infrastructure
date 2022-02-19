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

package cn.taketoday.core;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 00:29
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Special {@link ObjectInputStream} subclass that resolves class names
 * against a specific {@link ClassLoader}.
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see cn.taketoday.core.serializer.DefaultDeserializer
 */
public class ConfigurableObjectInputStream extends ObjectInputStream {

  @Nullable
  private final ClassLoader classLoader;

  private final boolean acceptProxyClasses;


  /**
   * Create a new ConfigurableObjectInputStream for the given InputStream and ClassLoader.
   * @param in the InputStream to read from
   * @param classLoader the ClassLoader to use for loading local classes
   * @see java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
   */
  public ConfigurableObjectInputStream(InputStream in, @Nullable ClassLoader classLoader) throws IOException {
    this(in, classLoader, true);
  }

  /**
   * Create a new ConfigurableObjectInputStream for the given InputStream and ClassLoader.
   * @param in the InputStream to read from
   * @param classLoader the ClassLoader to use for loading local classes
   * @param acceptProxyClasses whether to accept deserialization of proxy classes
   * (may be deactivated as a security measure)
   * @see java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
   */
  public ConfigurableObjectInputStream(
          InputStream in, @Nullable ClassLoader classLoader, boolean acceptProxyClasses) throws IOException {

    super(in);
    this.classLoader = classLoader;
    this.acceptProxyClasses = acceptProxyClasses;
  }


  @Override
  protected Class<?> resolveClass(ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
    try {
      if (this.classLoader != null) {
        // Use the specified ClassLoader to resolve local classes.
        return ClassUtils.forName(classDesc.getName(), this.classLoader);
      }
      else {
        // Use the default ClassLoader...
        return super.resolveClass(classDesc);
      }
    }
    catch (ClassNotFoundException ex) {
      return resolveFallbackIfPossible(classDesc.getName(), ex);
    }
  }

  @Override
  protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
    if (!this.acceptProxyClasses) {
      throw new NotSerializableException("Not allowed to accept serialized proxy classes");
    }
    if (this.classLoader != null) {
      // Use the specified ClassLoader to resolve local proxy classes.
      Class<?>[] resolvedInterfaces = new Class<?>[interfaces.length];
      for (int i = 0; i < interfaces.length; i++) {
        try {
          resolvedInterfaces[i] = ClassUtils.forName(interfaces[i], this.classLoader);
        }
        catch (ClassNotFoundException ex) {
          resolvedInterfaces[i] = resolveFallbackIfPossible(interfaces[i], ex);
        }
      }
      try {
        return ClassUtils.createCompositeInterface(resolvedInterfaces, this.classLoader);
      }
      catch (IllegalArgumentException ex) {
        throw new ClassNotFoundException(null, ex);
      }
    }
    else {
      // Use ObjectInputStream's default ClassLoader...
      try {
        return super.resolveProxyClass(interfaces);
      }
      catch (ClassNotFoundException ex) {
        Class<?>[] resolvedInterfaces = new Class<?>[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
          resolvedInterfaces[i] = resolveFallbackIfPossible(interfaces[i], ex);
        }
        return ClassUtils.createCompositeInterface(resolvedInterfaces, getFallbackClassLoader());
      }
    }
  }


  /**
   * Resolve the given class name against a fallback class loader.
   * <p>The default implementation simply rethrows the original exception,
   * since there is no fallback available.
   * @param className the class name to resolve
   * @param ex the original exception thrown when attempting to load the class
   * @return the newly resolved class (never {@code null})
   */
  protected Class<?> resolveFallbackIfPossible(String className, ClassNotFoundException ex)
          throws IOException, ClassNotFoundException{

    throw ex;
  }

  /**
   * Return the fallback ClassLoader to use when no ClassLoader was specified
   * and ObjectInputStream's own default class loader failed.
   * <p>The default implementation simply returns {@code null}, indicating
   * that no specific fallback is available.
   */
  @Nullable
  protected ClassLoader getFallbackClassLoader() throws IOException {
    return null;
  }

}

