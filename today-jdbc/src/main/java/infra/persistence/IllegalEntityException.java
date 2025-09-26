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

package infra.persistence;

import org.jspecify.annotations.Nullable;

import infra.jdbc.PersistenceException;

/**
 * Thrown to indicate that an entity is in an illegal or invalid state
 * during persistence operations. This exception typically occurs when
 * an entity violates constraints or rules defined by the persistence layer.
 *
 * <p>This exception extends {@link PersistenceException}, allowing it
 * to be integrated into the broader exception handling framework for
 * persistence-related errors.
 *
 * <h3>Usage Example</h3>
 * Below is an example of how this exception might be used in a scenario
 * where an entity with an invalid primary key is detected:
 *
 * <pre>{@code
 *   public void validateEntity(Entity entity) {
 *     if (entity.getId() == null) {
 *       throw new IllegalEntityException("Entity ID must not be null");
 *     }
 *   }
 * }</pre>
 *
 * <p>In the above example, if the entity's ID is null, an
 * {@code IllegalEntityException} is thrown to signal that the entity
 * is invalid and cannot be persisted.
 *
 * <h3>Handling the Exception</h3>
 * When catching this exception, you can access the error message and
 * underlying cause (if any) to provide meaningful feedback to the user
 * or log the issue for debugging purposes:
 *
 * <pre>{@code
 *   try {
 *     validateEntity(entity);
 *   }
 *   catch (IllegalEntityException e) {
 *     System.err.println("Invalid entity: " + e.getMessage());
 *     // Optionally log the exception or rethrow it
 *   }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/15 15:50
 */
public class IllegalEntityException extends PersistenceException {

  /**
   * Constructs a new {@code IllegalEntityException} with the specified
   * detail message. The message provides additional context about why
   * the exception was thrown.
   *
   * <p>If the message is {@code null}, no detailed message will be available
   * for this exception instance.
   *
   * @param message the detail message explaining the cause of the exception.
   * This can be {@code null}.
   */
  public IllegalEntityException(@Nullable String message) {
    this(message, null);
  }

  /**
   * Constructs a new {@code IllegalEntityException} with the specified
   * detail message and cause.
   *
   * @param message the detail message explaining the cause of the exception.
   * This can be {@code null}.
   * @param cause the underlying cause of the exception, typically another
   * exception or error. This can be {@code null}.
   */
  public IllegalEntityException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

}
