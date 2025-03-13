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

package infra.expression.spel.ast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import infra.expression.EvaluationException;
import infra.expression.TypedValue;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;

/**
 * Represents projection, where a given operation is performed on all elements in
 * some input sequence, returning a new sequence of the same size.
 *
 * <p>For example: <code>{1,2,3,4,5,6,7,8,9,10}.![#isEven(#this)]</code> evaluates
 * to {@code [n, y, n, y, n, y, n, y, n, y]}.
 *
 * <h3>Null-safe Projection</h3>
 *
 * <p>Null-safe projection is supported via the {@code '?.!'} operator. For example,
 * {@code 'names?.![#this.length]'} will evaluate to {@code null} if {@code names}
 * is {@code null} and will otherwise evaluate to a sequence containing the lengths
 * of the names. null-safe projection also applies when
 * performing projection on an {@link Optional} target. For example, if {@code names}
 * is of type {@code Optional<List<String>>}, the expression
 * {@code 'names?.![#this.length]'} will evaluate to {@code null} if {@code names}
 * is {@code null} or {@link Optional#isEmpty() empty} and will otherwise evaluate
 * to a sequence containing the lengths of the names, effectively
 * {@code names.get().stream().map(String::length).toList()}.
 *
 * @author Andy Clement
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Projection extends SpelNodeImpl {

  private final boolean nullSafe;

  public Projection(boolean nullSafe, int startPos, int endPos, SpelNodeImpl expression) {
    super(startPos, endPos, expression);
    this.nullSafe = nullSafe;
  }

  /**
   * Does this node represent a null-safe projection operation?
   */
  @Override
  public final boolean isNullSafe() {
    return this.nullSafe;
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    return getValueRef(state).getValue();
  }

  @Override
  protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
    TypedValue contextObject = state.getActiveContextObject();
    Object operand = contextObject.getValue();

    if (isNullSafe()) {
      if (operand == null) {
        return ValueRef.NullValueRef.INSTANCE;
      }
      if (operand instanceof Optional<?> optional) {
        if (optional.isEmpty()) {
          return ValueRef.NullValueRef.INSTANCE;
        }
        operand = optional.get();
      }
    }

    if (operand == null) {
      throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROJECTION_NOT_SUPPORTED_ON_TYPE, "null");
    }

    // When the input is a map, we push a Map.Entry on the stack before calling
    // the specified operation. Map.Entry has two properties 'key' and 'value'
    // that can be referenced in the operation -- for example,
    // {'a':'y', 'b':'n'}.![value == 'y' ? key : null] evaluates to ['a', null].
    if (operand instanceof Map<?, ?> mapData) {
      List<Object> result = new ArrayList<>();
      for (Map.Entry<?, ?> entry : mapData.entrySet()) {
        try {
          state.pushActiveContextObject(new TypedValue(entry));
          state.enterScope();
          result.add(this.children[0].getValueInternal(state).getValue());
        }
        finally {
          state.popActiveContextObject();
          state.exitScope();
        }
      }
      return new ValueRef.TypedValueHolderValueRef(new TypedValue(result), this);
    }

    boolean operandIsArray = ObjectUtils.isArray(operand);
    if (operand instanceof Iterable || operandIsArray) {
      Iterable<?> data = operand instanceof Iterable ?
              (Iterable<?>) operand : Arrays.asList(ObjectUtils.toObjectArray(operand));

      ArrayList<Object> result = new ArrayList<>();
      Class<?> arrayElementType = null;
      for (Object element : data) {
        try {
          state.pushActiveContextObject(new TypedValue(element));
          state.enterScope();
          Object value = children[0].getValueInternal(state).getValue();
          if (value != null && operandIsArray) {
            arrayElementType = determineCommonType(arrayElementType, value.getClass());
          }
          result.add(value);
        }
        finally {
          state.exitScope();
          state.popActiveContextObject();
        }
      }

      if (operandIsArray) {
        if (arrayElementType == null) {
          arrayElementType = Object.class;
        }
        Object resultArray = Array.newInstance(arrayElementType, result.size());
        System.arraycopy(result.toArray(), 0, resultArray, 0, result.size());
        return new ValueRef.TypedValueHolderValueRef(new TypedValue(resultArray), this);
      }

      return new ValueRef.TypedValueHolderValueRef(new TypedValue(result), this);
    }

    throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROJECTION_NOT_SUPPORTED_ON_TYPE,
            operand.getClass().getName());
  }

  @Override
  public String toStringAST() {
    return "![" + getChild(0).toStringAST() + "]";
  }

  private Class<?> determineCommonType(@Nullable Class<?> oldType, Class<?> newType) {
    if (oldType == null) {
      return newType;
    }
    if (oldType.isAssignableFrom(newType)) {
      return oldType;
    }
    Class<?> nextType = newType;
    while (nextType != Object.class) {
      if (nextType.isAssignableFrom(oldType)) {
        return nextType;
      }
      nextType = nextType.getSuperclass();
    }
    for (Class<?> nextInterface : ClassUtils.getAllInterfacesForClassAsSet(newType)) {
      if (nextInterface.isAssignableFrom(oldType)) {
        return nextInterface;
      }
    }
    return Object.class;
  }

}
