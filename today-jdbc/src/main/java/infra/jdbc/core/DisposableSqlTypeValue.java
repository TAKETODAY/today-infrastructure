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

package infra.jdbc.core;

import infra.jdbc.core.support.SqlLobValue;
import infra.jdbc.support.SqlValue;

/**
 * Subinterface of {@link SqlTypeValue} that adds a cleanup callback,
 * to be invoked after the value has been set and the corresponding
 * statement has been executed.
 *
 * @author Juergen Hoeller
 * @see SqlLobValue
 * @since 4.0
 */
public interface DisposableSqlTypeValue extends SqlTypeValue {

  /**
   * Clean up resources held by this type value,
   * for example the LobCreator in case of an SqlLobValue.
   *
   * @see SqlLobValue#cleanup()
   * @see SqlValue#cleanup()
   */
  void cleanup();

}
