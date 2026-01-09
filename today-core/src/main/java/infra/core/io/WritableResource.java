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

package infra.core.io;

/**
 * Extended interface for a resource that supports writing to it.
 *
 * @author TODAY
 * @since 2.1.6 2019-05-14 20:56
 */
public interface WritableResource extends Resource, OutputStreamSource {

  /**
   * Indicate whether the contents of this resource can be written
   * via {@link #getOutputStream()}.
   * <p>Will be {@code true} for typical resource descriptors;
   * note that actual content writing may still fail when attempted.
   * However, a value of {@code false} is a definitive indication
   * that the resource content cannot be modified.
   *
   * @see #getOutputStream()
   * @see #isReadable()
   */
  default boolean isWritable() {
    return true;
  }

}
