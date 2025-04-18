/*
 * Copyright 2017 - 2023 the original author or authors.
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

package infra.dao.support;

import java.util.HashMap;
import java.util.Map;

import infra.dao.DataAccessException;

public class MapPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

  // in to out
  private final Map<RuntimeException, RuntimeException> translations = new HashMap<>();

  public void addTranslation(RuntimeException in, RuntimeException out) {
    this.translations.put(in, out);
  }

  @Override
  public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
    return (DataAccessException) this.translations.get(ex);
  }

}
