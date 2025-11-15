/*
 * Copyright 2017 - 2025 the original author or authors.
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
package infra.bytecode.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import infra.bytecode.AsmTest;
import infra.bytecode.Opcodes;
import infra.bytecode.TypeReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link CheckFieldAdapter}.
 *
 * @author Eric Bruneton
 */
public class CheckFieldAdapterTests extends AsmTest implements Opcodes {

  @Test
  public void testConstructor() {
    assertDoesNotThrow(() -> new CheckFieldAdapter(null));
  }

  @Test
  public void testVisitTypeAnnotation_illegalTypeAnnotation() {
    CheckFieldAdapter checkFieldAdapter = new CheckFieldAdapter(null);

    Executable visitTypeAnnotation =
            () ->
                    checkFieldAdapter.visitTypeAnnotation(
                            TypeReference.newFormalParameterReference(0).getValue(), null, "LA;", true);

    Exception exception = assertThrows(IllegalArgumentException.class, visitTypeAnnotation);
    assertEquals("Invalid type reference sort 0x16", exception.getMessage());
  }

  @Test
  public void testVisitAttribute_illegalAttribute() {
    CheckFieldAdapter checkFieldAdapter = new CheckFieldAdapter(null);

    Executable visitAttribute = () -> checkFieldAdapter.visitAttribute(null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitAttribute);
    assertEquals("Invalid attribute (must not be null)", exception.getMessage());
  }

  @Test
  public void testVisitAttribute_afterEnd() {
    CheckFieldAdapter checkFieldAdapter = new CheckFieldAdapter(null);
    checkFieldAdapter.visitEnd();

    Executable visitAttribute = () -> checkFieldAdapter.visitAttribute(new Comment());

    Exception exception = assertThrows(IllegalStateException.class, visitAttribute);
    assertEquals(
            "Cannot call a visit method after visitEnd has been called", exception.getMessage());
  }
}
