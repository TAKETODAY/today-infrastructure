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

package cn.taketoday.aot.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;

import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Assert;

/**
 * ASM {@link ClassFileTransformer} that delegates bytecode transformations
 * to a {@link InvocationsRecorderClassVisitor class visitor} if and only
 * if the class is in the list of packages considered for instrumentation.
 *
 * @author Brian Clozel
 * @see InvocationsRecorderClassVisitor
 */
class InvocationsRecorderClassTransformer implements ClassFileTransformer {

  private static final String AGENT_PACKAGE = InvocationsRecorderClassTransformer.class.getPackageName().replace('.', '/');

  private static final String AOT_DYNAMIC_CLASSLOADER = "cn/taketoday/aot/test/generate/compile/DynamicClassLoader";

  private final String[] instrumentedPackages;

  private final String[] ignoredPackages;

  public InvocationsRecorderClassTransformer(String[] instrumentedPackages, String[] ignoredPackages) {
    Assert.notNull(instrumentedPackages, "instrumentedPackages must not be null");
    Assert.notNull(ignoredPackages, "ignoredPackages must not be null");
    this.instrumentedPackages = rewriteToAsmFormat(instrumentedPackages);
    this.ignoredPackages = rewriteToAsmFormat(ignoredPackages);
  }

  private String[] rewriteToAsmFormat(String[] packages) {
    return Arrays.stream(packages).map(pack -> pack.replace('.', '/'))
            .toArray(String[]::new);
  }

  @Override
  public byte[] transform(@Nullable ClassLoader classLoader, String className, Class<?> classBeingRedefined,
          ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

    if (isTransformationCandidate(classLoader, className)) {
      return attemptClassTransformation(classfileBuffer);
    }
    return classfileBuffer;
  }

  private boolean isTransformationCandidate(@Nullable ClassLoader classLoader, String className) {
    // Ignore system classes
    if (classLoader == null) {
      return false;
    }
    // Ignore agent classes and today-core-test DynamicClassLoader
    else if (className.startsWith(AGENT_PACKAGE) || className.equals(AOT_DYNAMIC_CLASSLOADER)) {
      return false;
    }
    // Do not instrument CGlib classes
    else if (className.contains("$$")) {
      return false;
    }
    // Only some packages are instrumented
    else {
      for (String ignoredPackage : this.ignoredPackages) {
        if (className.startsWith(ignoredPackage)) {
          return false;
        }
      }
      for (String instrumentedPackage : this.instrumentedPackages) {
        if (className.startsWith(instrumentedPackage)) {
          return true;
        }
      }
    }
    return false;
  }

  private byte[] attemptClassTransformation(byte[] classfileBuffer) {
    ClassReader fileReader = new ClassReader(classfileBuffer);
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    try {
      fileReader.accept(classVisitor, 0);
    }
    catch (Exception ex) {
      ex.printStackTrace();
      return classfileBuffer;
    }
    if (classVisitor.isTransformed()) {
      return classVisitor.getTransformedClassBuffer();
    }
    return classfileBuffer;
  }
}
