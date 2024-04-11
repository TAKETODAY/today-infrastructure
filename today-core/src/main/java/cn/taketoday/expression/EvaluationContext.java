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

package cn.taketoday.expression;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.lang.Nullable;

/**
 * Expressions are executed in an evaluation context. It is in this context that
 * references are resolved when encountered during expression evaluation.
 *
 * <p>There are two default implementations of this interface.
 * <ul>
 * <li>{@link cn.taketoday.expression.spel.support.SimpleEvaluationContext
 * SimpleEvaluationContext}: a simpler builder-style {@code EvaluationContext}
 * variant for data-binding purposes, which allows for opting into several SpEL
 * features as needed.</li>
 * <li>{@link cn.taketoday.expression.spel.support.StandardEvaluationContext
 * StandardEvaluationContext}: a powerful and highly configurable {@code EvaluationContext}
 * implementation, which can be extended, rather than having to implement everything
 * manually.</li>
 * </ul>
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface EvaluationContext {

  /**
   * Return the default root context object against which unqualified
   * properties/methods/etc should be resolved. This can be overridden
   * when evaluating an expression.
   */
  TypedValue getRootObject();

  /**
   * Return a list of accessors that will be asked in turn to read/write a property.
   */
  List<PropertyAccessor> getPropertyAccessors();

  /**
   * Return a list of resolvers that will be asked in turn to locate a constructor.
   */
  List<ConstructorResolver> getConstructorResolvers();

  /**
   * Return a list of resolvers that will be asked in turn to locate a method.
   */
  List<MethodResolver> getMethodResolvers();

  /**
   * Return a bean resolver that can look up beans by name.
   */
  @Nullable
  BeanResolver getBeanResolver();

  /**
   * Return a type locator that can be used to find types, either by short or
   * fully qualified name.
   */
  TypeLocator getTypeLocator();

  /**
   * Return a type converter that can convert (or coerce) a value from one type to another.
   */
  TypeConverter getTypeConverter();

  /**
   * Return a type comparator for comparing pairs of objects for equality.
   */
  TypeComparator getTypeComparator();

  /**
   * Return an operator overloader that may support mathematical operations
   * between more than the standard set of types.
   */
  OperatorOverloader getOperatorOverloader();

  /**
   * Return a list of index accessors that will be asked in turn to access or
   * set an indexed value.
   * <p>The default implementation returns an empty list.
   */
  default List<IndexAccessor> getIndexAccessors() {
    return Collections.emptyList();
  }

  /**
   * Assign the value created by the specified {@link Supplier} to a named variable
   * within this evaluation context.
   * <p>In contrast to {@link #setVariable(String, Object)}, this method should only
   * be invoked to support the assignment operator ({@code =}) within an expression.
   * <p>By default, this method delegates to {@code setVariable(String, Object)},
   * providing the value created by the {@code valueSupplier}. Concrete implementations
   * may override this <em>default</em> method to provide different semantics.
   *
   * @param name the name of the variable to assign
   * @param valueSupplier the supplier of the value to be assigned to the variable
   * @return a {@link TypedValue} wrapping the assigned value
   */
  default TypedValue assignVariable(String name, Supplier<TypedValue> valueSupplier) {
    TypedValue typedValue = valueSupplier.get();
    setVariable(name, typedValue.getValue());
    return typedValue;
  }

  /**
   * Set a named variable within this evaluation context to a specified value.
   *
   * @param name the name of the variable to set
   * @param value the value to be placed in the variable
   */
  void setVariable(String name, @Nullable Object value);

  /**
   * Look up a named variable within this evaluation context.
   *
   * @param name variable to lookup
   * @return the value of the variable, or {@code null} if not found
   */
  @Nullable
  Object lookupVariable(String name);

}
