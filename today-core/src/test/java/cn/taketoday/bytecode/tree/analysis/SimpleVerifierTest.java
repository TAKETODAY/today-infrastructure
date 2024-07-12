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
package cn.taketoday.bytecode.tree.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cn.taketoday.bytecode.Type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SimpleVerifier}.
 *
 * @author Eric Bruneton
 */
public class SimpleVerifierTest {

  @Test
  public void testConstructor() {
    assertDoesNotThrow(() -> new SimpleVerifier());
  }

  @ParameterizedTest
  @CsvSource({
          "java/lang/String, java/lang/Number, java/lang/Object",
          "java/lang/Integer, java/lang/Number, java/lang/Number",
          "java/lang/Float, java/lang/Integer, java/lang/Number",
          "java/lang/Long, java/util/List, java/lang/Object",
          "java/util/Map, java/util/List, java/lang/Object"
  })
  public void testMerge_objectTypes(
          final String internalName1, final String internalName2, final String expectedInternalName) {
    BasicValue value1 = new BasicValue(Type.forInternalName(internalName1));
    BasicValue value2 = new BasicValue(Type.forInternalName(internalName2));
    SimpleVerifier verifier = new SimpleVerifier();

    BasicValue merge1 = verifier.merge(value1, value2);
    BasicValue merge2 = verifier.merge(value2, value1);

    BasicValue expectedValue = new BasicValue(Type.forInternalName(expectedInternalName));
    assertEquals(expectedValue, merge1);
    assertEquals(expectedValue, merge2);
  }

  @Test
  public void testIsAssignableFrom_subclassWithInterfaces() {
    Type baseType = Type.forInternalName("C");
    Type superType = Type.forInternalName("D");
    Type interfaceType = Type.forInternalName("I");
    SimpleVerifier simpleVerifier =
            new SimpleVerifier(
                    baseType,
                    superType,
                    false,
                    interfaceType) {

              @Override
              public boolean isAssignableFrom(final Type type1, final Type type2) {
                return super.isAssignableFrom(type1, type2);
              }

              @Override
              protected Class<?> getClass(final Type type) {
                // Return dummy classes, to make sure isAssignable in test() does not rely on them.
                if (type == baseType) {
                  return int.class;
                }
                if (type == superType) {
                  return float.class;
                }
                if (type == interfaceType) {
                  return double.class;
                }
                return super.getClass(type);
              }
            };

    assertTrue(simpleVerifier.isAssignableFrom(baseType, baseType));
    assertTrue(simpleVerifier.isAssignableFrom(superType, baseType));
    assertTrue(simpleVerifier.isAssignableFrom(interfaceType, baseType));
  }

  @Test
  public void testIsAssignableFrom_interface() {
    Type baseType = Type.forInternalName("C");
    Type interfaceType = Type.forInternalName("I");
    SimpleVerifier simpleVerifier =
            new SimpleVerifier(interfaceType, null, true) {

              @Override
              protected Type getSuperClass(final Type type) {
                return Type.forInternalName("java/lang/Object");
              }
            };

    assertTrue(simpleVerifier.isAssignableFrom(interfaceType, baseType));
    assertTrue(simpleVerifier.isAssignableFrom(interfaceType, Type.forInternalName("[I")));
    assertFalse(simpleVerifier.isAssignableFrom(interfaceType, Type.INT_TYPE));
  }
}
