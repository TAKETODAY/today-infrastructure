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

package infra.expression.spel;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import infra.core.TypeDescriptor;
import infra.expression.EvaluationContext;
import infra.expression.EvaluationException;
import infra.expression.Operation;
import infra.expression.OperatorOverloader;
import infra.expression.PropertyAccessor;
import infra.expression.TypeComparator;
import infra.expression.TypeConverter;
import infra.expression.TypedValue;
import infra.lang.Assert;
import infra.util.CollectionUtils;

/**
 * ExpressionState is for maintaining per-expression-evaluation state: any changes to
 * it are not seen by other expressions, but it gives a place to hold local variables and
 * for component expressions in a compound expression to communicate state. This is in
 * contrast to the EvaluationContext, which is shared amongst expression evaluations, and
 * any changes to it will be seen by other expressions or any code that chooses to ask
 * questions of the context.
 *
 * <p>It also acts as a place to define common utility routines that the various AST
 * nodes might need.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ExpressionState {

  private final EvaluationContext relatedContext;

  private final TypedValue rootObject;

  private final SpelParserConfiguration configuration;

  @Nullable
  private Deque<TypedValue> contextObjects;

  // When entering a new scope there is a new base object which should be used
  // for '#this' references (or to act as a target for unqualified references).
  // This ArrayDeque captures those objects at each nested scope level.
  // For example:
  // #list1.?[#list2.contains(#this)]
  // On entering the selection we enter a new scope, and #this is now the
  // element from list1.
  @Nullable
  private ArrayDeque<TypedValue> scopeRootObjects;

  public ExpressionState(EvaluationContext context) {
    this(context, context.getRootObject(), new SpelParserConfiguration(false, false));
  }

  public ExpressionState(EvaluationContext context, SpelParserConfiguration configuration) {
    this(context, context.getRootObject(), configuration);
  }

  public ExpressionState(EvaluationContext context, TypedValue rootObject) {
    this(context, rootObject, new SpelParserConfiguration(false, false));
  }

  public ExpressionState(EvaluationContext context, TypedValue rootObject, SpelParserConfiguration configuration) {
    Assert.notNull(context, "EvaluationContext is required");
    Assert.notNull(rootObject, "'rootObject' is required");
    Assert.notNull(configuration, "SpelParserConfiguration is required");
    this.relatedContext = context;
    this.rootObject = rootObject;
    this.configuration = configuration;
  }

  /**
   * The active context object is what unqualified references to properties/etc are resolved against.
   */
  public TypedValue getActiveContextObject() {
    if (CollectionUtils.isEmpty(this.contextObjects)) {
      return this.rootObject;
    }
    return this.contextObjects.element();
  }

  public void pushActiveContextObject(TypedValue obj) {
    initContextObjects().push(obj);
  }

  public void popActiveContextObject() {
    try {
      initContextObjects().pop();
    }
    catch (NoSuchElementException ex) {
      throw new IllegalStateException("Cannot pop active context object: stack is empty");
    }
  }

  public TypedValue getRootContextObject() {
    return this.rootObject;
  }

  public TypedValue getScopeRootContextObject() {
    if (CollectionUtils.isEmpty(this.scopeRootObjects)) {
      return this.rootObject;
    }
    return this.scopeRootObjects.element();
  }

  /**
   * Assign the value created by the specified {@link Supplier} to a named variable
   * within the evaluation context.
   * <p>In contrast to {@link #setVariable(String, Object)}, this method should
   * only be invoked to support assignment within an expression.
   *
   * @param name the name of the variable to assign
   * @param valueSupplier the supplier of the value to be assigned to the variable
   * @return a {@link TypedValue} wrapping the assigned value
   * @see EvaluationContext#assignVariable(String, Supplier)
   */
  public TypedValue assignVariable(String name, Supplier<TypedValue> valueSupplier) {
    return this.relatedContext.assignVariable(name, valueSupplier);
  }

  /**
   * Set a named variable in the evaluation context to a specified value.
   * <p>In contrast to {@link #assignVariable(String, Supplier)}, this method
   * should only be invoked programmatically.
   *
   * @param name the name of the variable to set
   * @param value the value to be placed in the variable
   * @see EvaluationContext#setVariable(String, Object)
   */
  public void setVariable(String name, @Nullable Object value) {
    this.relatedContext.setVariable(name, value);
  }

  /**
   * Look up a named global variable in the evaluation context.
   *
   * @param name the name of the variable to look up
   * @return a {@link TypedValue} containing the value of the variable, or
   * {@link TypedValue#NULL} if the variable does not exist
   * @see #assignVariable(String, Supplier)
   * @see #setVariable(String, Object)
   */
  public TypedValue lookupVariable(String name) {
    Object value = this.relatedContext.lookupVariable(name);
    return (value != null ? new TypedValue(value) : TypedValue.NULL);
  }

  public TypeComparator getTypeComparator() {
    return this.relatedContext.getTypeComparator();
  }

  public Class<?> findType(String type) throws EvaluationException {
    return this.relatedContext.getTypeLocator().findType(type);
  }

  public TypeConverter getTypeConverter() {
    return this.relatedContext.getTypeConverter();
  }

  public Object convertValue(Object value, TypeDescriptor targetTypeDescriptor) throws EvaluationException {
    Object result = this.relatedContext.getTypeConverter().convertValue(
            value, TypeDescriptor.forObject(value), targetTypeDescriptor);
    if (result == null) {
      throw new IllegalStateException("Null conversion result for value [" + value + "]");
    }
    return result;
  }

  public @Nullable Object convertValue(TypedValue value, TypeDescriptor targetTypeDescriptor) throws EvaluationException {
    Object val = value.getValue();
    return this.relatedContext.getTypeConverter().convertValue(
            val, TypeDescriptor.forObject(val), targetTypeDescriptor);
  }

  /**
   * Enter a new scope with a new {@linkplain #getActiveContextObject() root
   * context object} and a new local variable scope.
   */
  public void enterScope() {
    initScopeRootObjects().push(getActiveContextObject());
  }

  public void exitScope() {
    initScopeRootObjects().pop();
  }

  private Deque<TypedValue> initContextObjects() {
    if (this.contextObjects == null) {
      this.contextObjects = new ArrayDeque<>();
    }
    return this.contextObjects;
  }

  private Deque<TypedValue> initScopeRootObjects() {
    if (this.scopeRootObjects == null) {
      this.scopeRootObjects = new ArrayDeque<>();
    }
    return this.scopeRootObjects;
  }

  public TypedValue operate(Operation op, @Nullable Object left, @Nullable Object right) throws EvaluationException {
    OperatorOverloader overloader = this.relatedContext.getOperatorOverloader();
    if (overloader.overridesOperation(op, left, right)) {
      Object returnValue = overloader.operate(op, left, right);
      return new TypedValue(returnValue);
    }
    else {
      String leftType = (left == null ? "null" : left.getClass().getName());
      String rightType = (right == null ? "null" : right.getClass().getName());
      throw new SpelEvaluationException(SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES, op, leftType, rightType);
    }
  }

  public List<PropertyAccessor> getPropertyAccessors() {
    return this.relatedContext.getPropertyAccessors();
  }

  public EvaluationContext getEvaluationContext() {
    return this.relatedContext;
  }

  public SpelParserConfiguration getConfiguration() {
    return this.configuration;
  }

}
