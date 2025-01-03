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
 * An index accessor is able to read from (and possibly write to) an indexed
 * structure of an object.
 *
 * <p>This interface places no restrictions on what constitutes an indexed
 * structure. Implementors are therefore free to access indexed values any way
 * they deem appropriate.
 *
 * <p>An index accessor can specify an array of
 * {@linkplain #getSpecificTargetClasses() target classes} for which it should be
 * called. See {@link TargetedAccessor} for details.
 *
 * @author Jackmiking Lee
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyAccessor
 * @since 4.0
 */
public interface IndexAccessor extends TargetedAccessor {

  /**
   * Determine if this index accessor is able to read a specified index on a
   * specified target object.
   *
   * @param context the evaluation context in which the access is being attempted
   * @param target the target object upon which the index is being accessed
   * @param index the index being accessed
   * @return {@code true} if this index accessor is able to read the index
   * @throws AccessException if there is any problem determining whether the
   * index can be read
   */
  boolean canRead(EvaluationContext context, Object target, Object index) throws AccessException;

  /**
   * Read an index from a specified target object.
   * <p>Should only be invoked if {@link #canRead} returns {@code true} for the
   * same arguments.
   *
   * @param context the evaluation context in which the access is being attempted
   * @param target the target object upon which the index is being accessed
   * @param index the index being accessed
   * @return a TypedValue object wrapping the index value read and a type
   * descriptor for the value
   * @throws AccessException if there is any problem reading the index
   */
  TypedValue read(EvaluationContext context, Object target, Object index) throws AccessException;

  /**
   * Determine if this index accessor is able to write to a specified index on
   * a specified target object.
   *
   * @param context the evaluation context in which the access is being attempted
   * @param target the target object upon which the index is being accessed
   * @param index the index being accessed
   * @return {@code true} if this index accessor is able to write to the index
   * @throws AccessException if there is any problem determining whether the
   * index can be written to
   */
  boolean canWrite(EvaluationContext context, Object target, Object index) throws AccessException;

  /**
   * Write to an index on a specified target object.
   * <p>Should only be invoked if {@link #canWrite} returns {@code true} for the
   * same arguments.
   *
   * @param context the evaluation context in which the access is being attempted
   * @param target the target object upon which the index is being accessed
   * @param index the index being accessed
   * @param newValue the new value for the index
   * @throws AccessException if there is any problem writing to the index
   */
  void write(EvaluationContext context, Object target, Object index, @Nullable Object newValue)
          throws AccessException;

}
