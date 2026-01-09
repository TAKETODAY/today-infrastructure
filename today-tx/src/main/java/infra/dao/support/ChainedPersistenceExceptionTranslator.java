/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.dao.support;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

import infra.dao.DataAccessException;
import infra.lang.Assert;

/**
 * Implementation of {@link PersistenceExceptionTranslator} that supports chaining,
 * allowing the addition of PersistenceExceptionTranslator instances in order.
 * Returns {@code non-null} on the first (if any) match.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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
