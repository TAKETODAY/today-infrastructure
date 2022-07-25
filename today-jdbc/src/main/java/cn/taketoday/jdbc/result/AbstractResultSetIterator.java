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

package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import cn.taketoday.jdbc.PersistenceException;

/**
 * Iterator for a {@link ResultSet}. Tricky part here is getting
 * {@link #hasNext()} to work properly, meaning it can be called multiple times
 * without calling {@link #next()}.
 *
 * @author TODAY
 */
public abstract class AbstractResultSetIterator<T> implements Iterator<T> {
  // fields needed to read result set
  protected final ResultSet resultSet;

  protected AbstractResultSetIterator(ResultSet rs) {
    this.resultSet = rs;
  }

  // fields needed to properly implement
  private ResultSetValue<T> next; // keep track of next item in case hasNext() is called multiple times
  private boolean resultSetFinished; // used to note when result set exhausted

  @Override
  public boolean hasNext() {
    // check if we already fetched next item
    if (next != null) {
      return true;
    }
    // check if result set already finished
    if (resultSetFinished) {
      return false;
    }
    // now fetch next item
    next = safeReadNext();
    // check if we got something
    if (next != null) {
      return true;
    }
    // no more items
    resultSetFinished = true;
    return false;
  }

  @Override
  public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    final ResultSetValue<T> result = next;
    next = null;
    return result.value;
  }

  private ResultSetValue<T> safeReadNext() {
    try {
      return resultSet.next() ? new ResultSetValue<>(readNext()) : null;
    }
    catch (SQLException ex) {
      throw new PersistenceException("Database error: " + ex.getMessage(), ex);
    }
  }

  protected abstract T readNext() throws SQLException;

  record ResultSetValue<T>(T value) { }

}
