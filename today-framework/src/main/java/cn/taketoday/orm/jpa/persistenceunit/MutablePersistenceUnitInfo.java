/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.orm.jpa.persistenceunit;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

/**
 * Framework's base implementation of the JPA
 * {@link jakarta.persistence.spi.PersistenceUnitInfo} interface,
 * used to bootstrap an {@code EntityManagerFactory} in a container.
 *
 * <p>This implementation is largely a JavaBean, offering mutators
 * for all standard {@code PersistenceUnitInfo} properties.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 4.0
 */
public class MutablePersistenceUnitInfo implements SmartPersistenceUnitInfo {

  @Nullable
  private String persistenceUnitName;

  @Nullable
  private String persistenceProviderClassName;

  @Nullable
  private PersistenceUnitTransactionType transactionType;

  @Nullable
  private DataSource nonJtaDataSource;

  @Nullable
  private DataSource jtaDataSource;

  private final List<String> mappingFileNames = new ArrayList<>();

  private final List<URL> jarFileUrls = new ArrayList<>();

  @Nullable
  private URL persistenceUnitRootUrl;

  private final List<String> managedClassNames = new ArrayList<>();

  private final List<String> managedPackages = new ArrayList<>();

  private boolean excludeUnlistedClasses = false;

  private SharedCacheMode sharedCacheMode = SharedCacheMode.UNSPECIFIED;

  private ValidationMode validationMode = ValidationMode.AUTO;

  private Properties properties = new Properties();

  private String persistenceXMLSchemaVersion = "2.0";

  @Nullable
  private String persistenceProviderPackageName;

  public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
    this.persistenceUnitName = persistenceUnitName;
  }

  @Override
  @Nullable
  public String getPersistenceUnitName() {
    return this.persistenceUnitName;
  }

  public void setPersistenceProviderClassName(@Nullable String persistenceProviderClassName) {
    this.persistenceProviderClassName = persistenceProviderClassName;
  }

  @Override
  @Nullable
  public String getPersistenceProviderClassName() {
    return this.persistenceProviderClassName;
  }

  public void setTransactionType(PersistenceUnitTransactionType transactionType) {
    this.transactionType = transactionType;
  }

  @Override
  public PersistenceUnitTransactionType getTransactionType() {
    if (this.transactionType != null) {
      return this.transactionType;
    }
    else {
      return jtaDataSource != null ?
             PersistenceUnitTransactionType.JTA : PersistenceUnitTransactionType.RESOURCE_LOCAL;
    }
  }

  public void setJtaDataSource(@Nullable DataSource jtaDataSource) {
    this.jtaDataSource = jtaDataSource;
  }

  @Override
  @Nullable
  public DataSource getJtaDataSource() {
    return this.jtaDataSource;
  }

  public void setNonJtaDataSource(@Nullable DataSource nonJtaDataSource) {
    this.nonJtaDataSource = nonJtaDataSource;
  }

  @Override
  @Nullable
  public DataSource getNonJtaDataSource() {
    return this.nonJtaDataSource;
  }

  public void addMappingFileName(String mappingFileName) {
    this.mappingFileNames.add(mappingFileName);
  }

  @Override
  public List<String> getMappingFileNames() {
    return this.mappingFileNames;
  }

  public void addJarFileUrl(URL jarFileUrl) {
    this.jarFileUrls.add(jarFileUrl);
  }

  @Override
  public List<URL> getJarFileUrls() {
    return this.jarFileUrls;
  }

  public void setPersistenceUnitRootUrl(@Nullable URL persistenceUnitRootUrl) {
    this.persistenceUnitRootUrl = persistenceUnitRootUrl;
  }

  @Override
  @Nullable
  public URL getPersistenceUnitRootUrl() {
    return this.persistenceUnitRootUrl;
  }

  /**
   * Add a managed class name to the persistence provider's metadata.
   *
   * @see jakarta.persistence.spi.PersistenceUnitInfo#getManagedClassNames()
   * @see #addManagedPackage
   */
  public void addManagedClassName(String managedClassName) {
    this.managedClassNames.add(managedClassName);
  }

  @Override
  public List<String> getManagedClassNames() {
    return this.managedClassNames;
  }

  /**
   * Add a managed package to the persistence provider's metadata.
   * <p>Note: This refers to annotated {@code package-info.java} files. It does
   * <i>not</i> trigger entity scanning in the specified package; this is
   * rather the job of {@link DefaultPersistenceUnitManager#setPackagesToScan}.
   *
   * @see SmartPersistenceUnitInfo#getManagedPackages()
   * @see #addManagedClassName
   * @since 4.0
   */
  public void addManagedPackage(String packageName) {
    this.managedPackages.add(packageName);
  }

  @Override
  public List<String> getManagedPackages() {
    return this.managedPackages;
  }

  public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
    this.excludeUnlistedClasses = excludeUnlistedClasses;
  }

  @Override
  public boolean excludeUnlistedClasses() {
    return this.excludeUnlistedClasses;
  }

  public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
    this.sharedCacheMode = sharedCacheMode;
  }

  @Override
  public SharedCacheMode getSharedCacheMode() {
    return this.sharedCacheMode;
  }

  public void setValidationMode(ValidationMode validationMode) {
    this.validationMode = validationMode;
  }

  @Override
  public ValidationMode getValidationMode() {
    return this.validationMode;
  }

  public void addProperty(String name, String value) {
    this.properties.setProperty(name, value);
  }

  public void setProperties(Properties properties) {
    Assert.notNull(properties, "Properties must not be null");
    this.properties = properties;
  }

  @Override
  public Properties getProperties() {
    return this.properties;
  }

  public void setPersistenceXMLSchemaVersion(String persistenceXMLSchemaVersion) {
    this.persistenceXMLSchemaVersion = persistenceXMLSchemaVersion;
  }

  @Override
  public String getPersistenceXMLSchemaVersion() {
    return this.persistenceXMLSchemaVersion;
  }

  @Override
  public void setPersistenceProviderPackageName(@Nullable String persistenceProviderPackageName) {
    this.persistenceProviderPackageName = persistenceProviderPackageName;
  }

  @Nullable
  public String getPersistenceProviderPackageName() {
    return this.persistenceProviderPackageName;
  }

  /**
   * This implementation returns the default ClassLoader.
   *
   * @see cn.taketoday.util.ClassUtils#getDefaultClassLoader()
   */
  @Override
  @Nullable
  public ClassLoader getClassLoader() {
    return ClassUtils.getDefaultClassLoader();
  }

  /**
   * This implementation throws an UnsupportedOperationException.
   */
  @Override
  public void addTransformer(ClassTransformer classTransformer) {
    throw new UnsupportedOperationException("addTransformer not supported");
  }

  /**
   * This implementation throws an UnsupportedOperationException.
   */
  @Override
  public ClassLoader getNewTempClassLoader() {
    throw new UnsupportedOperationException("getNewTempClassLoader not supported");
  }

  @Override
  public String toString() {
    return "PersistenceUnitInfo: name '" + this.persistenceUnitName +
            "', root URL [" + this.persistenceUnitRootUrl + "]";
  }

}
