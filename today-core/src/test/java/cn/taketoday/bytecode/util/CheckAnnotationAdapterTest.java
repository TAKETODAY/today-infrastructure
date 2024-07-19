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
package cn.taketoday.bytecode.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link CheckAnnotationAdapter}.
 *
 * @author Eric Bruneton
 */
public class CheckAnnotationAdapterTest extends AsmTest implements Opcodes {

  @Test
  public void testVisit_illegalAnnotationName() {
    CheckAnnotationAdapter checkAnnotationAdapter = new CheckAnnotationAdapter(null);

    Executable visit = () -> checkAnnotationAdapter.visit(null, Integer.valueOf(0));

    Exception exception = assertThrows(IllegalArgumentException.class, visit);
    assertEquals("Annotation value name is required", exception.getMessage());
  }

  @Test
  public void testVisit_illegalAnnotationValue1() {
    CheckAnnotationAdapter checkAnnotationAdapter = new CheckAnnotationAdapter(null);

    Executable visit = () -> checkAnnotationAdapter.visit("name", new Object());

    Exception exception = assertThrows(IllegalArgumentException.class, visit);
    assertEquals("Invalid annotation value", exception.getMessage());
  }

  @Test
  public void testVisit_illegalAnnotationValue2() {
    CheckAnnotationAdapter checkAnnotationAdapter = new CheckAnnotationAdapter(null);

    Executable visit = () -> checkAnnotationAdapter.visit("name", Type.forMethod("()V"));

    Exception exception = assertThrows(IllegalArgumentException.class, visit);
    assertEquals("Invalid annotation value", exception.getMessage());
  }

  @Test
  public void testVisit_afterEnd() {
    CheckAnnotationAdapter checkAnnotationAdapter = new CheckAnnotationAdapter(null);
    checkAnnotationAdapter.visitEnd();

    Executable visit = () -> checkAnnotationAdapter.visit("name", Integer.valueOf(0));

    Exception exception = assertThrows(IllegalStateException.class, visit);
    assertEquals(
            "Cannot call a visit method after visitEnd has been called", exception.getMessage());
  }

  @Test
  public void testVisitEnum_illegalAnnotationEnumValue() {
    CheckAnnotationAdapter checkAnnotationAdapter = new CheckAnnotationAdapter(null);

    Executable visitEnum = () -> checkAnnotationAdapter.visitEnum("name", "Lpkg/Enum;", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitEnum);
    assertEquals("Invalid enum value", exception.getMessage());
  }
}
