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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.taketoday.dao.DataRetrievalFailureException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.lang.Nullable;

/**
 * The standard implementation of the {@link KeyHolder} interface, to be used for
 * holding auto-generated keys (as potentially returned by JDBC insert statements).
 *
 * <p>Create an instance of this class for each insert operation, and pass it
 * to the corresponding {@link cn.taketoday.jdbc.core.JdbcTemplate} or
 * {@link cn.taketoday.jdbc.object.SqlUpdate} methods.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Slawomir Dymitrow
 * @since 4.0
 */
public class GeneratedKeyHolder implements KeyHolder {

  private final List<Map<String, Object>> keyList;

  /**
   * Create a new GeneratedKeyHolder with a default list.
   */
  public GeneratedKeyHolder() {
    this.keyList = new ArrayList<>(1);
  }

  /**
   * Create a new GeneratedKeyHolder with a given list.
   *
   * @param keyList a list to hold maps of keys
   */
  public GeneratedKeyHolder(List<Map<String, Object>> keyList) {
    this.keyList = keyList;
  }

  @Override
  @Nullable
  public Number getKey() throws InvalidDataAccessApiUsageException, DataRetrievalFailureException {
    return getKeyAs(Number.class);
  }

  @Override
  @Nullable
  public <T> T getKeyAs(Class<T> keyType) throws InvalidDataAccessApiUsageException, DataRetrievalFailureException {
    if (this.keyList.isEmpty()) {
      return null;
    }
    if (this.keyList.size() > 1 || this.keyList.get(0).size() > 1) {
      throw new InvalidDataAccessApiUsageException(
              "The getKey method should only be used when a single key is returned. " +
                      "The current key entry contains multiple keys: " + this.keyList);
    }
    Iterator<Object> keyIter = this.keyList.get(0).values().iterator();
    if (keyIter.hasNext()) {
      Object key = keyIter.next();
      if (key == null || !(keyType.isAssignableFrom(key.getClass()))) {
        throw new DataRetrievalFailureException(
                "The generated key type is not supported. " +
                        "Unable to cast [" + (key != null ? key.getClass().getName() : null) +
                        "] to [" + keyType.getName() + "].");
      }
      return keyType.cast(key);
    }
    else {
      throw new DataRetrievalFailureException("Unable to retrieve the generated key. " +
              "Check that the table has an identity column enabled.");
    }
  }

  @Override
  @Nullable
  public Map<String, Object> getKeys() throws InvalidDataAccessApiUsageException {
    if (this.keyList.isEmpty()) {
      return null;
    }
    if (this.keyList.size() > 1) {
      throw new InvalidDataAccessApiUsageException(
              "The getKeys method should only be used when keys for a single row are returned. " +
                      "The current key list contains keys for multiple rows: " + this.keyList);
    }
    return this.keyList.get(0);
  }

  @Override
  public List<Map<String, Object>> getKeyList() {
    return this.keyList;
  }

}
