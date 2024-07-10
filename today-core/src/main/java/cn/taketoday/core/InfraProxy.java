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

package cn.taketoday.core;

/**
 * Interface to be implemented by transparent resource proxies that need to be
 * considered as equal to the underlying resource, for example for consistent
 * lookup key comparisons. Note that this interface does imply such special
 * semantics and does not constitute a general-purpose mixin!
 *
 * <p>Such wrappers will automatically be unwrapped for key comparisons in
 * {@link cn.taketoday.transaction.support.TransactionSynchronizationManager}.
 *
 * <p>Only fully transparent proxies, e.g. for redirection or service lookups,
 * are supposed to implement this interface. Proxies that decorate the target
 * object with new behavior, such as AOP proxies, do <i>not</i> qualify here!
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.transaction.support.TransactionSynchronizationManager
 * @since 4.0 2021/12/10 23:03
 */
public interface InfraProxy {

  /**
   * Return the underlying resource (never {@code null}).
   */
  Object getWrappedObject();

}

