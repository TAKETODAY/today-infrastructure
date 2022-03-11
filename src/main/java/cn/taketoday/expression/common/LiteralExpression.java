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

package cn.taketoday.expression.common;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.lang.Nullable;

/**
 * A very simple hardcoded implementation of the Expression interface that represents a
 * string literal. It is used with CompositeStringExpression when representing a template
 * expression which is made up of pieces - some being real expressions to be handled by
 * an EL implementation like SpEL, and some being just textual elements.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.0
 */
public class LiteralExpression implements Expression {

  /** Fixed literal value of this expression. */
  private final String literalValue;

  public LiteralExpression(String literalValue) {
    this.literalValue = literalValue;
  }

  @Override
  public final String getExpressionString() {
    return this.literalValue;
  }

  @Override
  public Class<?> getValueType(EvaluationContext context) {
    return String.class;
  }

  @Override
  public String getValue() {
    return this.literalValue;
  }

  @Override
  @Nullable
  public <T> T getValue(@Nullable Class<T> expectedResultType) throws EvaluationException {
    Object value = getValue();
    return ExpressionUtils.convertTypedValue(null, new TypedValue(value), expectedResultType);
  }

  @Override
  public String getValue(@Nullable Object rootObject) {
    return this.literalValue;
  }

  @Override
  @Nullable
  public <T> T getValue(@Nullable Object rootObject, @Nullable Class<T> desiredResultType) throws EvaluationException {
    Object value = getValue(rootObject);
    return ExpressionUtils.convertTypedValue(null, new TypedValue(value), desiredResultType);
  }

  @Override
  public String getValue(EvaluationContext context) {
    return this.literalValue;
  }

  @Override
  @Nullable
  public <T> T getValue(EvaluationContext context, @Nullable Class<T> expectedResultType)
          throws EvaluationException {

    Object value = getValue(context);
    return ExpressionUtils.convertTypedValue(context, new TypedValue(value), expectedResultType);
  }

  @Override
  public String getValue(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
    return this.literalValue;
  }

  @Override
  @Nullable
  public <T> T getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Class<T> desiredResultType)
          throws EvaluationException {

    Object value = getValue(context, rootObject);
    return ExpressionUtils.convertTypedValue(context, new TypedValue(value), desiredResultType);
  }

  @Override
  public Class<?> getValueType() {
    return String.class;
  }

  @Override
  public Class<?> getValueType(@Nullable Object rootObject) throws EvaluationException {
    return String.class;
  }

  @Override
  public Class<?> getValueType(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
    return String.class;
  }

  @Override
  public TypeDescriptor getValueTypeDescriptor() {
    return TypeDescriptor.valueOf(String.class);
  }

  @Override
  public TypeDescriptor getValueTypeDescriptor(@Nullable Object rootObject) throws EvaluationException {
    return TypeDescriptor.valueOf(String.class);
  }

  @Override
  public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) {
    return TypeDescriptor.valueOf(String.class);
  }

  @Override
  public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
    return TypeDescriptor.valueOf(String.class);
  }

  @Override
  public boolean isWritable(@Nullable Object rootObject) throws EvaluationException {
    return false;
  }

  @Override
  public boolean isWritable(EvaluationContext context) {
    return false;
  }

  @Override
  public boolean isWritable(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
    return false;
  }

  @Override
  public void setValue(@Nullable Object rootObject, @Nullable Object value) throws EvaluationException {
    throw new EvaluationException(this.literalValue, "Cannot call setValue() on a LiteralExpression");
  }

  @Override
  public void setValue(EvaluationContext context, @Nullable Object value) throws EvaluationException {
    throw new EvaluationException(this.literalValue, "Cannot call setValue() on a LiteralExpression");
  }

  @Override
  public void setValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Object value) throws EvaluationException {
    throw new EvaluationException(this.literalValue, "Cannot call setValue() on a LiteralExpression");
  }

}
