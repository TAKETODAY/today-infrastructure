/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.bytecode.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import infra.bytecode.ClassVisitor;
import infra.bytecode.ClassWriter;
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
