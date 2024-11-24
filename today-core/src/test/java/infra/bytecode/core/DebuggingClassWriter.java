/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.bytecode.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import infra.bytecode.ClassVisitor;
import infra.bytecode.ClassWriter;
import infra.bytecode.core.CodeGenerationException;
import infra.lang.TodayStrategies;

public class DebuggingClassWriter extends ClassVisitor {

  public static final String DEBUG_LOCATION_PROPERTY = "bytecode.debugLocation";

  private static final String debugLocation = TodayStrategies.getProperty(DEBUG_LOCATION_PROPERTY);

  private String className;

  private String superName;

  public DebuggingClassWriter(int flags) {
    super(new ClassWriter(flags));
  }

  public DebuggingClassWriter(ClassWriter classWriter) {
    super(classWriter);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.className = name;
    this.superName = superName;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public byte[] toByteArray() {
    byte[] b = ((ClassWriter) cv).toByteArray();
    if (debugLocation != null) {
      debug(b, debugLocation);
    }
    return b;
  }

  private void debug(byte[] b, String debugLocation) {
    this.className = className.replace('/', '.');
    this.superName = superName.replace('/', '.');
    String dirs = className.replace('.', File.separatorChar);
    try {
      new File(debugLocation + File.separatorChar + dirs).getParentFile().mkdirs();

      File file = new File(debugLocation, dirs + ".class");
      try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
        out.write(b);
      }
    }
    catch (Exception e) {
      throw new CodeGenerationException(e);
    }
  }
}
