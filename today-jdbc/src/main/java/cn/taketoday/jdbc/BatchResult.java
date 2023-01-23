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

import java.sql.PreparedStatement;
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
 * Batch execution result
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/17 11:25
 */
public class BatchResult extends ExecutionResult {

  @Nullable
  private int[] batchResult;

  @Nullable
  private ArrayList<Object> generatedKeys;

  @Nullable
  private Integer affectedRows;

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
    try {
      while (rs.next()) {
        T generatedKey = handler.getResult(rs, 1);
        keys.add(generatedKey);
      }
    }
    catch (SQLException e) {
      throw translateException("Getting generated keys.", e);
    }
  }

  void addKeys(ResultSet rs) {
    ArrayList<Object> keys = this.generatedKeys;
    if (keys == null) {
      keys = new ArrayList<>();
      this.generatedKeys = keys;
    }
    try {
      while (rs.next()) {
        keys.add(rs.getObject(1));
      }
    }
    catch (SQLException e) {
      throw translateException("Getting generated keys.", e);
    }
  }

  @Nullable
  public Object getKey() {
    assertCanGetKeys();
    List<Object> keys = this.generatedKeys;
    if (CollectionUtils.isNotEmpty(keys)) {
      return keys.get(0);
    }
    return null;
  }

  /**
   * @throws GeneratedKeysConversionException Generated Keys conversion failed
   * @throws IllegalArgumentException If conversionService is null
   */
  public <V> V getKey(Class<V> returnType) {
    return getKey(returnType, getManager().getConversionService());
  }

  /**
   * @throws GeneratedKeysConversionException Generated Keys conversion failed
   * @throws IllegalArgumentException If conversionService is null
   */
  public <V> V getKey(Class<V> returnType, ConversionService conversionService) {
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

  public Object[] getKeys() {
    assertCanGetKeys();
    if (generatedKeys != null) {
      return generatedKeys.toArray();
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
