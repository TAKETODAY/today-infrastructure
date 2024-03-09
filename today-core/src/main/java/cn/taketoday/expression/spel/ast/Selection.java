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

package cn.taketoday.expression.spel.ast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Represents selection over a map or collection.
 * Represents selection over a map or collection.
 *
 * <p>For example, <code>{1,2,3,4,5,6,7,8,9,10}.?[#isEven(#this)]</code> evaluates
 * to {@code [2, 4, 6, 8, 10]}.
 *
 * <p>Basically a subset of the input data is returned based on the evaluation of
 * the expression supplied as selection criteria.
 *
 * @author Andy Clement
 * @author Mark Fisher
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Selection extends SpelNodeImpl {

  /**
   * All items ({@code ?[]}).
   */
  public static final int ALL = 0;

  /**
   * The first item ({@code ^[]}).
   */
  public static final int FIRST = 1;

  /**
   * The last item ({@code $[]}).
   */
  public static final int LAST = 2;

  private final int variant;

  private final boolean nullSafe;

  public Selection(boolean nullSafe, int variant, int startPos, int endPos, SpelNodeImpl expression) {
    super(startPos, endPos, expression);
    this.nullSafe = nullSafe;
    this.variant = variant;
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    return getValueRef(state).getValue();
  }

  @Override
  protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
    TypedValue op = state.getActiveContextObject();
    Object operand = op.getValue();
    SpelNodeImpl selectionCriteria = this.children[0];

    if (operand instanceof Map<?, ?> mapdata) {
      Map<Object, Object> result = new HashMap<>();
      Object lastKey = null;

      for (Map.Entry<?, ?> entry : mapdata.entrySet()) {
        try {
          TypedValue kvPair = new TypedValue(entry);
          state.pushActiveContextObject(kvPair);
          state.enterScope();
          Object val = selectionCriteria.getValueInternal(state).getValue();
          if (val instanceof Boolean b) {
            if (b) {
              result.put(entry.getKey(), entry.getValue());
              if (this.variant == FIRST) {
                return new ValueRef.TypedValueHolderValueRef(new TypedValue(result), this);
              }
              lastKey = entry.getKey();
            }
          }
          else {
            throw new SpelEvaluationException(selectionCriteria.getStartPosition(),
                    SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
          }
        }
        finally {
          state.popActiveContextObject();
          state.exitScope();
        }
      }

      if ((this.variant == FIRST || this.variant == LAST) && result.isEmpty()) {
        return new ValueRef.TypedValueHolderValueRef(TypedValue.NULL, this);
      }

      if (this.variant == LAST) {
        Map<Object, Object> resultMap = new HashMap<>();
        Object lastValue = result.get(lastKey);
        resultMap.put(lastKey, lastValue);
        return new ValueRef.TypedValueHolderValueRef(new TypedValue(resultMap), this);
      }

      return new ValueRef.TypedValueHolderValueRef(new TypedValue(result), this);
    }

    if (operand instanceof Iterable || ObjectUtils.isArray(operand)) {
      Iterable<?> data = (operand instanceof Iterable ?
              (Iterable<?>) operand : Arrays.asList(ObjectUtils.toObjectArray(operand)));

      ArrayList<Object> result = new ArrayList<>();
      for (Object element : data) {
        try {
          state.pushActiveContextObject(new TypedValue(element));
          state.enterScope();
          Object val = selectionCriteria.getValueInternal(state).getValue();
          if (val instanceof Boolean b) {
            if (b) {
              if (this.variant == FIRST) {
                return new ValueRef.TypedValueHolderValueRef(new TypedValue(element), this);
              }
              result.add(element);
            }
          }
          else {
            throw new SpelEvaluationException(selectionCriteria.getStartPosition(),
                    SpelMessage.RESULT_OF_SELECTION_CRITERIA_IS_NOT_BOOLEAN);
          }
        }
        finally {
          state.exitScope();
          state.popActiveContextObject();
        }
      }

      if ((this.variant == FIRST || this.variant == LAST) && result.isEmpty()) {
        return ValueRef.NullValueRef.INSTANCE;
      }

      if (this.variant == LAST) {
        return new ValueRef.TypedValueHolderValueRef(new TypedValue(CollectionUtils.lastElement(result)), this);
      }

      if (operand instanceof Iterable) {
        return new ValueRef.TypedValueHolderValueRef(new TypedValue(result), this);
      }

      Class<?> elementType = null;
      TypeDescriptor typeDesc = op.getTypeDescriptor();
      if (typeDesc != null) {
        TypeDescriptor elementTypeDesc = typeDesc.getElementDescriptor();
        if (elementTypeDesc != null) {
          elementType = ClassUtils.resolvePrimitiveIfNecessary(elementTypeDesc.getType());
        }
      }
      Assert.state(elementType != null, "Unresolvable element type");

      Object resultArray = Array.newInstance(elementType, result.size());
      System.arraycopy(result.toArray(), 0, resultArray, 0, result.size());
      return new ValueRef.TypedValueHolderValueRef(new TypedValue(resultArray), this);
    }

    if (operand == null) {
      if (this.nullSafe) {
        return ValueRef.NullValueRef.INSTANCE;
      }
      throw new SpelEvaluationException(getStartPosition(), SpelMessage.INVALID_TYPE_FOR_SELECTION, "null");
    }

    throw new SpelEvaluationException(getStartPosition(), SpelMessage.INVALID_TYPE_FOR_SELECTION,
            operand.getClass().getName());
  }

  @Override
  public String toStringAST() {
    return prefix() + getChild(0).toStringAST() + "]";
  }

  private String prefix() {
    return switch (this.variant) {
      case ALL -> "?[";
      case FIRST -> "^[";
      case LAST -> "$[";
      default -> "";
    };
  }

}
