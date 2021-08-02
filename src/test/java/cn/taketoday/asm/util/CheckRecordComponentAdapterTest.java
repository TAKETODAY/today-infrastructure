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
import cn.taketoday.asm.TypeReference;
import cn.taketoday.asm.AsmTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link CheckRecordComponentAdapter}.
 *
 * @author Eric Bruneton
 */
public class CheckRecordComponentAdapterTest extends AsmTest implements Opcodes {

  @Test
  public void testConstructor() {
    assertDoesNotThrow(() -> new CheckRecordComponentAdapter(null));
  }

  @Test
  public void testVisitTypeAnnotation_illegalTypeAnnotation() {
    CheckRecordComponentAdapter checkRecordComponentAdapter = new CheckRecordComponentAdapter(null);

    Executable visitTypeAnnotation =
            () ->
                    checkRecordComponentAdapter.visitTypeAnnotation(
                            TypeReference.newFormalParameterReference(0).getValue(), null, "LA;", true);

    Exception exception = assertThrows(IllegalArgumentException.class, visitTypeAnnotation);
    assertEquals("Invalid type reference sort 0x16", exception.getMessage());
  }

  @Test
  public void testVisitAttribute_illegalAttribute() {
    CheckRecordComponentAdapter checkRecordComponentAdapter = new CheckRecordComponentAdapter(null);

    Executable visitAttribute = () -> checkRecordComponentAdapter.visitAttribute(null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitAttribute);
    assertEquals("Invalid attribute (must not be null)", exception.getMessage());
  }

  @Test
  public void testVisitAttribute_afterEnd() {
    CheckRecordComponentAdapter checkRecordComponentAdapter = new CheckRecordComponentAdapter(null);
    checkRecordComponentAdapter.visitEnd();

    Executable visitAttribute = () -> checkRecordComponentAdapter.visitAttribute(new cn.taketoday.asm.util.Comment());

    Exception exception = assertThrows(IllegalStateException.class, visitAttribute);
    assertEquals(
            "Cannot call a visit method after visitEnd has been called", exception.getMessage());
  }
}
