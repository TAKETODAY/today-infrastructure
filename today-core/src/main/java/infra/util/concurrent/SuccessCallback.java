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

package infra.util.concurrent;

import org.jspecify.annotations.Nullable;

/**
 * Success callback for a {@link Future}.
 *
 * @param <T> the result type
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface SuccessCallback<T extends @Nullable Object> {

  /**
   * Called when the {@link Future} completes with success.
   * <p>Note that Exceptions raised by this method are ignored.
   *
   * @param result the result
   */
  void onSuccess(T result) throws Throwable;

}
