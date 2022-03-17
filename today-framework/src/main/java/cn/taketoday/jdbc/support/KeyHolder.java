/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.support;

import java.util.List;
import java.util.Map;

import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.lang.Nullable;

/**
 * Interface for retrieving keys, typically used for auto-generated keys
 * as potentially returned by JDBC insert statements.
 *
 * <p>Implementations of this interface can hold any number of keys.
 * In the general case, the keys are returned as a List containing one Map
 * for each row of keys.
 *
 * <p>Most applications only use one key per row and process only one row at a
 * time in an insert statement. In these cases, just call {@link #getKey() getKey}
 * or {@link #getKeyAs(Class) getKeyAs} to retrieve the key. The value returned
 * by {@code getKey} is a {@link Number}, which is the usual type for auto-generated
 * keys. For any other auto-generated key type, use {@code getKeyAs} instead.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Slawomir Dymitrow
 * @see cn.taketoday.jdbc.core.JdbcTemplate
 * @see cn.taketoday.jdbc.object.SqlUpdate
 * @since 4.0
 */
public interface KeyHolder {

  /**
   * Retrieve the first item from the first map, assuming that there is just
   * one item and just one map, and that the item is a number.
   * This is the typical case: a single, numeric generated key.
   * <p>Keys are held in a List of Maps, where each item in the list represents
   * the keys for each row. If there are multiple columns, then the Map will have
   * multiple entries as well. If this method encounters multiple entries in
   * either the map or the list meaning that multiple keys were returned,
   * then an InvalidDataAccessApiUsageException is thrown.
   *
   * @return the generated key as a number
   * @throws InvalidDataAccessApiUsageException if multiple keys are encountered
   * @see #getKeyAs(Class)
   */
  @Nullable
  Number getKey() throws InvalidDataAccessApiUsageException;

  /**
   * Retrieve the first item from the first map, assuming that there is just
   * one item and just one map, and that the item is an instance of specified type.
   * This is a common case: a single generated key of the specified type.
   * <p>Keys are held in a List of Maps, where each item in the list represents
   * the keys for each row. If there are multiple columns, then the Map will have
   * multiple entries as well. If this method encounters multiple entries in
   * either the map or the list meaning that multiple keys were returned,
   * then an InvalidDataAccessApiUsageException is thrown.
   *
   * @param keyType the type of the auto-generated key
   * @return the generated key as an instance of specified type
   * @throws InvalidDataAccessApiUsageException if multiple keys are encountered
   * @see #getKey()
   * @since 4.0
   */
  @Nullable
  <T> T getKeyAs(Class<T> keyType) throws InvalidDataAccessApiUsageException;

  /**
   * Retrieve the first map of keys.
   * <p>If there are multiple entries in the list (meaning that multiple rows
   * had keys returned), then an InvalidDataAccessApiUsageException is thrown.
   *
   * @return the Map of generated keys for a single row
   * @throws InvalidDataAccessApiUsageException if keys for multiple rows are encountered
   */
  @Nullable
  Map<String, Object> getKeys() throws InvalidDataAccessApiUsageException;

  /**
   * Return a reference to the List that contains the keys.
   * <p>Can be used for extracting keys for multiple rows (an unusual case),
   * and also for adding new maps of keys.
   *
   * @return the List for the generated keys, with each entry representing
   * an individual row through a Map of column names and key values
   */
  List<Map<String, Object>> getKeyList();

}
