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

package infra.expression;

import infra.lang.Nullable;

/**
 * A property accessor is able to read from (and possibly write to) an object's
 * properties.
 *
 * <p>This interface places no restrictions on what constitutes a property.
 * Implementors are therefore free to access properties directly via fields,
 * through getters, or in any other way they deem appropriate.
 *
 * <p>A property accessor can specify an array of
 * {@linkplain #getSpecificTargetClasses() target classes} for which it should be
 * called. See {@link TargetedAccessor} for details.
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface PropertyAccessor extends TargetedAccessor {

  /**
   * Called to determine if this property accessor is able to read a specified
   * property on a specified target object.
   *
   * @param context the evaluation context in which the access is being attempted
   * @param target the target object upon which the property is being accessed
   * @param name the name of the property being accessed
   * @return true if this property accessor is able to read the property
   * @throws AccessException if there is any problem determining whether the
   * property can be read
   */
  boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException;

  /**
   * Called to read a property from a specified target object.
   * <p>Should only succeed if {@link #canRead} also returns {@code true}.
   *
   * @param context the evaluation context in which the access is being attempted
   * @param target the target object upon which the property is being accessed
   * @param name the name of the property being accessed
   * @return a TypedValue object wrapping the property value read and a type
   * descriptor for it
   * @throws AccessException if there is any problem reading the property value
   */
  TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException;

  /**
   * Called to determine if this property accessor is able to write to a specified
   * property on a specified target object.
   *
   * @param context the evaluation context in which the access is being attempted
   * @param target the target object upon which the property is being accessed
   * @param name the name of the property being accessed
   * @return true if this property accessor is able to write to the property
   * @throws AccessException if there is any problem determining whether the
   * property can be written to
   */
  boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException;

  /**
   * Called to write to a property on a specified target object.
   * <p>Should only succeed if {@link #canWrite} also returns {@code true}.
   *
   * @param context the evaluation context in which the access is being attempted
   * @param target the target object upon which the property is being accessed
   * @param name the name of the property being accessed
   * @param newValue the new value for the property
   * @throws AccessException if there is any problem writing to the property value
   */
  void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
          throws AccessException;

}
