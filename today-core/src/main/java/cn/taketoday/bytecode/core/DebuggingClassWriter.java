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
package cn.taketoday.bytecode.core;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.lang.TodayStrategies;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class DebuggingClassWriter extends ClassVisitor {

  public static final String DEBUG_LOCATION_PROPERTY = "bytecode.debugLocation";
  private static String debugLocation; //"/Users/today/temp";

  private String className;
  private String superName;

  static {
    debugLocation = TodayStrategies.getProperty(DEBUG_LOCATION_PROPERTY);
    if (debugLocation != null) {
      System.err.printf("CGLIB debugging enabled, writing to '%s'%n", debugLocation);
    }
  }

  public DebuggingClassWriter(int flags) {
    super(new ClassWriter(flags));
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.className = name;
    this.superName = superName;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public String getClassName() {
    return className;
  }

  public String getSuperName() {
    return superName;
  }

  public byte[] toByteArray() {
    byte[] b = ((ClassWriter) DebuggingClassWriter.super.cv).toByteArray();
    if (debugLocation != null) {
      debug(b);
    }
    return b;
  }

  public static void setDebugLocation(final String debugLocation) {
    DebuggingClassWriter.debugLocation = debugLocation;
  }

  private void debug(byte[] b) {
    this.className = className.replace('/', '.');
    this.superName = superName.replace('/', '.');
    String dirs = className.replace('.', File.separatorChar);
    try {
      new File(debugLocation + File.separatorChar + dirs).getParentFile().mkdirs();

      File file = new File(new File(debugLocation), dirs + ".class");
      try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
        out.write(b);
      }
    }
    catch (Exception e) {
      throw new CodeGenerationException(e);
    }
  }
}
