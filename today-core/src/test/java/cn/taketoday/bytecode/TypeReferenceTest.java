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
package cn.taketoday.bytecode;

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
