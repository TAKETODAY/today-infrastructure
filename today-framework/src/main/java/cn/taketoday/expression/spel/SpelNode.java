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

import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.lang.Nullable;

/**
 * Represents a node in the AST for a parsed expression.
 *
 * @author Andy Clement
 * @since 4.0
 */
public interface SpelNode {

  /**
   * Evaluate the expression node in the context of the supplied expression state
   * and return the value.
   *
   * @param expressionState the current expression state (includes the context)
   * @return the value of this node evaluated against the specified state
   */
  @Nullable
  Object getValue(ExpressionState expressionState) throws EvaluationException;

  /**
   * Evaluate the expression node in the context of the supplied expression state
   * and return the typed value.
   *
   * @param expressionState the current expression state (includes the context)
   * @return the type value of this node evaluated against the specified state
   */
  TypedValue getTypedValue(ExpressionState expressionState) throws EvaluationException;

  /**
   * Determine if this expression node will support a setValue() call.
   *
   * @param expressionState the current expression state (includes the context)
   * @return true if the expression node will allow setValue()
   * @throws EvaluationException if something went wrong trying to determine
   * if the node supports writing
   */
  boolean isWritable(ExpressionState expressionState) throws EvaluationException;

  /**
   * Evaluate the expression to a node and then set the new value on that node.
   * For example, if the expression evaluates to a property reference, then the
   * property will be set to the new value.
   *
   * @param expressionState the current expression state (includes the context)
   * @param newValue the new value
   * @throws EvaluationException if any problem occurs evaluating the expression or
   * setting the new value
   */
  void setValue(ExpressionState expressionState, @Nullable Object newValue) throws EvaluationException;

  /**
   * Return the string form the this AST node.
   *
   * @return the string form
   */
  String toStringAST();

  /**
   * Return the number of children under this node.
   *
   * @return the child count
   */
  int getChildCount();

  /**
   * Helper method that returns a SpelNode rather than an Antlr Tree node.
   *
   * @return the child node cast to a SpelNode
   */
  SpelNode getChild(int index);

  /**
   * Determine the class of the object passed in, unless it is already a class object.
   *
   * @param obj the object that the caller wants the class of
   * @return the class of the object if it is not already a class object,
   * or {@code null} if the object is {@code null}
   */
  @Nullable
  Class<?> getObjectClass(@Nullable Object obj);

  /**
   * Return the start position of this AST node in the expression string.
   *
   * @return the start position
   */
  int getStartPosition();

  /**
   * Return the end position of this AST node in the expression string.
   *
   * @return the end position
   */
  int getEndPosition();

}
