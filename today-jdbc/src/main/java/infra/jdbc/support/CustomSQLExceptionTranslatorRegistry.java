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

import java.util.HashMap;
import java.util.Map;

import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Registry for custom {@link SQLExceptionTranslator} instances associated with
 * specific databases allowing for overriding translation based on values contained in the configuration file
 * named "sql-error-codes.xml".
 *
 * @author Thomas Risberg
 * @see SQLErrorCodesFactory
 * @since 4.0
 */
public final class CustomSQLExceptionTranslatorRegistry {
  private static final Logger logger = LoggerFactory.getLogger(CustomSQLExceptionTranslatorRegistry.class);

  /**
   * Keep track of a single instance so we can return it to classes that request it.
   */
  private static final CustomSQLExceptionTranslatorRegistry instance = new CustomSQLExceptionTranslatorRegistry();

  /**
   * Return the singleton instance.
   */
  public static CustomSQLExceptionTranslatorRegistry getInstance() {
    return instance;
  }

  /**
   * Map registry to hold custom translators specific databases.
   * Key is the database product name as defined in the
   * {@link SQLErrorCodesFactory}.
   */
  private final Map<String, SQLExceptionTranslator> translatorMap = new HashMap<>();

  /**
   * Create a new instance of the {@link CustomSQLExceptionTranslatorRegistry} class.
   * <p>Not public to enforce Singleton design pattern.
   */
  private CustomSQLExceptionTranslatorRegistry() { }

  /**
   * Register a new custom translator for the specified database name.
   *
   * @param dbName the database name
   * @param translator the custom translator
   */
  public void registerTranslator(String dbName, SQLExceptionTranslator translator) {
    SQLExceptionTranslator replaced = this.translatorMap.put(dbName, translator);
    if (logger.isDebugEnabled()) {
      if (replaced != null) {
        logger.debug("Replacing custom translator [{}] for database '{}' with [{}]", replaced, dbName, translator);
      }
      else {
        logger.debug("Adding custom translator of type [{}] for database '{}'",
                translator.getClass().getName(), translator.getClass().getName(), dbName);
      }
    }
  }

  /**
   * Find a custom translator for the specified database.
   *
   * @param dbName the database name
   * @return the custom translator, or {@code null} if none found
   */
  @Nullable
  public SQLExceptionTranslator findTranslatorForDatabase(String dbName) {
    return this.translatorMap.get(dbName);
  }

}
