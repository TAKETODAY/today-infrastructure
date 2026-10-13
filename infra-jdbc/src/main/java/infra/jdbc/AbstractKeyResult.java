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
import infra.dao.DataAccessException;
import infra.jdbc.type.TypeHandler;
import infra.util.Assert;
import infra.util.CollectionUtils;

/**
 * Abstract base class for operation results that carry generated keys, such as
 * {@link UpdateResult} and {@link BatchResult}. It centralizes the retrieval and
 * conversion of generated keys after an update or batch operation.
 *
 * <p>Generated keys must be fetched from the database via {@link #setKeys(ResultSet, TypeHandler)}
 * or {@link #addKeys(ResultSet, TypeHandler)} before any of the {@code get} methods are called;
 * otherwise a {@link GeneratedKeysException} is thrown.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Retrieve the first generated key using {@link #getFirstKey()}.</li>
 *   <li>Access all generated keys using {@link #getKeys()}, {@link #getKeysArray(Class)},
 *   and {@link #getKeys(Class)}.</li>
 *   <li>Support for type conversion of generated keys using a {@link ConversionService}.</li>
 * </ul>
 *
 * @param <T> the type of generated keys
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public abstract class AbstractKeyResult<T extends @Nullable Object> extends ExecutionResult {

  private @Nullable ArrayList<T> generatedKeys;

  /**
   * Creates a new {@code AbstractKeyResult} associated with the given connection.
   *
   * @param connection the {@link JdbcConnection} used to access the database
   */
  protected AbstractKeyResult(JdbcConnection connection) {
    super(connection);
  }

  /**
   * Adds the generated keys read from the given result set to the internally accumulated keys.
   * Repeated calls accumulate keys, which is suitable for incremental batch executions.
   *
   * @param rs the {@link ResultSet} holding the generated keys, must not be {@code null}
   * @param handler the {@link TypeHandler} used to read each generated key, must not be {@code null}
   * @throws DataAccessException if a {@link SQLException} occurs while reading the keys
   */
  protected final void addKeys(ResultSet rs, TypeHandler<T> handler) {
    ArrayList<T> keys = this.generatedKeys;
    if (keys == null) {
      keys = new ArrayList<>();
      this.generatedKeys = keys;
    }
    readGeneratedKeys(rs, handler, keys);
  }

  /**
   * Replaces the internally stored generated keys with those read from the given result set.
   *
   * @param rs the {@link ResultSet} holding the generated keys, must not be {@code null}
   * @param handler the {@link TypeHandler} used to read each generated key, must not be {@code null}
   * @throws DataAccessException if a {@link SQLException} occurs while reading the keys
   */
  protected final void setKeys(ResultSet rs, TypeHandler<T> handler) {
    ArrayList<T> keys = new ArrayList<>();
    readGeneratedKeys(rs, handler, keys);
    this.generatedKeys = keys;
  }

  private void readGeneratedKeys(ResultSet rs, TypeHandler<T> handler, ArrayList<T> keys) {
    Throwable failure = null;
    try {
      while (rs.next()) {
        keys.add(handler.getResult(rs, 1));
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
   * Returns the first generated key, if available.
   *
   * <p>This method retrieves the first element from the generated keys collection
   * using {@code CollectionUtils.firstElement}. If no keys are present, it returns
   * {@code null}.
   *
   * @return the first generated key, or {@code null} if no keys are available
   * @throws GeneratedKeysException if no generated keys were fetched from the database
   */
  public @Nullable T getFirstKey() {
    return CollectionUtils.firstElement(generatedKeys());
  }

  /**
   * Retrieves the first generated key, converted to the specified return type.
   *
   * <p>This method retrieves the first generated key using {@link #getFirstKey()} and converts it
   * to the desired type using a {@link ConversionService} obtained from the manager. If no keys
   * are available, or if the conversion fails, appropriate exceptions are thrown.
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
   * Retrieves the first generated key, converted to the specified return type using the provided
   * {@link ConversionService}.
   *
   * <p>This method retrieves the first generated key using {@link #getFirstKey()} and converts it
   * to the desired type using the given {@link ConversionService}. If no keys are available, or
   * if the conversion fails, appropriate exceptions are thrown.
   *
   * @param <V> the type of the return value
   * @param returnType the class object representing the desired return type
   * @param conversionService the {@link ConversionService} used to convert the generated key
   * @return the first generated key, converted to the specified return type
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
   * Returns an array of all generated keys.
   *
   * <p>This method retrieves all generated keys stored in the internal collection and returns
   * them as an array. If no keys are available, an empty array is returned.
   *
   * @return an array containing all generated keys, or an empty array if no keys are available
   * @throws GeneratedKeysException if no generated keys were fetched from the database
   * (e.g., if the {@code returnGeneratedKeys} parameter was not set in the query)
   */
  public Object[] getKeys() {
    return generatedKeys().toArray();
  }

  /**
   * Returns an array of all generated keys, converted to the specified component type.
   *
   * <p>This method retrieves all generated keys stored in the internal collection and returns
   * them as an array of the specified type. If no keys are available, an empty array is returned.
   * The method uses reflection to create an array of the specified component type and populates
   * it with the generated keys.
   *
   * @param <V> the type of the array elements
   * @param componentType the class object representing the component type of the array to be returned
   * @return an array containing all generated keys, converted to the specified component type
   * @throws GeneratedKeysException if no generated keys were fetched from the database
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
   * Retrieves all generated keys, converted to the specified return type.
   *
   * <p>This method retrieves all generated keys stored in the internal collection and converts
   * each key to the desired type using a {@link ConversionService} obtained from the manager.
   * If no keys are available, or if the conversion fails, appropriate exceptions are thrown.
   *
   * @param <V> the type of the return value
   * @param returnType the class object representing the desired return type for the keys
   * @return a list containing all generated keys, converted to the specified return type
   * @throws GeneratedKeysConversionException if the conversion of any generated key fails
   * @throws IllegalArgumentException if the conversion service is null
   */
  public <V> List<V> getKeys(Class<V> returnType) {
    return getKeys(returnType, getManager().getConversionService());
  }

  /**
   * Retrieves all generated keys, converted to the specified return type using the provided
   * {@link ConversionService}.
   *
   * <p>This method retrieves all generated keys stored in the internal collection and converts
   * each key to the desired type using the given {@link ConversionService}. If no keys are
   * available, or if the conversion fails, appropriate exceptions are thrown.
   *
   * @param <V> the type of the return value
   * @param returnType the class object representing the desired return type for the keys
   * @param conversionService the {@link ConversionService} used to convert the generated keys
   * @return a list containing all generated keys, converted to the specified return type
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