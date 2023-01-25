/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * Update execution result
 *
 * @param <T> generatedKey type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/17 10:28
 */
public class UpdateResult<T> extends ExecutionResult {

  @Nullable
  private final Integer affectedRows;

  @Nullable
  private ArrayList<T> generatedKeys;

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
   * Get first generated-key
   *
   * @return first generated-key
   */
  @Nullable
  public T getFirstKey() {
    return CollectionUtils.firstElement(generatedKeys());
  }

  /**
   * @throws GeneratedKeysConversionException Generated Keys conversion failed
   * @throws IllegalArgumentException If conversionService is null
   */
  public <V> V getFirstKey(Class<V> returnType) {
    return getFirstKey(returnType, getManager().getConversionService());
  }

  /**
   * @throws GeneratedKeysConversionException Generated Keys conversion failed
   * @throws IllegalArgumentException If conversionService is null
   */
  public <V> V getFirstKey(Class<V> returnType, ConversionService conversionService) {
    Assert.notNull(conversionService, "conversionService is required");
    Object key = getFirstKey();
    try {
      return conversionService.convert(key, returnType);
    }
    catch (ConversionException e) {
      throw new GeneratedKeysConversionException(
              "Exception occurred while converting value from database to type " + returnType.toString(), e);
    }
  }

  public Object[] getKeys() {
    ArrayList<T> generatedKeys = generatedKeys();
    if (generatedKeys != null) {
      return generatedKeys.toArray();
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <V> V[] getKeysArray(Class<V> componentType) {
    ArrayList<T> generatedKeys = generatedKeys();
    if (generatedKeys != null) {
      V[] o = (V[]) Array.newInstance(componentType, generatedKeys.size());
      return generatedKeys.toArray(o);
    }
    return null;
  }

  /**
   * @throws GeneratedKeysConversionException cannot converting value from database
   * @throws IllegalArgumentException If conversionService is null
   */
  @Nullable
  public <V> List<V> getKeys(Class<V> returnType) {
    return getKeys(returnType, getManager().getConversionService());
  }

  /**
   * @throws GeneratedKeysConversionException cannot converting value from database
   * @throws IllegalArgumentException If conversionService is null
   */
  @Nullable
  public <V> List<V> getKeys(Class<V> returnType, ConversionService conversionService) {
    ArrayList<T> generatedKeys = generatedKeys();
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
