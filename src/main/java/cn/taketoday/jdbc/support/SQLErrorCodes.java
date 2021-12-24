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

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * JavaBean for holding JDBC error codes for a particular database.
 * Instances of this class are normally loaded through a bean factory.
 *
 * <p>Used by Framework's {@link SQLErrorCodeSQLExceptionTranslator}.
 * The file "sql-error-codes.xml" in this package contains default
 * {@code SQLErrorCodes} instances for various databases.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see SQLErrorCodesFactory
 * @see SQLErrorCodeSQLExceptionTranslator
 */
public class SQLErrorCodes {

  @Nullable
  private String[] databaseProductNames;

  private boolean useSqlStateForTranslation = false;

  private String[] badSqlGrammarCodes = new String[0];

  private String[] invalidResultSetAccessCodes = new String[0];

  private String[] duplicateKeyCodes = new String[0];

  private String[] dataIntegrityViolationCodes = new String[0];

  private String[] permissionDeniedCodes = new String[0];

  private String[] dataAccessResourceFailureCodes = new String[0];

  private String[] transientDataAccessResourceCodes = new String[0];

  private String[] cannotAcquireLockCodes = new String[0];

  private String[] deadlockLoserCodes = new String[0];

  private String[] cannotSerializeTransactionCodes = new String[0];

  @Nullable
  private CustomSQLErrorCodesTranslation[] customTranslations;

  @Nullable
  private SQLExceptionTranslator customSqlExceptionTranslator;

  /**
   * Set this property if the database name contains spaces,
   * in which case we can not use the bean name for lookup.
   */
  public void setDatabaseProductName(@Nullable String databaseProductName) {
    this.databaseProductNames = new String[] { databaseProductName };
  }

  @Nullable
  public String getDatabaseProductName() {
    return (this.databaseProductNames != null && this.databaseProductNames.length > 0 ?
            this.databaseProductNames[0] : null);
  }

  /**
   * Set this property to specify multiple database names that contains spaces,
   * in which case we can not use bean names for lookup.
   */
  public void setDatabaseProductNames(@Nullable String... databaseProductNames) {
    this.databaseProductNames = databaseProductNames;
  }

  @Nullable
  public String[] getDatabaseProductNames() {
    return this.databaseProductNames;
  }

  /**
   * Set this property to true for databases that do not provide an error code
   * but that do provide SQL State (this includes PostgreSQL).
   */
  public void setUseSqlStateForTranslation(boolean useStateCodeForTranslation) {
    this.useSqlStateForTranslation = useStateCodeForTranslation;
  }

  public boolean isUseSqlStateForTranslation() {
    return this.useSqlStateForTranslation;
  }

  public void setBadSqlGrammarCodes(String... badSqlGrammarCodes) {
    this.badSqlGrammarCodes = StringUtils.sortArray(badSqlGrammarCodes);
  }

  public String[] getBadSqlGrammarCodes() {
    return this.badSqlGrammarCodes;
  }

  public void setInvalidResultSetAccessCodes(String... invalidResultSetAccessCodes) {
    this.invalidResultSetAccessCodes = StringUtils.sortArray(invalidResultSetAccessCodes);
  }

  public String[] getInvalidResultSetAccessCodes() {
    return this.invalidResultSetAccessCodes;
  }

  public String[] getDuplicateKeyCodes() {
    return this.duplicateKeyCodes;
  }

  public void setDuplicateKeyCodes(String... duplicateKeyCodes) {
    this.duplicateKeyCodes = duplicateKeyCodes;
  }

  public void setDataIntegrityViolationCodes(String... dataIntegrityViolationCodes) {
    this.dataIntegrityViolationCodes = StringUtils.sortArray(dataIntegrityViolationCodes);
  }

  public String[] getDataIntegrityViolationCodes() {
    return this.dataIntegrityViolationCodes;
  }

  public void setPermissionDeniedCodes(String... permissionDeniedCodes) {
    this.permissionDeniedCodes = StringUtils.sortArray(permissionDeniedCodes);
  }

  public String[] getPermissionDeniedCodes() {
    return this.permissionDeniedCodes;
  }

  public void setDataAccessResourceFailureCodes(String... dataAccessResourceFailureCodes) {
    this.dataAccessResourceFailureCodes = StringUtils.sortArray(dataAccessResourceFailureCodes);
  }

  public String[] getDataAccessResourceFailureCodes() {
    return this.dataAccessResourceFailureCodes;
  }

  public void setTransientDataAccessResourceCodes(String... transientDataAccessResourceCodes) {
    this.transientDataAccessResourceCodes = StringUtils.sortArray(transientDataAccessResourceCodes);
  }

  public String[] getTransientDataAccessResourceCodes() {
    return this.transientDataAccessResourceCodes;
  }

  public void setCannotAcquireLockCodes(String... cannotAcquireLockCodes) {
    this.cannotAcquireLockCodes = StringUtils.sortArray(cannotAcquireLockCodes);
  }

  public String[] getCannotAcquireLockCodes() {
    return this.cannotAcquireLockCodes;
  }

  public void setDeadlockLoserCodes(String... deadlockLoserCodes) {
    this.deadlockLoserCodes = StringUtils.sortArray(deadlockLoserCodes);
  }

  public String[] getDeadlockLoserCodes() {
    return this.deadlockLoserCodes;
  }

  public void setCannotSerializeTransactionCodes(String... cannotSerializeTransactionCodes) {
    this.cannotSerializeTransactionCodes = StringUtils.sortArray(cannotSerializeTransactionCodes);
  }

  public String[] getCannotSerializeTransactionCodes() {
    return this.cannotSerializeTransactionCodes;
  }

  public void setCustomTranslations(CustomSQLErrorCodesTranslation... customTranslations) {
    this.customTranslations = customTranslations;
  }

  @Nullable
  public CustomSQLErrorCodesTranslation[] getCustomTranslations() {
    return this.customTranslations;
  }

  public void setCustomSqlExceptionTranslatorClass(@Nullable Class<? extends SQLExceptionTranslator> customTranslatorClass) {
    if (customTranslatorClass != null) {
      try {
        this.customSqlExceptionTranslator =
                ReflectionUtils.accessibleConstructor(customTranslatorClass).newInstance();
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Unable to instantiate custom translator", ex);
      }
    }
    else {
      this.customSqlExceptionTranslator = null;
    }
  }

  public void setCustomSqlExceptionTranslator(@Nullable SQLExceptionTranslator customSqlExceptionTranslator) {
    this.customSqlExceptionTranslator = customSqlExceptionTranslator;
  }

  @Nullable
  public SQLExceptionTranslator getCustomSqlExceptionTranslator() {
    return this.customSqlExceptionTranslator;
  }

}
