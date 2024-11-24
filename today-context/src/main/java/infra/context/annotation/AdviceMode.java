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

package infra.context.annotation;

import infra.scheduling.annotation.AsyncConfigurationSelector;
import infra.scheduling.annotation.EnableAsync;

/**
 * Enumeration used to determine whether JDK proxy-based or
 * AspectJ weaving-based advice should be applied.
 *
 * @author Chris Beams
 * @see EnableAsync#mode()
 * @see AsyncConfigurationSelector#selectImports
 * @see infra.transaction.annotation.EnableTransactionManagement#mode()
 * @since 4.0
 */
public enum AdviceMode {

  /**
   * JDK proxy-based advice.
   */
  PROXY,

  /**
   * AspectJ weaving-based advice.
   */
  ASPECTJ

}
