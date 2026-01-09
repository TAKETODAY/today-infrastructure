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

package infra.web.multipart.parsing;

import java.io.InputStream;
import java.io.Serial;

import infra.web.multipart.MultipartException;

/**
 * This exception is thrown, if an attempt is made to read data from the {@link InputStream}, which has been returned by
 * {@link FieldItemInput#getInputStream()}, after {@link java.util.Iterator#hasNext()} has been invoked on the iterator, which created the
 * {@link FieldItemInput}.
 */
public class ItemSkippedException extends MultipartException {

  @Serial
  private static final long serialVersionUID = 1;

  /**
   * Constructs an instance with a given detail message.
   *
   * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method)
   */
  public ItemSkippedException(final String message) {
    super(message);
  }

}
