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

package cn.taketoday.expression.spel;

import org.junit.jupiter.api.Test;

import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.SimpleEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test construction of arrays.
 *
 * @author Andy Clement
 */
public class ArrayConstructorTests extends AbstractExpressionTests {

  @Test
  public void simpleArrayWithInitializer() {
    evaluateArrayBuildingExpression("new int[]{1,2,3}", "[1,2,3]");
    evaluateArrayBuildingExpression("new int[]{}", "[]");
    evaluate("new int[]{}.length", "0", Integer.class);
  }

  @Test
  public void conversion() {
    evaluate("new String[]{1,2,3}[0]", "1", String.class);
    evaluate("new int[]{'123'}[0]", 123, Integer.class);
  }

  @Test
  public void multidimensionalArrays() {
    evaluateAndCheckError("new int[][]{{1,2},{3,4}}", SpelMessage.MULTIDIM_ARRAY_INITIALIZER_NOT_SUPPORTED);
    evaluateAndCheckError("new int[3][]", SpelMessage.MISSING_ARRAY_DIMENSION);
    evaluateAndCheckError("new int[]", SpelMessage.MISSING_ARRAY_DIMENSION);
    evaluateAndCheckError("new String[]", SpelMessage.MISSING_ARRAY_DIMENSION);
    evaluateAndCheckError("new int[][1]", SpelMessage.MISSING_ARRAY_DIMENSION);
  }

  @Test
  void noArrayConstruction() {
    EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();
    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
            parser.parseExpression("new int[2]").getValue(context));
  }

  @Test
  public void primitiveTypeArrayConstructors() {
    evaluateArrayBuildingExpression("new int[]{1,2,3,4}", "[1,2,3,4]");
    evaluateArrayBuildingExpression("new boolean[]{true,false,true}", "[true,false,true]");
    evaluateArrayBuildingExpression("new char[]{'a','b','c'}", "[a,b,c]");
    evaluateArrayBuildingExpression("new long[]{1,2,3,4,5}", "[1,2,3,4,5]");
    evaluateArrayBuildingExpression("new short[]{2,3,4,5,6}", "[2,3,4,5,6]");
    evaluateArrayBuildingExpression("new double[]{1d,2d,3d,4d}", "[1.0,2.0,3.0,4.0]");
    evaluateArrayBuildingExpression("new float[]{1f,2f,3f,4f}", "[1.0,2.0,3.0,4.0]");
    evaluateArrayBuildingExpression("new byte[]{1,2,3,4}", "[1,2,3,4]");
  }

  @Test
  public void primitiveTypeArrayConstructorsElements() {
    evaluate("new int[]{1,2,3,4}[0]", 1, Integer.class);
    evaluate("new boolean[]{true,false,true}[0]", true, Boolean.class);
    evaluate("new char[]{'a','b','c'}[0]", 'a', Character.class);
    evaluate("new long[]{1,2,3,4,5}[0]", 1L, Long.class);
    evaluate("new short[]{2,3,4,5,6}[0]", (short) 2, Short.class);
    evaluate("new double[]{1d,2d,3d,4d}[0]", (double) 1, Double.class);
    evaluate("new float[]{1f,2f,3f,4f}[0]", (float) 1, Float.class);
    evaluate("new byte[]{1,2,3,4}[0]", (byte) 1, Byte.class);
    evaluate("new String(new char[]{'h','e','l','l','o'})", "hello", String.class);
  }

  @Test
  public void errorCases() {
    evaluateAndCheckError("new char[7]{'a','c','d','e'}", SpelMessage.INITIALIZER_LENGTH_INCORRECT);
    evaluateAndCheckError("new char[3]{'a','c','d','e'}", SpelMessage.INITIALIZER_LENGTH_INCORRECT);
    evaluateAndCheckError("new char[2]{'hello','world'}", SpelMessage.TYPE_CONVERSION_ERROR);
    evaluateAndCheckError("new String('a','c','d')", SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM);
  }

  @Test
  public void typeArrayConstructors() {
    evaluate("new String[]{'a','b','c','d'}[1]", "b", String.class);
    evaluateAndCheckError("new String[]{'a','b','c','d'}.size()", SpelMessage.METHOD_NOT_FOUND, 30, "size()",
            "java.lang.String[]");
    evaluate("new String[]{'a','b','c','d'}.length", 4, Integer.class);
  }

  @Test
  public void basicArray() {
    evaluate("new String[3]", "java.lang.String[3]{null,null,null}", String[].class);
  }

  @Test
  public void multiDimensionalArray() {
    evaluate("new String[2][2]", "[Ljava.lang.String;[2]{[2]{null,null},[2]{null,null}}", String[][].class);
    evaluate("new String[3][2][1]",
            "[[Ljava.lang.String;[3]{[2]{[1]{null},[1]{null}},[2]{[1]{null},[1]{null}},[2]{[1]{null},[1]{null}}}",
            String[][][].class);
  }

  @Test
  public void constructorInvocation03() {
    evaluateAndCheckError("new String[]", SpelMessage.MISSING_ARRAY_DIMENSION);
  }

  public void constructorInvocation04() {
    evaluateAndCheckError("new Integer[3]{'3','ghi','5'}", SpelMessage.INCORRECT_ELEMENT_TYPE_FOR_ARRAY, 4);
  }

  private String evaluateArrayBuildingExpression(String expression, String expectedToString) {
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression e = parser.parseExpression(expression);
    Object o = e.getValue();
    assertThat(o).isNotNull();
    assertThat(o.getClass().isArray()).isTrue();
    StringBuilder s = new StringBuilder();
    s.append('[');
    if (o instanceof int[]) {
      int[] array = (int[]) o;
      for (int i = 0; i < array.length; i++) {
        if (i > 0) {
          s.append(',');
        }
        s.append(array[i]);
      }
    }
    else if (o instanceof boolean[]) {
      boolean[] array = (boolean[]) o;
      for (int i = 0; i < array.length; i++) {
        if (i > 0) {
          s.append(',');
        }
        s.append(array[i]);
      }
    }
    else if (o instanceof char[]) {
      char[] array = (char[]) o;
      for (int i = 0; i < array.length; i++) {
        if (i > 0) {
          s.append(',');
        }
        s.append(array[i]);
      }
    }
    else if (o instanceof long[]) {
      long[] array = (long[]) o;
      for (int i = 0; i < array.length; i++) {
        if (i > 0) {
          s.append(',');
        }
        s.append(array[i]);
      }
    }
    else if (o instanceof short[]) {
      short[] array = (short[]) o;
      for (int i = 0; i < array.length; i++) {
        if (i > 0) {
          s.append(',');
        }
        s.append(array[i]);
      }
    }
    else if (o instanceof double[]) {
      double[] array = (double[]) o;
      for (int i = 0; i < array.length; i++) {
        if (i > 0) {
          s.append(',');
        }
        s.append(array[i]);
      }
    }
    else if (o instanceof float[]) {
      float[] array = (float[]) o;
      for (int i = 0; i < array.length; i++) {
        if (i > 0) {
          s.append(',');
        }
        s.append(array[i]);
      }
    }
    else if (o instanceof byte[]) {
      byte[] array = (byte[]) o;
      for (int i = 0; i < array.length; i++) {
        if (i > 0) {
          s.append(',');
        }
        s.append(array[i]);
      }
    }
    else {
      throw new IllegalStateException("Not supported " + o.getClass());
    }
    s.append(']');
    assertThat(s.toString()).isEqualTo(expectedToString);
    return s.toString();
  }

}
