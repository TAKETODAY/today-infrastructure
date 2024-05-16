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

package cn.taketoday.orm;

import cn.taketoday.dao.DataRetrievalFailureException;
import cn.taketoday.lang.Nullable;

/**
 * Exception thrown if a mapped object could not be retrieved via its identifier.
 * Provides information about the persistent class and the identifier.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ObjectRetrievalFailureException extends DataRetrievalFailureException {

  @Nullable
  private final Object persistentClass;

  @Nullable
  private final Object identifier;

  /**
   * Create a general ObjectRetrievalFailureException with the given message,
   * without any information on the affected object.
   *
   * @param msg the detail message
   * @param cause the source exception
   */
  public ObjectRetrievalFailureException(String msg, Throwable cause) {
    super(msg, cause);
    this.persistentClass = null;
    this.identifier = null;
  }

  /**
   * Create a new ObjectRetrievalFailureException for the given object,
   * with the default "not found" message.
   *
   * @param persistentClass the persistent class
   * @param identifier the ID of the object that should have been retrieved
   */
  public ObjectRetrievalFailureException(Class<?> persistentClass, Object identifier) {
    this(persistentClass, identifier,
            "Object of class [" + persistentClass.getName() + "] with identifier [" + identifier + "]: not found",
            null);
  }

  /**
   * Create a new ObjectRetrievalFailureException for the given object,
   * with the given explicit message and exception.
   *
   * @param persistentClass the persistent class
   * @param identifier the ID of the object that should have been retrieved
   * @param msg the detail message
   * @param cause the source exception
   */
  public ObjectRetrievalFailureException(
          Class<?> persistentClass, @Nullable Object identifier, String msg, @Nullable Throwable cause) {

    super(msg, cause);
    this.persistentClass = persistentClass;
    this.identifier = identifier;
  }

  /**
   * Create a new ObjectRetrievalFailureException for the given object,
   * with the default "not found" message.
   *
   * @param persistentClassName the name of the persistent class
   * @param identifier the ID of the object that should have been retrieved
   */
  public ObjectRetrievalFailureException(String persistentClassName, Object identifier) {
    this(persistentClassName, identifier,
            "Object of class [" + persistentClassName + "] with identifier [" + identifier + "]: not found",
            null);
  }

  /**
   * Create a new ObjectRetrievalFailureException for the given object,
   * with the given explicit message and exception.
   *
   * @param persistentClassName the name of the persistent class
   * @param identifier the ID of the object that should have been retrieved
   * @param msg the detail message
   * @param cause the source exception
   */
  public ObjectRetrievalFailureException(
          String persistentClassName, @Nullable Object identifier, String msg, @Nullable Throwable cause) {

    super(msg, cause);
    this.persistentClass = persistentClassName;
    this.identifier = identifier;
  }

  /**
   * Return the persistent class of the object that was not found.
   * If no Class was specified, this method returns null.
   */
  @Nullable
  public Class<?> getPersistentClass() {
    return (this.persistentClass instanceof Class ? (Class<?>) this.persistentClass : null);
  }

  /**
   * Return the name of the persistent class of the object that was not found.
   * Will work for both Class objects and String names.
   */
  @Nullable
  public String getPersistentClassName() {
    if (this.persistentClass instanceof Class) {
      return ((Class<?>) this.persistentClass).getName();
    }
    return (this.persistentClass != null ? this.persistentClass.toString() : null);
  }

  /**
   * Return the identifier of the object that was not found.
   */
  @Nullable
  public Object getIdentifier() {
    return this.identifier;
  }

}
