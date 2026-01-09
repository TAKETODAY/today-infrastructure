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

package infra.jdbc.support;

import org.jspecify.annotations.Nullable;

import infra.dao.DataAccessException;
import infra.util.StringUtils;

/**
 * JavaBean for holding custom JDBC error codes translation for a particular
 * database. The "exceptionClass" property defines which exception will be
 * thrown for the list of error codes specified in the errorCodes property.
 *
 * @author Thomas Risberg
 * @see SQLErrorCodeSQLExceptionTranslator
 * @since 4.0
 */
public class CustomSQLErrorCodesTranslation {

  private String[] errorCodes = new String[0];

  @Nullable
  private Class<?> exceptionClass;

  /**
   * Set the SQL error codes to match.
   */
  public void setErrorCodes(String... errorCodes) {
    this.errorCodes = StringUtils.sortArray(errorCodes);
  }

  /**
   * Return the SQL error codes to match.
   */
  public String[] getErrorCodes() {
    return this.errorCodes;
  }

  /**
   * Set the exception class for the specified error codes.
   */
  public void setExceptionClass(@Nullable Class<?> exceptionClass) {
    if (exceptionClass != null && !DataAccessException.class.isAssignableFrom(exceptionClass)) {
      throw new IllegalArgumentException("Invalid exception class [" + exceptionClass +
              "]: needs to be a subclass of [infra.dao.DataAccessException]");
    }
    this.exceptionClass = exceptionClass;
  }

  /**
   * Return the exception class for the specified error codes.
   */
  @Nullable
  public Class<?> getExceptionClass() {
    return this.exceptionClass;
  }

}
