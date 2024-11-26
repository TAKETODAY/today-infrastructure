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

package infra.retry.interceptor;

/**
 * Strategy interface for recovery action when processing of an item fails.
 *
 * @param <T> the return type
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public interface MethodInvocationRecoverer<T> {

  /**
   * Recover gracefully from an error. Clients can call this if processing of the item
   * throws an unexpected exception. Caller can use the return value to decide whether
   * to try more corrective action or perhaps throw an exception.
   *
   * @param args the arguments for the method invocation that failed.
   * @param cause the cause of the failure that led to this recovery.
   * @return the value to be returned to the caller
   */
  T recover(Object[] args, Throwable cause);

}
