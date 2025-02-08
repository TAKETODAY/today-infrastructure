/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.classify;

/**
 * Base class for {@link Classifier} implementations. Provides default behaviour and some
 * convenience members, like constants.
 *
 * @param <C> the type of the thing to classify
 * @param <T> the output of the classifier
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@SuppressWarnings("serial")
public class ClassifierSupport<C, T> implements Classifier<C, T> {

  private final T defaultValue;

  /**
   * @param defaultValue the default value
   */
  public ClassifierSupport(T defaultValue) {
    super();
    this.defaultValue = defaultValue;
  }

  /**
   * Always returns the default value. This is the main extension point for subclasses,
   * so it must be able to classify null.
   *
   * @see Classifier#classify(Object)
   */
  @Override
  public T classify(C throwable) {
    return this.defaultValue;
  }

}
