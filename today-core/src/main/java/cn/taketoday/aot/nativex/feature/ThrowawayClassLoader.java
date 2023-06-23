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

package cn.taketoday.aot.nativex.feature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * {@link ClassLoader} used to load classes without causing build-time
 * initialization.
 *
 * @author Phillip Webb
 * @since 4.0
 */
class ThrowawayClassLoader extends ClassLoader {

  static {
    registerAsParallelCapable();
  }

  private final ClassLoader resourceLoader;

  ThrowawayClassLoader(ClassLoader parent) {
    super(parent.getParent());
    this.resourceLoader = parent;
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized(getClassLoadingLock(name)) {
      Class<?> loaded = findLoadedClass(name);
      if (loaded != null) {
        return loaded;
      }
      try {
        return super.loadClass(name, true);
      }
      catch (ClassNotFoundException ex) {
        return loadClassFromResource(name);
      }
    }
  }

  private Class<?> loadClassFromResource(String name) throws ClassNotFoundException, ClassFormatError {
    String resourceName = name.replace('.', '/') + ".class";
    InputStream inputStream = this.resourceLoader.getResourceAsStream(resourceName);
    if (inputStream == null) {
      return null;
    }
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      inputStream.transferTo(outputStream);
      byte[] bytes = outputStream.toByteArray();
      return defineClass(name, bytes, 0, bytes.length);

    }
    catch (IOException ex) {
      throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
    }
  }

  @Override
  protected URL findResource(String name) {
    return this.resourceLoader.getResource(name);
  }

}
