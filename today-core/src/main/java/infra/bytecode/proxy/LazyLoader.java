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
package infra.bytecode.proxy;

/**
 * Lazy-loading {@link Enhancer} callback.
 */
@FunctionalInterface
public interface LazyLoader extends Callback {

  /**
   * Return the object which the original method invocation should be dispatched.
   * Called as soon as the first lazily-loaded method in the enhanced instance is
   * invoked. The same object is then used for every future method call to the
   * proxy instance.
   *
   * @return an object that can invoke the method
   */
  Object loadObject() throws Exception;
}
