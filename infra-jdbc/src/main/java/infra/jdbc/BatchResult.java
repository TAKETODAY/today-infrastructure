/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc;

import org.jspecify.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import infra.core.conversion.ConversionException;
import infra.core.conversion.ConversionService;
import infra.jdbc.type.TypeHandler;
import infra.util.Assert;
import infra.util.CollectionUtils;

/**
 * Batch execution result
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/17 11:25
 */
public class BatchResult extends ExecutionResult {

  private int @Nullable [] batchResult;

  private @Nullable ArrayList<Object> generatedKeys;

  private @Nullable Integer affectedRows;

  /**
   * Creates a new {@code BatchResult} associated with the given connection.
   *
   * @param connection the {@link JdbcConnection} used to access the database
   */
  public BatchResult(JdbcConnection connection) {
    super(connection);
  }

  /**
   * Get last batch execution result
   * <p>
   *
   * Maybe explicitly execution
   *
   * @see NamedQuery#addToBatch()
   * @see PreparedStatement#executeBatch()
   */
  public int[] getLastBatchResult() {
    if (batchResult == null) {
      throw new PersistenceException(
              "It is required to call executeBatch() method before calling getLastBatchResult().");
    }
    return batchResult;
  }

  void setBatchResult(int[] batchResult) {
    this.batchResult = batchResult;
    if (this.affectedRows == null) {
      this.affectedRows = batchResult.length;
    }
    else {
      this.affectedRows += batchResult.length;
    }
  }

  /**
   * @return the number of rows updated or deleted
   */
  public int getAffectedRows() {
    if (affectedRows == null) {
      throw new PersistenceException(
              "It is required to call executeBatch() method before calling getAffectedRows().");
    }
    return affectedRows;
  }

  // ------------------------------------------------
  // -------------------- Keys ----------------------
  // ------------------------------------------------

  <T> void addKeys(ResultSet rs, TypeHandler<T> handler) {
    ArrayList<Object> keys = this.generatedKeys;
    if (keys == null) {
      keys = new ArrayList<>();
      this.generatedKeys = keys;
    }
    Throwable failure = null;
    try {
      while (rs.next()) {
        T generatedKey = handler.getResult(rs, 1);
        keys.add(generatedKey);
      }
    }
    catch (SQLException e) {
      failure = e;
      throw translateException("Getting generated keys.", e);
    }
    catch (RuntimeException | Error ex) {
      failure = ex;
      throw ex;
    }
    finally {
      getManager().closeResource(rs, null, failure);
    }
  }

  /**
   * Returns the first generated key of the last batch execution, or {@code null}
   * if no generated keys were fetched.
   *
   * @return the first generated key, or {@code null} if none is available
   * @throws GeneratedKeysException if no generated keys were fetched from the database
   */
  public @Nullable Object getKey() {
    assertCanGetKeys();
    List<Object> keys = this.generatedKeys;
    if (CollectionUtils.isNotEmpty(keys)) {
      return keys.get(0);
    }
    return null;
  }

  /**
   * Returns the first generated key of the last batch execution, converted to the
   * specified return type using the default conversion service.
   *
   * @param <V> the type of the generated key
   * @param returnType the target type of the generated key, must not be {@code null}
   * @return the converted first generated key
   * @throws GeneratedKeysConversionException Generated Keys conversion failed
   * @throws IllegalArgumentException If conversionService is null
   */
  public <V extends @Nullable Object> V getKey(Class<V> returnType) {
    return getKey(returnType, getManager().getConversionService());
  }

  /**
   * Returns the first generated key of the last batch execution, converted to the
   * specified return type using the provided conversion service.
   *
   * @param <V> the type of the generated key
   * @param returnType the target type of the generated key, must not be {@code null}
   * @param conversionService the {@link ConversionService} used to convert the key, must not be {@code null}
   * @return the converted first generated key
   * @throws GeneratedKeysConversionException Generated Keys conversion failed
   * @throws IllegalArgumentException If conversionService is null
   */
  public <V extends @Nullable Object> V getKey(Class<V> returnType, ConversionService conversionService) {
    Assert.notNull(conversionService, "conversionService is required");
    Object key = getKey();
    try {
      return conversionService.convert(key, returnType);
    }
    catch (ConversionException e) {
      throw new GeneratedKeysConversionException(
              "Exception occurred while converting value from database to type " + returnType.toString(), e);
    }
  }

  /**
   * Returns all generated keys of the last batch execution, or {@code null} if no
   * generated keys were fetched.
   *
   * @return an array containing all generated keys, or {@code null} if none is available
   * @throws GeneratedKeysException if no generated keys were fetched from the database
   */
  public Object @Nullable [] getKeys() {
    assertCanGetKeys();
    if (generatedKeys != null) {
      return generatedKeys.toArray();
    }
    return null;
  }

  /**
   * Returns all generated keys of the last batch execution, converted to the specified
   * return type using the default conversion service.
   *
   * @param <V> the type of the generated keys
   * @param returnType the target type of the generated keys, must not be {@code null}
   * @return a list containing all converted generated keys, or {@code null} if no
   * generated keys were fetched
   * @throws GeneratedKeysConversionException cannot converting value from database
   * @throws IllegalArgumentException If conversionService is null
   */
  public <V> @Nullable List<V> getKeys(Class<V> returnType) {
    return getKeys(returnType, getManager().getConversionService());
  }

  /**
   * Returns all generated keys of the last batch execution, converted to the specified
   * return type using the provided conversion service.
   *
   * @param <V> the type of the generated keys
   * @param returnType the target type of the generated keys, must not be {@code null}
   * @param conversionService the {@link ConversionService} used to convert the keys, must not be {@code null}
   * @return a list containing all converted generated keys, or {@code null} if no
   * generated keys were fetched
   * @throws GeneratedKeysConversionException cannot converting value from database
   * @throws IllegalArgumentException If conversionService is null
   */
  public <V> @Nullable List<V> getKeys(Class<V> returnType, ConversionService conversionService) {
    assertCanGetKeys();
    if (generatedKeys != null) {
      Assert.notNull(conversionService, "conversionService is required");
      try {
        ArrayList<V> convertedKeys = new ArrayList<>(generatedKeys.size());
        for (Object key : generatedKeys) {
          convertedKeys.add(conversionService.convert(key, returnType));
        }
        return convertedKeys;
      }
      catch (ConversionException e) {
        throw new GeneratedKeysConversionException(
                "Exception occurred while converting value from database to type " + returnType, e);
      }
    }
    return null;
  }

  private void assertCanGetKeys() {
    if (generatedKeys == null) {
      throw new GeneratedKeysException(
              "Keys where not fetched from database." +
                      " Please set the returnGeneratedKeys parameter " +
                      "in the createQuery() method to enable fetching of generated keys.");
    }

  }

}
