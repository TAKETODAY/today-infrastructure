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
package cn.taketoday.bytecode.transform;

import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;

import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.core.ClassGenerator;
import cn.taketoday.bytecode.core.CodeGenerationException;
import cn.taketoday.bytecode.core.DebuggingClassWriter;
import cn.taketoday.lang.NonNull;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author Today <br>
 * 2018-11-08 15:07
 */
@SuppressWarnings({ "rawtypes" })
public abstract class AbstractClassLoader extends ClassLoader {

  private final ClassFilter filter;
  private final ClassLoader classPath;
  private static final ProtectionDomain DOMAIN = ReflectionUtils.getProtectionDomain(AbstractClassLoader.class);

  protected AbstractClassLoader(ClassLoader parent, ClassLoader classPath, ClassFilter filter) {
    super(parent);
    this.filter = filter;
    this.classPath = classPath;
  }

  @Override
  public Class loadClass(String name) throws ClassNotFoundException {
    Class loaded = findLoadedClass(name);

    if (loaded != null && loaded.getClassLoader() == this) {
      return loaded;
    } // else reload with this class loader

    if (!filter.accept(name)) {
      return super.loadClass(name);
    }
    ClassReader reader = getClassReader(name);
    try {
      DebuggingClassWriter w = new DebuggingClassWriter(ClassWriter.COMPUTE_FRAMES);
      getGenerator(reader).generateClass(w);
      byte[] b = w.toByteArray();
      Class c = super.defineClass(name, b, 0, b.length, DOMAIN);
      postProcess(c);
      return c;
    }
    catch (RuntimeException | Error e) {
      throw e;
    }
    catch (Exception e) {
      throw new CodeGenerationException(e);
    }
  }

  @NonNull
  private ClassReader getClassReader(String name) throws ClassNotFoundException {
    try {
      try (InputStream is = classPath.getResourceAsStream(name.replace('.', '/').concat(".class"))) {
        if (is == null) {
          throw new ClassNotFoundException(name);
        }
        return new ClassReader(is);
      }
    }
    catch (IOException e) {
      throw new ClassNotFoundException(name + ':' + e.getMessage());
    }
  }

  protected ClassGenerator getGenerator(ClassReader r) {
    return new ClassReaderGenerator(r, attributes(), getFlags());
  }

  protected int getFlags() {
    return ClassReader.EXPAND_FRAMES;
  }

  protected Attribute[] attributes() {
    return null;
  }

  protected void postProcess(Class c) { }
}
