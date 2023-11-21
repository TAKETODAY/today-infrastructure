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

package cn.taketoday.dao.support;

import java.util.ArrayList;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Implementation of {@link PersistenceExceptionTranslator} that supports chaining,
 * allowing the addition of PersistenceExceptionTranslator instances in order.
 * Returns {@code non-null} on the first (if any) match.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ChainedPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

  /** List of PersistenceExceptionTranslators. */
  private final ArrayList<PersistenceExceptionTranslator> delegates = new ArrayList<>(4);

  /**
   * Add a PersistenceExceptionTranslator to the chained delegate list.
   */
  public final void addDelegate(PersistenceExceptionTranslator pet) {
    Assert.notNull(pet, "PersistenceExceptionTranslator is required");
    this.delegates.add(pet);
  }

  /**
   * Return all registered PersistenceExceptionTranslator delegates (as array).
   */
  public final PersistenceExceptionTranslator[] getDelegates() {
    return this.delegates.toArray(new PersistenceExceptionTranslator[0]);
  }

  @Override
  @Nullable
  public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
    for (PersistenceExceptionTranslator pet : this.delegates) {
      DataAccessException translatedDex = pet.translateExceptionIfPossible(ex);
      if (translatedDex != null) {
        return translatedDex;
      }
    }
    return null;
  }

}
