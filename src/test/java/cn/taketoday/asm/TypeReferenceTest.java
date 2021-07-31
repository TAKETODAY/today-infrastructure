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
package cn.taketoday.asm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link TypeReference}.
 *
 * @author Eric Bruneton
 */
public class TypeReferenceTest {

  @Test
  public void testNewTypeReference() {
    TypeReference typeReference = TypeReference.newTypeReference(TypeReference.FIELD);

    assertEquals(TypeReference.FIELD, typeReference.getSort());
    assertEquals(TypeReference.FIELD << 24, typeReference.getValue());
  }

  @Test
  public void testNewTypeParameterReference() {
    TypeReference typeReference =
        TypeReference.newTypeParameterReference(TypeReference.CLASS_TYPE_PARAMETER, 3);

    assertEquals(TypeReference.CLASS_TYPE_PARAMETER, typeReference.getSort());
    assertEquals(3, typeReference.getTypeParameterIndex());
  }

  @Test
  public void testNewTypeParameterBoundReference() {
    TypeReference typeReference =
        TypeReference.newTypeParameterBoundReference(TypeReference.CLASS_TYPE_PARAMETER, 3, 7);

    assertEquals(TypeReference.CLASS_TYPE_PARAMETER, typeReference.getSort());
    assertEquals(3, typeReference.getTypeParameterIndex());
    assertEquals(7, typeReference.getTypeParameterBoundIndex());
  }

  @Test
  public void testNewSuperTypeReference() {
    TypeReference typeReference = TypeReference.newSuperTypeReference(-1);

    assertEquals(TypeReference.CLASS_EXTENDS, typeReference.getSort());
    assertEquals(-1, typeReference.getSuperTypeIndex());
  }

  @Test
  public void testNewFormalParameterReference() {
    TypeReference typeReference = TypeReference.newFormalParameterReference(3);

    assertEquals(TypeReference.METHOD_FORMAL_PARAMETER, typeReference.getSort());
    assertEquals(3, typeReference.getFormalParameterIndex());
  }

  @Test
  public void testNewExceptionReference() {
    TypeReference typeReference = TypeReference.newExceptionReference(3);

    assertEquals(TypeReference.THROWS, typeReference.getSort());
    assertEquals(3, typeReference.getExceptionIndex());
  }

  @Test
  public void testNewTryCatchReference() {
    TypeReference typeReference = TypeReference.newTryCatchReference(3);

    assertEquals(TypeReference.EXCEPTION_PARAMETER, typeReference.getSort());
    assertEquals(3, typeReference.getTryCatchBlockIndex());
  }

  @Test
  public void testNewTypeArgumentReference() {
    TypeReference typeReference = TypeReference.newTypeArgumentReference(TypeReference.CAST, 3);

    assertEquals(TypeReference.CAST, typeReference.getSort());
    assertEquals(3, typeReference.getTypeArgumentIndex());
  }
}
