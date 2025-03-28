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

package infra.aop.aspectj;

import infra.aop.PointcutAdvisor;

/**
 * Interface to be implemented by Framework AOP Advisors wrapping AspectJ
 * aspects that may have a lazy initialization strategy. For example,
 * a perThis instantiation model would mean lazy initialization of the advice.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface InstantiationModelAwarePointcutAdvisor extends PointcutAdvisor {

  /**
   * Return whether this advisor is lazily initializing its underlying advice.
   */
  boolean isLazy();

  /**
   * Return whether this advisor has already instantiated its advice.
   */
  boolean isAdviceInstantiated();

}
