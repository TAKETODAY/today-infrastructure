/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.support;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Registry for custom {@link cn.taketoday.jdbc.support.SQLExceptionTranslator} instances associated with
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
   * {@link cn.taketoday.jdbc.support.SQLErrorCodesFactory}.
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
