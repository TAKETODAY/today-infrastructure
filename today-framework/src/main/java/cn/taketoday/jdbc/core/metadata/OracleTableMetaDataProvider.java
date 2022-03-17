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

package cn.taketoday.jdbc.core.metadata;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * Oracle-specific implementation of the {@link cn.taketoday.jdbc.core.metadata.TableMetaDataProvider}.
 * Supports a feature for including synonyms in the meta-data lookup. Also supports lookup of current schema
 * using the {@code sys_context}.
 *
 * <p>Thanks to Mike Youngstrom and Bruce Campbell for submitting the original suggestion for the Oracle
 * current schema lookup implementation.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 4.0
 */
public class OracleTableMetaDataProvider extends GenericTableMetaDataProvider {

  private final boolean includeSynonyms;

  @Nullable
  private final String defaultSchema;

  /**
   * Constructor used to initialize with provided database meta-data.
   *
   * @param databaseMetaData meta-data to be used
   */
  public OracleTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
    this(databaseMetaData, false);
  }

  /**
   * Constructor used to initialize with provided database meta-data.
   *
   * @param databaseMetaData meta-data to be used
   * @param includeSynonyms whether to include synonyms
   */
  public OracleTableMetaDataProvider(DatabaseMetaData databaseMetaData, boolean includeSynonyms)
          throws SQLException {

    super(databaseMetaData);
    this.includeSynonyms = includeSynonyms;
    this.defaultSchema = lookupDefaultSchema(databaseMetaData);
  }

  /*
   * Oracle-based implementation for detecting the current schema.
   */
  @Nullable
  private static String lookupDefaultSchema(DatabaseMetaData databaseMetaData) {
    try {
      CallableStatement cstmt = null;
      try {
        Connection con = databaseMetaData.getConnection();
        if (con == null) {
          logger.debug("Cannot check default schema - no Connection from DatabaseMetaData");
          return null;
        }
        cstmt = con.prepareCall("{? = call sys_context('USERENV', 'CURRENT_SCHEMA')}");
        cstmt.registerOutParameter(1, Types.VARCHAR);
        cstmt.execute();
        return cstmt.getString(1);
      }
      finally {
        if (cstmt != null) {
          cstmt.close();
        }
      }
    }
    catch (SQLException ex) {
      logger.debug("Exception encountered during default schema lookup", ex);
      return null;
    }
  }

  @Override
  @Nullable
  protected String getDefaultSchema() {
    if (this.defaultSchema != null) {
      return this.defaultSchema;
    }
    return super.getDefaultSchema();
  }

  @Override
  public void initializeWithTableColumnMetaData(DatabaseMetaData databaseMetaData,
                                                @Nullable String catalogName, @Nullable String schemaName, @Nullable String tableName)
          throws SQLException {

    if (!this.includeSynonyms) {
      logger.debug("Defaulting to no synonyms in table meta-data lookup");
      super.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName, tableName);
      return;
    }

    Connection con = databaseMetaData.getConnection();
    if (con == null) {
      logger.info("Unable to include synonyms in table meta-data lookup - no Connection from DatabaseMetaData");
      super.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName, tableName);
      return;
    }

    try {
      Class<?> oracleConClass = con.getClass().getClassLoader().loadClass("oracle.jdbc.OracleConnection");
      con = (Connection) con.unwrap(oracleConClass);
    }
    catch (ClassNotFoundException | SQLException ex) {
      if (logger.isInfoEnabled()) {
        logger.info("Unable to include synonyms in table meta-data lookup - no Oracle Connection: " + ex);
      }
      super.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName, tableName);
      return;
    }

    logger.debug("Including synonyms in table meta-data lookup");
    Method setIncludeSynonyms;
    Boolean originalValueForIncludeSynonyms;

    try {
      Method getIncludeSynonyms = con.getClass().getMethod("getIncludeSynonyms");
      ReflectionUtils.makeAccessible(getIncludeSynonyms);
      originalValueForIncludeSynonyms = (Boolean) getIncludeSynonyms.invoke(con);

      setIncludeSynonyms = con.getClass().getMethod("setIncludeSynonyms", boolean.class);
      ReflectionUtils.makeAccessible(setIncludeSynonyms);
      setIncludeSynonyms.invoke(con, Boolean.TRUE);
    }
    catch (Throwable ex) {
      throw new InvalidDataAccessApiUsageException("Could not prepare Oracle Connection", ex);
    }

    super.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName, tableName);

    try {
      setIncludeSynonyms.invoke(con, originalValueForIncludeSynonyms);
    }
    catch (Throwable ex) {
      throw new InvalidDataAccessApiUsageException("Could not reset Oracle Connection", ex);
    }
  }

}
