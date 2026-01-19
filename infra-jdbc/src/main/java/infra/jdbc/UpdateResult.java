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

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import infra.core.conversion.ConversionException;
import infra.core.conversion.ConversionService;
import infra.jdbc.type.TypeHandler;
import infra.lang.Assert;
import infra.util.CollectionUtils;

/**
 * Represents the result of an update operation in the database.
 * This class extends {@link ExecutionResult} and provides methods to retrieve
 * information about the affected rows and generated keys after an update or insert operation.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Retrieve the number of affected rows using {@link #getAffectedRows()}.</li>
 *   <li>Access generated keys using methods like {@link #getFirstKey()}, {@link #getKeys()}, and {@link #getKeys(Class)}.</li>
 *   <li>Support for type conversion of generated keys using a {@link ConversionService}.</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Execute an update query with returnGeneratedKeys set to true
 * Query query = executionResult.createQuery("INSERT INTO users (name) VALUES ('John Doe')", true);
 * UpdateResult<Long> result = query.executeUpdate();
 *
 * // Retrieve the number of affected rows
 * int affectedRows = result.getAffectedRows();
 * System.out.println("Affected Rows: " + affectedRows);
 *
 * // Retrieve the first generated key
 * Long firstKey = result.getFirstKey();
 * System.out.println("First Generated Key: " + firstKey);
 *
 * // Retrieve all generated keys as a list of Strings
 * List<String> keysAsStrings = result.getKeys(String.class);
 * System.out.println("Generated Keys as Strings: " + keysAsStrings);
 * }</pre>
 *
 * <p><strong>Notes:</strong>
 * <ul>
 *   <li>To fetch generated keys, ensure that the `returnGeneratedKeys` parameter is set to `true`
 *       when creating the query using {@link ExecutionResult#createQuery(String, boolean)}.</li>
 *   <li>If generated keys are not fetched, calling methods like {@link #getFirstKey()} or {@link #getKeys()}
 *       will throw a {@link GeneratedKeysException}.</li>
 *   <li>Type conversion for generated keys requires a valid {@link ConversionService} instance.</li>
 * </ul>
 *
 * @param <T> the type of generated keys
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/17 10:28
 */
public class UpdateResult<T extends @Nullable Object> extends ExecutionResult {

  private final @Nullable Integer affectedRows;

  private @Nullable ArrayList<T> generatedKeys;

  public UpdateResult(@Nullable Integer affectedRows, JdbcConnection connection) {
    super(connection);
    this.affectedRows = affectedRows;
  }

  /**
   * @return the number of rows updated or deleted
   */
  public int getAffectedRows() {
    if (affectedRows == null) {
      throw new PersistenceException(
              "It is required to call executeUpdate() method before calling getAffectedRows().");
    }
    return affectedRows;
  }

  // ------------------------------------------------
  // -------------------- Keys ----------------------
  // ------------------------------------------------

  void setKeys(ResultSet rs, TypeHandler<T> generatedKeyHandler) {
    try (rs) {
      ArrayList<T> keys = new ArrayList<>();
      while (rs.next()) {
        keys.add(generatedKeyHandler.getResult(rs, 1));
      }
      this.generatedKeys = keys;
    }
    catch (SQLException e) {
      throw translateException("Getting generated keys.", e);
    }
  }

  /**
   * Returns the first generated key from the result set, if available.
   *
   * <p>This method retrieves the first element from the generated keys collection
   * using {@code CollectionUtils.firstElement}. If no keys are present, it returns
   * {@code null}.
   *
   * @return the first generated key, or {@code null} if no keys are available
   */
  public @Nullable T getFirstKey() {
    return CollectionUtils.firstElement(generatedKeys());
  }

  /**
   * Retrieves the first generated key from the result set, converted to the specified return type.
   *
   * <p>This method retrieves the first generated key using {@link #getFirstKey()} and converts it
   * to the desired type using a {@link ConversionService} obtained from the manager. If no keys
   * are available, or if the conversion fails, appropriate exceptions are thrown.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   * UpdateResult result = updateOperation.execute();
   * Long firstKey = result.getFirstKey(Long.class);
   * System.out.println("First generated key: " + firstKey);
   * }</pre>
   *
   * @param <V> the type of the return value
   * @param returnType the class object representing the desired return type
   * @return the first generated key, converted to the specified return type
   * @throws GeneratedKeysConversionException if the conversion of the generated key fails
   * @throws IllegalArgumentException if the conversion service is null
   */
  public <V extends @Nullable Object> V getFirstKey(Class<V> returnType) {
    return getFirstKey(returnType, getManager().getConversionService());
  }

  /**
   * Retrieves the first generated key from the result set, converted to the specified return type
   * using the provided {@link ConversionService}.
   *
   * <p>This method retrieves the first generated key using {@link #getFirstKey()} and converts it
   * to the desired type using the given {@link ConversionService}. If no keys are available, or
   * if the conversion fails, appropriate exceptions are thrown.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   * UpdateResult result = updateOperation.execute();
   * ConversionService conversionService = new DefaultConversionService();
   * Long firstKey = result.getFirstKey(Long.class, conversionService);
   * System.out.println("First generated key: " + firstKey);
   * }</pre>
   *
   * @param <V> the type of the return value
   * @param returnType the class object representing the desired return type
   * @param conversionService the {@link ConversionService} used to convert the generated key
   * @return the first generated key, converted to the specified return type, or {@code null}
   * if no keys are available
   * @throws GeneratedKeysConversionException if the conversion of the generated key fails
   * @throws IllegalArgumentException if the {@code conversionService} is {@code null}
   */
  public <V extends @Nullable Object> V getFirstKey(Class<V> returnType, ConversionService conversionService) {
    Assert.notNull(conversionService, "conversionService is required");
    Object key = getFirstKey();
    try {
      return conversionService.convert(key, returnType);
    }
    catch (ConversionException e) {
      throw new GeneratedKeysConversionException(
              "Exception occurred while converting value from database to type " + returnType, e);
    }
  }

  /**
   * Returns an array of all generated keys from the result set, if available.
   *
   * <p>This method retrieves all generated keys stored in the internal collection
   * and returns them as an array. If no keys are available, an empty array is returned.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   * UpdateResult result = updateOperation.execute();
   * Object[] keys = result.getKeys();
   * for (Object key : keys) {
   *   System.out.println("Generated key: " + key);
   * }
   * }</pre>
   *
   * @return an array containing all generated keys, or an empty array if no keys are available
   * @throws GeneratedKeysException if the generated keys were not fetched from the database
   * (e.g., if the {@code returnGeneratedKeys} parameter was not set in the query)
   */
  public Object[] getKeys() {
    ArrayList<T> generatedKeys = generatedKeys();
    return generatedKeys.toArray();
  }

  /**
   * Returns an array of all generated keys from the result set, converted to the specified component type.
   *
   * <p>This method retrieves all generated keys stored in the internal collection and returns them as an
   * array of the specified type. If no keys are available, an empty array is returned. The method uses
   * reflection to create an array of the specified component type and populates it with the generated keys.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   * UpdateResult result = updateOperation.execute();
   * Long[] keys = result.getKeysArray(Long.class);
   * for (Long key : keys) {
   *   System.out.println("Generated key: " + key);
   * }
   * }</pre>
   *
   * @param <V> the type of the array elements
   * @param componentType the class object representing the component type of the array to be returned
   * @return an array containing all generated keys, converted to the specified component type
   * @throws GeneratedKeysException if the generated keys were not fetched from the database
   * (e.g., if the {@code returnGeneratedKeys} parameter was not set in the query)
   * @throws ArrayStoreException if the generated keys cannot be stored in an array of the specified type
   */
  @SuppressWarnings("unchecked")
  public <V extends @Nullable Object> V[] getKeysArray(Class<V> componentType) {
    ArrayList<T> generatedKeys = generatedKeys();
    V[] o = (V[]) Array.newInstance(componentType, generatedKeys.size());
    return generatedKeys.toArray(o);
  }

  /**
   * Retrieves all generated keys from the result set, converted to the specified return type.
   *
   * <p>This method retrieves all generated keys stored in the internal collection and converts
   * each key to the desired type using a {@link ConversionService} obtained from the manager.
   * If no keys are available, or if the conversion fails, appropriate exceptions are thrown.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   * UpdateResult result = updateOperation.execute();
   * List<Long> keys = result.getKeys(Long.class);
   * for (Long key : keys) {
   *   System.out.println("Generated key: " + key);
   * }
   * }</pre>
   *
   * @param <V> the type of the return value
   * @param returnType the class object representing the desired return type for the keys
   * @return a list containing all generated keys, converted to the specified return type,
   * or {@code null} if no keys are available
   * @throws GeneratedKeysConversionException if the conversion of any generated key fails
   * @throws IllegalArgumentException if the conversion service is null
   */
  public <V> List<V> getKeys(Class<V> returnType) {
    return getKeys(returnType, getManager().getConversionService());
  }

  /**
   * Retrieves all generated keys from the result set, converted to the specified return type
   * using the provided {@link ConversionService}.
   *
   * <p>This method retrieves all generated keys stored in the internal collection and converts
   * each key to the desired type using the given {@link ConversionService}. If no keys are
   * available, or if the conversion fails, appropriate exceptions are thrown.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   * UpdateResult result = updateOperation.execute();
   * ConversionService conversionService = new DefaultConversionService();
   * List<Long> keys = result.getKeys(Long.class, conversionService);
   * for (Long key : keys) {
   *   System.out.println("Generated key: " + key);
   * }
   * }</pre>
   *
   * @param <V> the type of the return value
   * @param returnType the class object representing the desired return type for the keys
   * @param conversionService the {@link ConversionService} used to convert the generated keys
   * @return a list containing all generated keys, converted to the specified return type,
   * or {@code null} if no keys are available
   * @throws GeneratedKeysConversionException if the conversion of any generated key fails
   * @throws IllegalArgumentException if the {@code conversionService} is {@code null}
   */
  public <V> List<V> getKeys(Class<V> returnType, ConversionService conversionService) {
    ArrayList<T> generatedKeys = generatedKeys();
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

  private ArrayList<T> generatedKeys() {
    if (generatedKeys == null) {
      throw new GeneratedKeysException(
              "Keys where not fetched from database." +
                      " Please set the returnGeneratedKeys parameter " +
                      "in the createQuery() method to enable fetching of generated keys.");
    }
    return generatedKeys;
  }

}
