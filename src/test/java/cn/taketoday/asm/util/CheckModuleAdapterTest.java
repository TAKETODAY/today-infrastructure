// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package cn.taketoday.asm.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import cn.taketoday.asm.Opcodes;

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
