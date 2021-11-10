/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.bytecode;

import cn.taketoday.lang.Nullable;

import java.security.ProtectionDomain;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author TODAY 2021/11/9 22:59
 */
public class ByteCodeClassLoader extends ClassLoader {
  private final ConcurrentHashMap<String, ClassBytes> bytesCache = new ConcurrentHashMap<>(256);

  public ByteCodeClassLoader(@Nullable ClassLoader parent) {
    super(parent);
  }

  public Class<?> loadClass(String className, byte[] b, ProtectionDomain domain) throws ClassNotFoundException {
    return loadClass(new ClassBytes(className, b, domain));
  }

  public static class ClassBytes {
    final byte[] bytes;
    final String className;
    final ProtectionDomain domain;

    public ClassBytes(String className, byte[] bytes, ProtectionDomain domain) {
      this.bytes = bytes;
      this.className = className;
      this.domain = domain;
    }
  }

  public Class<?> loadClass(ClassBytes classBytes) throws ClassNotFoundException {
    bytesCache.put(classBytes.className, classBytes);
    return loadClass(classBytes.className);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> result = loadClassFromBytes(name);
    if (result != null) {
      if (resolve) {
        resolveClass(result);
      }
      return result;
    }
    return super.loadClass(name, resolve);
  }

  protected Class<?> loadClassFromBytes(String name) {
    ClassBytes classBytes = bytesCache.get(name);
    if (classBytes == null) {
      return null;
    }
    else {
      byte[] bytes = classBytes.bytes;
      return defineClass(name, bytes, 0, bytes.length, classBytes.domain);
    }
  }

}
