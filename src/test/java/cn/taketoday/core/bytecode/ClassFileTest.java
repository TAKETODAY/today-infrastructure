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
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link ClassFile}.
 *
 * @author Eric Bruneton
 */
public class ClassFileTest extends AsmTest {

  @Test
  public void testGetConstantPoolDump() {
    ClassFile classFile = new ClassFile(PrecompiledClass.JDK3_ALL_INSTRUCTIONS.getBytes());

    String constantPoolDump = classFile.getConstantPoolDump();

    assertTrue(constantPoolDump.contains("constant_pool: ConstantClassInfo jdk3/AllInstructions"));
  }

  /** Tests that newInstance() succeeds for each precompiled class. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testNewInstance_validClass(
          final PrecompiledClass classParameter) {
    ClassFile classFile = new ClassFile(classParameter.getBytes());

    Executable newInstance = classFile::newInstance;

    if (classParameter.isNotCompatibleWithCurrentJdk()) {
      assertThrows(UnsupportedClassVersionError.class, newInstance);
    }
    else {
      assertDoesNotThrow(newInstance);
    }
  }

  /** Tests that newInstance() fails when trying to load an invalid or unverifiable class. */
  @ParameterizedTest
  @EnumSource(InvalidClass.class)
  public void testNewInstance_invalidClass(final InvalidClass invalidClass) {
    ClassFile classFile = new ClassFile(invalidClass.getBytes());

    Executable newInstance = () -> classFile.newInstance();

    assertThrows(ClassFormatException.class, newInstance);
  }

  /**
   * Tests that the static newInstance() method fails when trying to load an invalid or unverifiable
   * class.
   */
  @ParameterizedTest
  @EnumSource(InvalidClass.class)
  public void testStaticNewInstance_invalidClass(final InvalidClass invalidClass) {
    String className = invalidClass.toString();
    byte[] classContent = invalidClass.getBytes();

    Executable newInstance = () -> ClassFile.newInstance(className, classContent);

    switch (invalidClass) {
      case INVALID_ELEMENT_VALUE:
      case INVALID_TYPE_ANNOTATION_TARGET_TYPE:
      case INVALID_INSN_TYPE_ANNOTATION_TARGET_TYPE:
        break;
      case INVALID_BYTECODE_OFFSET:
      case INVALID_OPCODE:
      case INVALID_WIDE_OPCODE:
        assertThrows(VerifyError.class, newInstance);
        break;
      case INVALID_CLASS_VERSION:
      case INVALID_CODE_LENGTH:
      case INVALID_CONSTANT_POOL_INDEX:
      case INVALID_CONSTANT_POOL_REFERENCE:
      case INVALID_CP_INFO_TAG:
      case INVALID_SOURCE_DEBUG_EXTENSION:
      case INVALID_STACK_MAP_FRAME_TYPE:
      case INVALID_VERIFICATION_TYPE_INFO:
        assertThrows(ClassFormatError.class, newInstance);
        break;
      default:
        fail("Unknown invalid class");
        break;
    }
  }

  @Test
  public void testEquals() {
    ClassFile classFile1 = new ClassFile(PrecompiledClass.JDK3_ALL_INSTRUCTIONS.getBytes());
    ClassFile classFile2 = new ClassFile(PrecompiledClass.JDK5_ALL_INSTRUCTIONS.getBytes());

    boolean equalsThis = classFile1.equals(classFile1);
    boolean equalsDifferentClass = classFile1.equals(classFile2);
    boolean equalsInvalidClass = classFile1.equals(new byte[0]);

    assertTrue(equalsThis);
    assertFalse(equalsDifferentClass);
    assertFalse(equalsInvalidClass);
  }

  @Test
  public void testHashcode_validClass() {
    PrecompiledClass precompiledClass = PrecompiledClass.JDK3_ALL_INSTRUCTIONS;
    ClassFile classFile = new ClassFile(precompiledClass.getBytes());

    int hashCode = classFile.hashCode();

    assertNotEquals(0, hashCode);
  }

  @Test
  public void testHashcode_invalidClass() {
    InvalidClass invalidClass = InvalidClass.INVALID_CLASS_VERSION;
    ClassFile classFile = new ClassFile(invalidClass.getBytes());

    Executable hashCode = () -> classFile.hashCode();

    Exception exception = assertThrows(ClassFormatException.class, hashCode);
    assertEquals("Unsupported class version", exception.getMessage());
  }

  @Test
  public void testToString_validClass() {
    PrecompiledClass precompiledClass = PrecompiledClass.JDK3_ALL_INSTRUCTIONS;
    ClassFile classFile = new ClassFile(precompiledClass.getBytes());

    String classString = classFile.toString();

    assertTrue(classString.contains(precompiledClass.getInternalName()));
  }

  @Test
  public void testToString_invalidClass() {
    InvalidClass invalidClass = InvalidClass.INVALID_CLASS_VERSION;
    ClassFile classFile = new ClassFile(invalidClass.getBytes());

    Executable toString = () -> classFile.toString();

    Exception exception = assertThrows(ClassFormatException.class, toString);
    assertEquals("Unsupported class version", exception.getMessage());
  }
}
