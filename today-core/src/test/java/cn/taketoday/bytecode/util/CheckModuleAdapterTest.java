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
package cn.taketoday.bytecode.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import cn.taketoday.bytecode.Opcodes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link CheckModuleAdapter}.
 *
 * @author Eric Bruneton
 */
public class CheckModuleAdapterTest {

  @Test
  public void testConstructor() {
    assertDoesNotThrow(() -> new CheckModuleAdapter(null, /* open = */ false));
  }

  @Test // see issue #317804
  public void testVisitRequire_javaBaseTransitive() {
    CheckModuleAdapter checkModuleAdapter = new CheckModuleAdapter(null, /* open = */ false);
    checkModuleAdapter.classVersion = Opcodes.V10;

    Executable visitRequire =
            () -> checkModuleAdapter.visitRequire("java.base", Opcodes.ACC_TRANSITIVE, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitRequire);
    assertEquals(
            "Invalid access flags: 32 java.base can not be declared ACC_TRANSITIVE or ACC_STATIC_PHASE",
            exception.getMessage());
  }

  @Test // see issue #317804
  public void testVisitRequire_javaBaseStaticPhase() {
    CheckModuleAdapter checkModuleAdapter = new CheckModuleAdapter(null, /* open = */ false);
    checkModuleAdapter.classVersion = Opcodes.V10;

    Executable visitRequire =
            () -> checkModuleAdapter.visitRequire("java.base", Opcodes.ACC_STATIC_PHASE, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitRequire);
    assertEquals(
            "Invalid access flags: 64 java.base can not be declared ACC_TRANSITIVE or ACC_STATIC_PHASE",
            exception.getMessage());
  }

  @Test // see issue #317804
  public void testVisitRequire_javaBaseTransitiveAndStaticPhase() {
    CheckModuleAdapter checkModuleAdapter = new CheckModuleAdapter(null, /* open = */ false);
    checkModuleAdapter.classVersion = Opcodes.V10;

    Executable visitRequire =
            () ->
                    checkModuleAdapter.visitRequire(
                            "java.base", Opcodes.ACC_TRANSITIVE | Opcodes.ACC_STATIC_PHASE, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitRequire);
    assertEquals(
            "Invalid access flags: 96 java.base can not be declared ACC_TRANSITIVE or ACC_STATIC_PHASE",
            exception.getMessage());
  }

  @Test // see issue #317804
  public void testVisitRequire_javaBaseTransitiveOrStaticPhaseAreIgnoredUnderJvms9() {
    CheckModuleAdapter checkModuleAdapter = new CheckModuleAdapter(null, /* open = */ false);
    checkModuleAdapter.classVersion = Opcodes.V9;

    Executable visitRequire =
            () ->
                    checkModuleAdapter.visitRequire(
                            "java.base", Opcodes.ACC_TRANSITIVE | Opcodes.ACC_STATIC_PHASE, null);

    assertDoesNotThrow(visitRequire);
  }

  @Test
  public void testVisitExport_nullArray() {
    CheckModuleAdapter checkModuleAdapter = new CheckModuleAdapter(null, /* open = */ false);

    Executable visitExport = () -> checkModuleAdapter.visitExport("package", 0, (String[]) null);

    assertDoesNotThrow(visitExport);
  }

  @Test
  public void testVisitOpen_nullArray() {
    CheckModuleAdapter checkModuleAdapter = new CheckModuleAdapter(null, /* open = */ false);

    Executable visitOpen = () -> checkModuleAdapter.visitOpen("package", 0, (String[]) null);

    assertDoesNotThrow(visitOpen);
  }

  @Test
  public void testVisitOpen_openModule() {
    CheckModuleAdapter checkModuleAdapter = new CheckModuleAdapter(null, /* open = */ true);

    Executable visitOpen = () -> checkModuleAdapter.visitOpen("package", 0, (String[]) null);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitOpen);
    assertEquals("An open module can not use open directive", exception.getMessage());
  }

  @Test
  public void testVisitUse_nameAlreadyDeclared() {
    CheckModuleAdapter checkModuleAdapter = new CheckModuleAdapter(null, /* open = */ false);
    checkModuleAdapter.visitUse("service");

    Executable visitUse = () -> checkModuleAdapter.visitUse("service");

    Exception exception = assertThrows(IllegalArgumentException.class, visitUse);
    assertEquals("Module uses 'service' already declared", exception.getMessage());
  }

  @Test
  public void testVisitUse_afterEnd() {
    CheckModuleAdapter checkModuleAdapter = new CheckModuleAdapter(null, /* open = */ false);
    checkModuleAdapter.visitEnd();

    Executable visitUse = () -> checkModuleAdapter.visitUse("service");

    Exception exception = assertThrows(IllegalStateException.class, visitUse);
    assertEquals(
            "Cannot call a visit method after visitEnd has been called", exception.getMessage());
  }

  @Test
  public void testVisitProvide_nullProviderList() {
    CheckModuleAdapter checkModuleAdapter = new CheckModuleAdapter(null, /* open = */ false);

    Executable visitProvide = () -> checkModuleAdapter.visitProvide("service2", (String[]) null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitProvide);
    assertEquals("Providers cannot be null or empty", exception.getMessage());
  }

  @Test
  public void testVisitProvide_emptyProviderList() {
    CheckModuleAdapter checkModuleAdapter = new CheckModuleAdapter(null, /* open = */ false);

    Executable visitProvide = () -> checkModuleAdapter.visitProvide("service1");

    Exception exception = assertThrows(IllegalArgumentException.class, visitProvide);
    assertEquals("Providers cannot be null or empty", exception.getMessage());
  }
}
