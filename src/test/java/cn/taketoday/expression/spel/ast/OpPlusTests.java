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

package cn.taketoday.expression.spel.ast;

import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cn.taketoday.core.conversion.support.GenericConversionService;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.expression.spel.support.StandardTypeConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for SpEL's plus operator.
 *
 * @author Ivo Smid
 * @author Chris Beams
 * @see OpPlus
 * @since 3.2
 */
public class OpPlusTests {

  @Test
  public void test_emptyOperands() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new OpPlus(-1, -1));
  }

  @Test
  public void test_unaryPlusWithStringLiteral() {
    ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

    StringLiteral str = new StringLiteral("word", -1, -1, "word");

    OpPlus o = new OpPlus(-1, -1, str);
    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
            o.getValueInternal(expressionState));
  }

  @Test
  public void test_unaryPlusWithNumberOperand() {
    ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

    {
      RealLiteral realLiteral = new RealLiteral("123.00", -1, -1, 123.0);
      OpPlus o = new OpPlus(-1, -1, realLiteral);
      TypedValue value = o.getValueInternal(expressionState);

      assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(Double.class);
      assertThat(value.getTypeDescriptor().getType()).isEqualTo(Double.class);
      assertThat(value.getValue()).isEqualTo(realLiteral.getLiteralValue().getValue());
    }

    {
      IntLiteral intLiteral = new IntLiteral("123", -1, -1, 123);
      OpPlus o = new OpPlus(-1, -1, intLiteral);
      TypedValue value = o.getValueInternal(expressionState);

      assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(Integer.class);
      assertThat(value.getTypeDescriptor().getType()).isEqualTo(Integer.class);
      assertThat(value.getValue()).isEqualTo(intLiteral.getLiteralValue().getValue());
    }

    {
      LongLiteral longLiteral = new LongLiteral("123", -1, -1, 123L);
      OpPlus o = new OpPlus(-1, -1, longLiteral);
      TypedValue value = o.getValueInternal(expressionState);

      assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(Long.class);
      assertThat(value.getTypeDescriptor().getType()).isEqualTo(Long.class);
      assertThat(value.getValue()).isEqualTo(longLiteral.getLiteralValue().getValue());
    }
  }

  @Test
  public void test_binaryPlusWithNumberOperands() {
    ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

    {
      RealLiteral n1 = new RealLiteral("123.00", -1, -1, 123.0);
      RealLiteral n2 = new RealLiteral("456.00", -1, -1, 456.0);
      OpPlus o = new OpPlus(-1, -1, n1, n2);
      TypedValue value = o.getValueInternal(expressionState);

      assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(Double.class);
      assertThat(value.getTypeDescriptor().getType()).isEqualTo(Double.class);
      assertThat(value.getValue()).isEqualTo(Double.valueOf(123.0 + 456.0));
    }

    {
      LongLiteral n1 = new LongLiteral("123", -1, -1, 123L);
      LongLiteral n2 = new LongLiteral("456", -1, -1, 456L);
      OpPlus o = new OpPlus(-1, -1, n1, n2);
      TypedValue value = o.getValueInternal(expressionState);

      assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(Long.class);
      assertThat(value.getTypeDescriptor().getType()).isEqualTo(Long.class);
      assertThat(value.getValue()).isEqualTo(Long.valueOf(123L + 456L));
    }

    {
      IntLiteral n1 = new IntLiteral("123", -1, -1, 123);
      IntLiteral n2 = new IntLiteral("456", -1, -1, 456);
      OpPlus o = new OpPlus(-1, -1, n1, n2);
      TypedValue value = o.getValueInternal(expressionState);

      assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(Integer.class);
      assertThat(value.getTypeDescriptor().getType()).isEqualTo(Integer.class);
      assertThat(value.getValue()).isEqualTo(Integer.valueOf(123 + 456));
    }
  }

  @Test
  public void test_binaryPlusWithStringOperands() {
    ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

    StringLiteral n1 = new StringLiteral("\"foo\"", -1, -1, "\"foo\"");
    StringLiteral n2 = new StringLiteral("\"bar\"", -1, -1, "\"bar\"");
    OpPlus o = new OpPlus(-1, -1, n1, n2);
    TypedValue value = o.getValueInternal(expressionState);

    assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(String.class);
    assertThat(value.getTypeDescriptor().getType()).isEqualTo(String.class);
    assertThat(value.getValue()).isEqualTo("foobar");
  }

  @Test
  public void test_binaryPlusWithLeftStringOperand() {
    ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

    StringLiteral n1 = new StringLiteral("\"number is \"", -1, -1, "\"number is \"");
    LongLiteral n2 = new LongLiteral("123", -1, -1, 123);
    OpPlus o = new OpPlus(-1, -1, n1, n2);
    TypedValue value = o.getValueInternal(expressionState);

    assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(String.class);
    assertThat(value.getTypeDescriptor().getType()).isEqualTo(String.class);
    assertThat(value.getValue()).isEqualTo("number is 123");
  }

  @Test
  public void test_binaryPlusWithRightStringOperand() {
    ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());

    LongLiteral n1 = new LongLiteral("123", -1, -1, 123);
    StringLiteral n2 = new StringLiteral("\" is a number\"", -1, -1, "\" is a number\"");
    OpPlus o = new OpPlus(-1, -1, n1, n2);
    TypedValue value = o.getValueInternal(expressionState);

    assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(String.class);
    assertThat(value.getTypeDescriptor().getType()).isEqualTo(String.class);
    assertThat(value.getValue()).isEqualTo("123 is a number");
  }

  @Test
  public void test_binaryPlusWithTime_ToString() {
    ExpressionState expressionState = new ExpressionState(new StandardEvaluationContext());
    Time time = new Time(new Date().getTime());

    VariableReference var = new VariableReference("timeVar", -1, -1);
    var.setValue(expressionState, time);

    StringLiteral n2 = new StringLiteral("\" is now\"", -1, -1, "\" is now\"");
    OpPlus o = new OpPlus(-1, -1, var, n2);
    TypedValue value = o.getValueInternal(expressionState);

    assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(String.class);
    assertThat(value.getTypeDescriptor().getType()).isEqualTo(String.class);
    assertThat(value.getValue()).isEqualTo((time + " is now"));
  }

  @Test
  public void test_binaryPlusWithTimeConverted() {
    SimpleDateFormat format = new SimpleDateFormat("hh :--: mm :--: ss", Locale.ENGLISH);

    GenericConversionService conversionService = new GenericConversionService();
    conversionService.addConverter(Time.class, String.class, format::format);

    StandardEvaluationContext evaluationContextConverter = new StandardEvaluationContext();
    evaluationContextConverter.setTypeConverter(new StandardTypeConverter(conversionService));

    ExpressionState expressionState = new ExpressionState(evaluationContextConverter);
    Time time = new Time(new Date().getTime());

    VariableReference var = new VariableReference("timeVar", -1, -1);
    var.setValue(expressionState, time);

    StringLiteral n2 = new StringLiteral("\" is now\"", -1, -1, "\" is now\"");
    OpPlus o = new OpPlus(-1, -1, var, n2);
    TypedValue value = o.getValueInternal(expressionState);

    assertThat(value.getTypeDescriptor().getObjectType()).isEqualTo(String.class);
    assertThat(value.getTypeDescriptor().getType()).isEqualTo(String.class);
    assertThat(value.getValue()).isEqualTo((format.format(time) + " is now"));
  }

}
