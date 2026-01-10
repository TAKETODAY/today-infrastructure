/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.core;

import infra.lang.Assert;

/**
 * A callback interface for a decorator to be applied to any {@code T}
 *
 * @param <T> Type to be decorated
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/12 21:26
 */
public interface Decorator<T> {

  /**
   * Decorate the given {@code delegate}, returning a potentially wrapped
   * {@code delegate} for actual execution, internally delegating to the
   * original methods implementation.
   *
   * @param delegate the original {@code delegate}
   * @return the decorated object
   */
  T decorate(T delegate);

  /**
   * call it after this decoration
   */
  default Decorator<T> andThen(Decorator<T> decorator) {
    Assert.notNull(decorator, "decorator is required");
    return delegate -> {
      delegate = decorate(delegate);
      return decorator.decorate(delegate);
    };
  }

}
