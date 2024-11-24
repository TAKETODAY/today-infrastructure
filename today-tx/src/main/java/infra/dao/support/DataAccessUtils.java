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

package infra.dao.support;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import infra.dao.DataAccessException;
import infra.dao.EmptyResultDataAccessException;
import infra.dao.IncorrectResultSizeDataAccessException;
import infra.dao.TypeMismatchDataAccessException;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.NumberUtils;

/**
 * Miscellaneous utility methods for DAO implementations.
 * Useful with any data access technology.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class DataAccessUtils {

  /**
   * Return a single result object from the given Collection.
   * <p>Returns {@code null} if 0 result objects found;
   * throws an exception if more than 1 element found.
   *
   * @param results the result Collection (can be {@code null})
   * @return the single result object, or {@code null} if none
   * @throws IncorrectResultSizeDataAccessException if more than one
   * element has been found in the given Collection
   */
  @Nullable
  public static <T> T singleResult(@Nullable Collection<T> results) throws IncorrectResultSizeDataAccessException {
    if (CollectionUtils.isEmpty(results)) {
      return null;
    }
    if (results.size() > 1) {
      throw new IncorrectResultSizeDataAccessException(1, results.size());
    }
    return results.iterator().next();
  }

  /**
   * Return a single result object from the given Stream.
   * <p>Returns {@code null} if 0 result objects found;
   * throws an exception if more than 1 element found.
   *
   * @param results the result Stream (can be {@code null})
   * @return the single result object, or {@code null} if none
   * @throws IncorrectResultSizeDataAccessException if more than one
   * element has been found in the given Stream
   */
  @Nullable
  public static <T> T singleResult(@Nullable Stream<T> results) throws IncorrectResultSizeDataAccessException {
    if (results == null) {
      return null;
    }
    try (results) {
      List<T> resultList = results.limit(2).toList();
      if (resultList.size() > 1) {
        throw new IncorrectResultSizeDataAccessException(1);
      }
      return resultList.isEmpty() ? null : resultList.get(0);
    }
  }

  /**
   * Return a single result object from the given Iterator.
   * <p>Returns {@code null} if 0 result objects found;
   * throws an exception if more than 1 element found.
   *
   * @param results the result Iterator (can be {@code null})
   * @return the single result object, or {@code null} if none
   * @throws IncorrectResultSizeDataAccessException if more than one
   * element has been found in the given Iterator
   */
  @Nullable
  public static <T> T singleResult(@Nullable Iterator<T> results) throws IncorrectResultSizeDataAccessException {
    if (results == null) {
      return null;
    }
    T result = (results.hasNext() ? results.next() : null);
    if (results.hasNext()) {
      throw new IncorrectResultSizeDataAccessException(1);
    }
    return result;
  }

  /**
   * Return a single result object from the given Collection.
   * <p>Returns {@code Optional.empty()} if 0 result objects found;
   * throws an exception if more than 1 element found.
   *
   * @param results the result Collection (can be {@code null})
   * @return the single optional result object, or {@code Optional.empty()} if none
   * @throws IncorrectResultSizeDataAccessException if more than one
   * element has been found in the given Collection
   */
  public static <T> Optional<T> optionalResult(@Nullable Collection<T> results) throws IncorrectResultSizeDataAccessException {
    return Optional.ofNullable(singleResult(results));
  }

  /**
   * Return a single result object from the given Stream.
   * <p>Returns {@code Optional.empty()} if 0 result objects found;
   * throws an exception if more than 1 element found.
   *
   * @param results the result Stream (can be {@code null})
   * @return the single optional result object, or {@code Optional.empty()} if none
   * @throws IncorrectResultSizeDataAccessException if more than one
   * element has been found in the given Stream
   */
  public static <T> Optional<T> optionalResult(@Nullable Stream<T> results) throws IncorrectResultSizeDataAccessException {
    return Optional.ofNullable(singleResult(results));
  }

  /**
   * Return a single result object from the given Iterator.
   * <p>Returns {@code Optional.empty()} if 0 result objects found;
   * throws an exception if more than 1 element found.
   *
   * @param results the result Iterator (can be {@code null})
   * @return the single optional result object, or {@code Optional.empty()} if none
   * @throws IncorrectResultSizeDataAccessException if more than one
   * element has been found in the given Iterator
   */
  public static <T> Optional<T> optionalResult(@Nullable Iterator<T> results) throws IncorrectResultSizeDataAccessException {
    return Optional.ofNullable(singleResult(results));
  }

  /**
   * Return a single result object from the given Collection.
   * <p>Throws an exception if 0 or more than 1 element found.
   *
   * @param results the result Collection (can be {@code null}
   * but is not expected to contain {@code null} elements)
   * @return the single result object
   * @throws IncorrectResultSizeDataAccessException if more than one
   * element has been found in the given Collection
   * @throws EmptyResultDataAccessException if no element at all
   * has been found in the given Collection
   */
  public static <T> T requiredSingleResult(@Nullable Collection<T> results) throws IncorrectResultSizeDataAccessException {
    if (CollectionUtils.isEmpty(results)) {
      throw new EmptyResultDataAccessException(1);
    }
    if (results.size() > 1) {
      throw new IncorrectResultSizeDataAccessException(1, results.size());
    }
    T result = results.iterator().next();
    if (result == null) {
      throw new TypeMismatchDataAccessException("Result value is null but no null value expected");
    }
    return result;
  }

  /**
   * Return a single result object from the given Collection.
   * <p>Throws an exception if 0 or more than 1 element found.
   *
   * @param results the result Collection (can be {@code null}
   * and is also expected to contain {@code null} elements)
   * @return the single result object
   * @throws IncorrectResultSizeDataAccessException if more than one
   * element has been found in the given Collection
   * @throws EmptyResultDataAccessException if no element at all
   * has been found in the given Collection
   */
  @Nullable
  public static <T> T nullableSingleResult(@Nullable Collection<T> results) throws IncorrectResultSizeDataAccessException {
    // This is identical to the requiredSingleResult implementation but differs in the
    // semantics of the incoming Collection (which we currently can't formally express)
    if (CollectionUtils.isEmpty(results)) {
      throw new EmptyResultDataAccessException(1);
    }
    if (results.size() > 1) {
      throw new IncorrectResultSizeDataAccessException(1, results.size());
    }
    return results.iterator().next();
  }

  /**
   * Return a unique result object from the given Collection.
   * <p>Returns {@code null} if 0 result objects found;
   * throws an exception if more than 1 instance found.
   *
   * @param results the result Collection (can be {@code null})
   * @return the unique result object, or {@code null} if none
   * @throws IncorrectResultSizeDataAccessException if more than one
   * result object has been found in the given Collection
   * @see CollectionUtils#hasUniqueObject
   */
  @Nullable
  public static <T> T uniqueResult(@Nullable Collection<T> results) throws IncorrectResultSizeDataAccessException {
    if (CollectionUtils.isEmpty(results)) {
      return null;
    }
    if (!CollectionUtils.hasUniqueObject(results)) {
      throw new IncorrectResultSizeDataAccessException(1, results.size());
    }
    return results.iterator().next();
  }

  /**
   * Return a unique result object from the given Collection.
   * <p>Throws an exception if 0 or more than 1 instance found.
   *
   * @param results the result Collection (can be {@code null}
   * but is not expected to contain {@code null} elements)
   * @return the unique result object
   * @throws IncorrectResultSizeDataAccessException if more than one
   * result object has been found in the given Collection
   * @throws EmptyResultDataAccessException if no result object at all
   * has been found in the given Collection
   * @see CollectionUtils#hasUniqueObject
   */
  public static <T> T requiredUniqueResult(@Nullable Collection<T> results) throws IncorrectResultSizeDataAccessException {
    if (CollectionUtils.isEmpty(results)) {
      throw new EmptyResultDataAccessException(1);
    }
    if (!CollectionUtils.hasUniqueObject(results)) {
      throw new IncorrectResultSizeDataAccessException(1, results.size());
    }
    T result = results.iterator().next();
    if (result == null) {
      throw new TypeMismatchDataAccessException("Result value is null but no null value expected");
    }
    return result;
  }

  /**
   * Return a unique result object from the given Collection.
   * Throws an exception if 0 or more than 1 result objects found,
   * of if the unique result object is not convertible to the
   * specified required type.
   *
   * @param results the result Collection (can be {@code null}
   * but is not expected to contain {@code null} elements)
   * @return the unique result object
   * @throws IncorrectResultSizeDataAccessException if more than one
   * result object has been found in the given Collection
   * @throws EmptyResultDataAccessException if no result object
   * at all has been found in the given Collection
   * @throws TypeMismatchDataAccessException if the unique object does
   * not match the specified required type
   */
  @SuppressWarnings("unchecked")
  public static <T> T objectResult(@Nullable Collection<?> results, @Nullable Class<T> requiredType)
          throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {

    Object result = requiredUniqueResult(results);
    if (requiredType != null && !requiredType.isInstance(result)) {
      if (String.class == requiredType) {
        result = result.toString();
      }
      else if (Number.class.isAssignableFrom(requiredType) && result instanceof Number number) {
        try {
          result = NumberUtils.convertNumberToTargetClass(number, (Class<? extends Number>) requiredType);
        }
        catch (IllegalArgumentException ex) {
          throw new TypeMismatchDataAccessException(ex.getMessage());
        }
      }
      else {
        throw new TypeMismatchDataAccessException(
                "Result object is of type [" + result.getClass().getName() +
                        "] and could not be converted to required type [" + requiredType.getName() + "]");
      }
    }
    return (T) result;
  }

  /**
   * Return a unique int result from the given Collection.
   * Throws an exception if 0 or more than 1 result objects found,
   * of if the unique result object is not convertible to an int.
   *
   * @param results the result Collection (can be {@code null}
   * but is not expected to contain {@code null} elements)
   * @return the unique int result
   * @throws IncorrectResultSizeDataAccessException if more than one
   * result object has been found in the given Collection
   * @throws EmptyResultDataAccessException if no result object
   * at all has been found in the given Collection
   * @throws TypeMismatchDataAccessException if the unique object
   * in the collection is not convertible to an int
   */
  public static int intResult(@Nullable Collection<?> results)
          throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {

    return objectResult(results, Number.class).intValue();
  }

  /**
   * Return a unique long result from the given Collection.
   * Throws an exception if 0 or more than 1 result objects found,
   * of if the unique result object is not convertible to a long.
   *
   * @param results the result Collection (can be {@code null}
   * but is not expected to contain {@code null} elements)
   * @return the unique long result
   * @throws IncorrectResultSizeDataAccessException if more than one
   * result object has been found in the given Collection
   * @throws EmptyResultDataAccessException if no result object
   * at all has been found in the given Collection
   * @throws TypeMismatchDataAccessException if the unique object
   * in the collection is not convertible to a long
   */
  public static long longResult(@Nullable Collection<?> results)
          throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {

    return objectResult(results, Number.class).longValue();
  }

  /**
   * Return a translated exception if this is appropriate,
   * otherwise return the given exception as-is.
   *
   * @param rawException an exception that we may wish to translate
   * @param pet the PersistenceExceptionTranslator to use to perform the translation
   * @return a translated persistence exception if translation is possible,
   * or the raw exception if it is not
   */
  public static RuntimeException translateIfNecessary(
          RuntimeException rawException, PersistenceExceptionTranslator pet) {

    Assert.notNull(pet, "PersistenceExceptionTranslator is required");
    DataAccessException dae = pet.translateExceptionIfPossible(rawException);
    return (dae != null ? dae : rawException);
  }

}
