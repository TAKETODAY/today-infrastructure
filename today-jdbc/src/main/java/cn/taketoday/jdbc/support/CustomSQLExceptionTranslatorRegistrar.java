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

import cn.taketoday.beans.factory.InitializingBean;

/**
 * Registry for custom {@link SQLExceptionTranslator} instances for specific databases.
 *
 * @author Thomas Risberg
 * @since 4.0
 */
public class CustomSQLExceptionTranslatorRegistrar implements InitializingBean {

  /**
   * Map registry to hold custom translators specific databases.
   * Key is the database product name as defined in the
   * {@link cn.taketoday.jdbc.support.SQLErrorCodesFactory}.
   */
  private final Map<String, SQLExceptionTranslator> translators = new HashMap<>();

  /**
   * Setter for a Map of {@link SQLExceptionTranslator} references where the key must
   * be the database name as defined in the {@code sql-error-codes.xml} file.
   * <p>Note that any existing translators will remain unless there is a match in the
   * database name, at which point the new translator will replace the existing one.
   */
  public void setTranslators(Map<String, SQLExceptionTranslator> translators) {
    this.translators.putAll(translators);
  }

  @Override
  public void afterPropertiesSet() {
    for (Map.Entry<String, SQLExceptionTranslator> entry : translators.entrySet()) {
      String dbName = entry.getKey();
      SQLExceptionTranslator translator = entry.getValue();
      CustomSQLExceptionTranslatorRegistry.getInstance().registerTranslator(dbName, translator);
    }
  }

}
