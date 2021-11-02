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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Unit tests for {@link MethodWriter}.
 *
 * @author Eric Bruneton
 */
class MethodWriterTest {

  /**
   * Tests that the attribute name fields of Constants are the expected ones. This test is designed
   * to fail each time new attributes are added to Constants, and serves as a reminder to update the
   * {@link MethodWriter#canCopyMethodAttributes} method, if needed.
   */
  @Test
  void testCanCopyMethodAttributesUpdated() {
    Set<Object> actualAttributes =
            Arrays.stream(Constants.class.getDeclaredFields())
                    .filter(field -> field.getType() == String.class)
                    .map(field -> {
                      try {
                        return field.get(null);
                      }
                      catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new RuntimeException("Can't get field value", e);
                      }
                    })
                    .collect(toSet());

    HashSet<String> expectedAttributes =
            new HashSet<String>(
                    Arrays.asList(
                            Constants.CONSTANT_VALUE,
                            Constants.CODE,
                            Constants.STACK_MAP_TABLE,
                            Constants.EXCEPTIONS,
                            Constants.INNER_CLASSES,
                            Constants.ENCLOSING_METHOD,
                            Constants.SYNTHETIC,
                            Constants.SIGNATURE,
                            Constants.SOURCE_FILE,
                            Constants.SOURCE_DEBUG_EXTENSION,
                            Constants.LINE_NUMBER_TABLE,
                            Constants.LOCAL_VARIABLE_TABLE,
                            Constants.LOCAL_VARIABLE_TYPE_TABLE,
                            Constants.DEPRECATED,
                            Constants.RUNTIME_VISIBLE_ANNOTATIONS,
                            Constants.RUNTIME_INVISIBLE_ANNOTATIONS,
                            Constants.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS,
                            Constants.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS,
                            Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS,
                            Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS,
                            Constants.ANNOTATION_DEFAULT,
                            Constants.BOOTSTRAP_METHODS,
                            Constants.METHOD_PARAMETERS,
                            Constants.MODULE,
                            Constants.MODULE_PACKAGES,
                            Constants.MODULE_MAIN_CLASS,
                            Constants.NEST_HOST,
                            Constants.NEST_MEMBERS,
                            Constants.PERMITTED_SUBCLASSES,
                            Constants.RECORD));
    // IMPORTANT: if this fails, update the list AND update MethodWriter.canCopyMethodAttributes(),
    // if needed.
    assertEquals(expectedAttributes, actualAttributes);
  }

  @Test
  void testRecursiveCondyFastEnough() {
    Handle bsm =
            new Handle(Opcodes.H_INVOKESTATIC,
                    "RT",
                    "bsm",
                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;I)Ljava/lang/invoke/CallSite;",
                    false);
    ConstantDynamic chain = new ConstantDynamic("condy", "I", bsm, 0);
    for (int i = 0; i < 32; i++) {
      chain = new ConstantDynamic("condy" + i, "I", bsm, chain);
    }
    ConstantDynamic condy = chain;

    assertTimeoutPreemptively(Duration.ofMillis(1_000), () -> {
      ClassWriter classWriter = new ClassWriter(0);
      classWriter.visit(Opcodes.V11, Opcodes.ACC_SUPER, "Test", null, "java/lang/Object", null);
      MethodVisitor mv = classWriter.visitMethod(Opcodes.ACC_STATIC, "m", "()V", null, null);
      mv.visitCode();
      mv.visitLdcInsn(condy);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
      classWriter.visitEnd();
      classWriter.toByteArray();
    });
  }
}
