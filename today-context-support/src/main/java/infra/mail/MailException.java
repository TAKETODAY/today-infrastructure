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

package infra.mail;

import org.jspecify.annotations.Nullable;

import infra.core.NestedRuntimeException;

/**
 * Base class for all mail exceptions.
 *
 * @author Dmitriy Kopylenko
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class MailException extends NestedRuntimeException {

  /**
   * Constructor for MailException.
   *
   * @param msg the detail message
   * @param cause the root cause from the mail API in use
   */
  public MailException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
