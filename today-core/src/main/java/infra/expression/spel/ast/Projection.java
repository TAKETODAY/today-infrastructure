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

package infra.expression.spel.ast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import infra.expression.EvaluationException;
import infra.expression.TypedValue;
import infra.expression.spel.ExpressionState;
import infra.expression.spel.SpelEvaluationException;
import infra.expression.spel.SpelMessage;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;

/**
 * Represents projection, where a given operation is performed on all elements in some
 * input sequence, returning a new sequence of the same size.
 *
 * <p>For example: <code>{1,2,3,4,5,6,7,8,9,10}.![#isEven(#this)]</code> evaluates
 * to {@code [n, y, n, y, n, y, n, y, n, y]}.
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
  public boolean isNullSafe() {
    return nullSafe;
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    return getValueRef(state).getValue();
  }

  @Override
  protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
    TypedValue op = state.getActiveContextObject();

    Object operand = op.getValue();
    boolean operandIsArray = ObjectUtils.isArray(operand);
    // TypeDescriptor operandTypeDescriptor = op.getTypeDescriptor();

    // When the input is a map, we push a special context object on the stack
    // before calling the specified operation. This special context object
    // has two fields 'key' and 'value' that refer to the map entries key
    // and value, and they can be referenced in the operation
    // eg. {'a':'y','b':'n'}.![value=='y'?key:null]" == ['a', null]
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
      return new ValueRef.TypedValueHolderValueRef(new TypedValue(result), this);  // TODO unable to build correct type descriptor
    }

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

    if (operand == null) {
      if (this.nullSafe) {
        return ValueRef.NullValueRef.INSTANCE;
      }
      throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROJECTION_NOT_SUPPORTED_ON_TYPE, "null");
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
