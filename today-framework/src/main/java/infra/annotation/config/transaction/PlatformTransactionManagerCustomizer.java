/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.annotation.config.transaction;

import infra.transaction.PlatformTransactionManager;

/**
 * Callback interface that can be implemented by beans wishing to customize
 * {@link PlatformTransactionManager PlatformTransactionManagers} whilst retaining default
 * auto-configuration.
 *
 * @param <T> the transaction manager type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface PlatformTransactionManagerCustomizer<T extends PlatformTransactionManager> {

  /**
   * Customize the given transaction manager.
   *
   * @param transactionManager the transaction manager to customize
   */
  void customize(T transactionManager);

}
