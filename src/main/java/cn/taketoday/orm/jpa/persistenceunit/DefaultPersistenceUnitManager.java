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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.annotation.ClassPathBeanDefinitionScanner;
import cn.taketoday.context.aware.ResourceLoaderAware;
import cn.taketoday.context.classloading.InstrumentationLoadTimeWeaver;
import cn.taketoday.context.classloading.LoadTimeWeaver;
import cn.taketoday.context.loader.CandidateComponentsIndex;
import cn.taketoday.context.loader.CandidateComponentsIndexLoader;
import cn.taketoday.context.weaving.LoadTimeWeaverAware;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.classreading.CachingMetadataReaderFactory;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.core.type.filter.TypeFilter;
import cn.taketoday.jdbc.datasource.lookup.DataSourceLookup;
import cn.taketoday.jdbc.datasource.lookup.JndiDataSourceLookup;
import cn.taketoday.jdbc.datasource.lookup.MapDataSourceLookup;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ResourceUtils;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Default implementation of the {@link PersistenceUnitManager} interface.
 * Used as internal default by
 * {@link cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean}.
 *
 * <p>Supports standard JPA scanning for {@code persistence.xml} files,
 * with configurable file locations, JDBC DataSource lookup and load-time weaving.
 *
 * <p>The default XML file location is {@code classpath*:META-INF/persistence.xml},
 * scanning for all matching files in the classpath (as defined in the JPA specification).
 * DataSource names are by default interpreted as JNDI names, and no load time weaving
 * is available (which requires weaving to be turned off in the persistence provider).
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see #setPersistenceXmlLocations
 * @see #setDataSourceLookup
 * @see #setLoadTimeWeaver
 * @see cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean#setPersistenceUnitManager
 * @since 4.0
 */
public class DefaultPersistenceUnitManager
        implements PersistenceUnitManager, ResourceLoaderAware, LoadTimeWeaverAware, InitializingBean {

  private static final String CLASS_RESOURCE_PATTERN = "/**/*.class";

  private static final String PACKAGE_INFO_SUFFIX = ".package-info";

  private static final String DEFAULT_ORM_XML_RESOURCE = "META-INF/orm.xml";

  private static final String PERSISTENCE_XML_FILENAME = "persistence.xml";

  /**
   * Default location of the {@code persistence.xml} file:
   * "classpath*:META-INF/persistence.xml".
   */
  public static final String DEFAULT_PERSISTENCE_XML_LOCATION = "classpath*:META-INF/" + PERSISTENCE_XML_FILENAME;

  /**
   * Default location for the persistence unit root URL:
   * "classpath:", indicating the root of the classpath.
   */
  public static final String ORIGINAL_DEFAULT_PERSISTENCE_UNIT_ROOT_LOCATION = "classpath:";

  /**
   * Default persistence unit name.
   */
  public static final String ORIGINAL_DEFAULT_PERSISTENCE_UNIT_NAME = "default";

  private static final Set<AnnotationTypeFilter> entityTypeFilters;

  static {
    entityTypeFilters = new LinkedHashSet<>(8);
    entityTypeFilters.add(new AnnotationTypeFilter(Entity.class, false));
    entityTypeFilters.add(new AnnotationTypeFilter(Embeddable.class, false));
    entityTypeFilters.add(new AnnotationTypeFilter(MappedSuperclass.class, false));
    entityTypeFilters.add(new AnnotationTypeFilter(Converter.class, false));
  }

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private String[] persistenceXmlLocations = new String[] { DEFAULT_PERSISTENCE_XML_LOCATION };

  @Nullable
  private String defaultPersistenceUnitRootLocation = ORIGINAL_DEFAULT_PERSISTENCE_UNIT_ROOT_LOCATION;

  @Nullable
  private String defaultPersistenceUnitName = ORIGINAL_DEFAULT_PERSISTENCE_UNIT_NAME;

  @Nullable
  private String[] packagesToScan;

  @Nullable
  private String[] mappingResources;

  @Nullable
  private SharedCacheMode sharedCacheMode;

  @Nullable
  private ValidationMode validationMode;

  private DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

  @Nullable
  private DataSource defaultDataSource;

  @Nullable
  private DataSource defaultJtaDataSource;

  @Nullable
  private PersistenceUnitPostProcessor[] persistenceUnitPostProcessors;

  @Nullable
  private LoadTimeWeaver loadTimeWeaver;

  private PatternResourceLoader patternResourceLoader = new PathMatchingPatternResourceLoader();

  @Nullable
  private CandidateComponentsIndex componentsIndex;

  private final Set<String> persistenceUnitInfoNames = new HashSet<>();

  private final Map<String, PersistenceUnitInfo> persistenceUnitInfos = new HashMap<>();

  /**
   * Specify the location of the {@code persistence.xml} files to load.
   * These can be specified as Framework resource locations and/or location patterns.
   * <p>Default is "classpath*:META-INF/persistence.xml".
   */
  public void setPersistenceXmlLocation(String persistenceXmlLocation) {
    this.persistenceXmlLocations = new String[] { persistenceXmlLocation };
  }

  /**
   * Specify multiple locations of {@code persistence.xml} files to load.
   * These can be specified as Framework resource locations and/or location patterns.
   * <p>Default is "classpath*:META-INF/persistence.xml".
   *
   * @param persistenceXmlLocations an array of Framework resource Strings
   * identifying the location of the {@code persistence.xml} files to read
   */
  public void setPersistenceXmlLocations(String... persistenceXmlLocations) {
    this.persistenceXmlLocations = persistenceXmlLocations;
  }

  /**
   * Set the default persistence unit root location, to be applied
   * if no unit-specific persistence unit root could be determined.
   * <p>Default is "classpath:", that is, the root of the current classpath
   * (nearest root directory). To be overridden if unit-specific resolution
   * does not work and the classpath root is not appropriate either.
   */
  public void setDefaultPersistenceUnitRootLocation(String defaultPersistenceUnitRootLocation) {
    this.defaultPersistenceUnitRootLocation = defaultPersistenceUnitRootLocation;
  }

  /**
   * Specify the name of the default persistence unit, if any. Default is "default".
   * <p>Primarily applied to a scanned persistence unit without {@code persistence.xml}.
   * Also applicable to selecting a default unit from several persistence units available.
   *
   * @see #setPackagesToScan
   * @see #obtainDefaultPersistenceUnitInfo
   */
  public void setDefaultPersistenceUnitName(String defaultPersistenceUnitName) {
    this.defaultPersistenceUnitName = defaultPersistenceUnitName;
  }

  /**
   * Set whether to use Framework-based scanning for entity classes in the classpath
   * instead of using JPA's standard scanning of jar files with {@code persistence.xml}
   * markers in them. In case of Framework-based scanning, no {@code persistence.xml}
   * is necessary; all you need to do is to specify base packages to search here.
   * <p>Default is none. Specify packages to search for autodetection of your entity
   * classes in the classpath. This is analogous to Framework's component-scan feature
   * ({@link ClassPathBeanDefinitionScanner}).
   * <p>Such package scanning defines a "default persistence unit" in Framework, which
   * may live next to regularly defined units originating from {@code persistence.xml}.
   * Its name is determined by {@link #setDefaultPersistenceUnitName}: by default,
   * it's simply "default".
   * <p><b>Note: There may be limitations in comparison to regular JPA scanning.</b>
   * In particular, JPA providers may pick up annotated packages for provider-specific
   * annotations only when driven by {@code persistence.xml}. As of 4.1, Framework's
   * scan can detect annotated packages as well if supported by the given
   * {@link cn.taketoday.orm.jpa.JpaVendorAdapter} (e.g. for Hibernate).
   * <p>If no explicit {@link #setMappingResources mapping resources} have been
   * specified in addition to these packages, this manager looks for a default
   * {@code META-INF/orm.xml} file in the classpath, registering it as a mapping
   * resource for the default unit if the mapping file is not co-located with a
   * {@code persistence.xml} file (in which case we assume it is only meant to be
   * used with the persistence units defined there, like in standard JPA).
   *
   * @see #setDefaultPersistenceUnitName
   * @see #setMappingResources
   */
  public void setPackagesToScan(String... packagesToScan) {
    this.packagesToScan = packagesToScan;
  }

  /**
   * Specify one or more mapping resources (equivalent to {@code <mapping-file>}
   * entries in {@code persistence.xml}) for the default persistence unit.
   * Can be used on its own or in combination with entity scanning in the classpath,
   * in both cases avoiding {@code persistence.xml}.
   * <p>Note that mapping resources must be relative to the classpath root,
   * e.g. "META-INF/mappings.xml" or "com/mycompany/repository/mappings.xml",
   * so that they can be loaded through {@code ClassLoader.getResource}.
   * <p>If no explicit mapping resources have been specified next to
   * {@link #setPackagesToScan packages to scan}, this manager looks for a default
   * {@code META-INF/orm.xml} file in the classpath, registering it as a mapping
   * resource for the default unit if the mapping file is not co-located with a
   * {@code persistence.xml} file (in which case we assume it is only meant to be
   * used with the persistence units defined there, like in standard JPA).
   * <p>Note that specifying an empty array/list here suppresses the default
   * {@code META-INF/orm.xml} check. On the other hand, explicitly specifying
   * {@code META-INF/orm.xml} here will register that file even if it happens
   * to be co-located with a {@code persistence.xml} file.
   *
   * @see #setDefaultPersistenceUnitName
   * @see #setPackagesToScan
   */
  public void setMappingResources(String... mappingResources) {
    this.mappingResources = mappingResources;
  }

  /**
   * Specify the JPA 2.0 shared cache mode for all of this manager's persistence
   * units, overriding any value in {@code persistence.xml} if set.
   *
   * @see PersistenceUnitInfo#getSharedCacheMode()
   * @since 4.0
   */
  public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
    this.sharedCacheMode = sharedCacheMode;
  }

  /**
   * Specify the JPA 2.0 validation mode for all of this manager's persistence
   * units, overriding any value in {@code persistence.xml} if set.
   *
   * @see PersistenceUnitInfo#getValidationMode()
   * @since 4.0
   */
  public void setValidationMode(ValidationMode validationMode) {
    this.validationMode = validationMode;
  }

  /**
   * Specify the JDBC DataSources that the JPA persistence provider is supposed
   * to use for accessing the database, resolving data source names in
   * {@code persistence.xml} against Framework-managed DataSources.
   * <p>The specified Map needs to define data source names for specific DataSource
   * objects, matching the data source names used in {@code persistence.xml}.
   * If not specified, data source names will be resolved as JNDI names instead
   * (as defined by standard JPA).
   *
   * @see cn.taketoday.jdbc.datasource.lookup.MapDataSourceLookup
   */
  public void setDataSources(Map<String, DataSource> dataSources) {
    this.dataSourceLookup = new MapDataSourceLookup(dataSources);
  }

  /**
   * Specify the JDBC DataSourceLookup that provides DataSources for the
   * persistence provider, resolving data source names in {@code persistence.xml}
   * against Framework-managed DataSource instances.
   * <p>Default is JndiDataSourceLookup, which resolves DataSource names as
   * JNDI names (as defined by standard JPA). Specify a BeanFactoryDataSourceLookup
   * instance if you want DataSource names to be resolved against Framework bean names.
   * <p>Alternatively, consider passing in a map from names to DataSource instances
   * via the "dataSources" property. If the {@code persistence.xml} file
   * does not define DataSource names at all, specify a default DataSource
   * via the "defaultDataSource" property.
   *
   * @see cn.taketoday.jdbc.datasource.lookup.JndiDataSourceLookup
   * @see cn.taketoday.jdbc.datasource.lookup.BeanFactoryDataSourceLookup
   * @see #setDataSources
   * @see #setDefaultDataSource
   */
  public void setDataSourceLookup(@Nullable DataSourceLookup dataSourceLookup) {
    this.dataSourceLookup = (dataSourceLookup != null ? dataSourceLookup : new JndiDataSourceLookup());
  }

  /**
   * Return the JDBC DataSourceLookup that provides DataSources for the
   * persistence provider, resolving data source names in {@code persistence.xml}
   * against Framework-managed DataSource instances.
   */
  @Nullable
  public DataSourceLookup getDataSourceLookup() {
    return this.dataSourceLookup;
  }

  /**
   * Specify the JDBC DataSource that the JPA persistence provider is supposed to use
   * for accessing the database if none has been specified in {@code persistence.xml}.
   * This variant indicates no special transaction setup, i.e. typical resource-local.
   * <p>In JPA speak, a DataSource passed in here will be uses as "nonJtaDataSource"
   * on the PersistenceUnitInfo passed to the PersistenceProvider, provided that
   * none has been registered before.
   *
   * @see PersistenceUnitInfo#getNonJtaDataSource()
   */
  public void setDefaultDataSource(@Nullable DataSource defaultDataSource) {
    this.defaultDataSource = defaultDataSource;
  }

  /**
   * Return the JDBC DataSource that the JPA persistence provider is supposed to use
   * for accessing the database if none has been specified in {@code persistence.xml}.
   */
  @Nullable
  public DataSource getDefaultDataSource() {
    return this.defaultDataSource;
  }

  /**
   * Specify the JDBC DataSource that the JPA persistence provider is supposed to use
   * for accessing the database if none has been specified in {@code persistence.xml}.
   * This variant indicates that JTA is supposed to be used as transaction type.
   * <p>In JPA speak, a DataSource passed in here will be uses as "jtaDataSource"
   * on the PersistenceUnitInfo passed to the PersistenceProvider, provided that
   * none has been registered before.
   *
   * @see PersistenceUnitInfo#getJtaDataSource()
   */
  public void setDefaultJtaDataSource(@Nullable DataSource defaultJtaDataSource) {
    this.defaultJtaDataSource = defaultJtaDataSource;
  }

  /**
   * Return the JTA-aware DataSource that the JPA persistence provider is supposed to use
   * for accessing the database if none has been specified in {@code persistence.xml}.
   */
  @Nullable
  public DataSource getDefaultJtaDataSource() {
    return this.defaultJtaDataSource;
  }

  /**
   * Set the PersistenceUnitPostProcessors to be applied to each
   * PersistenceUnitInfo that has been parsed by this manager.
   * <p>Such post-processors can, for example, register further entity classes and
   * jar files, in addition to the metadata read from {@code persistence.xml}.
   */
  public void setPersistenceUnitPostProcessors(@Nullable PersistenceUnitPostProcessor... postProcessors) {
    this.persistenceUnitPostProcessors = postProcessors;
  }

  /**
   * Return the PersistenceUnitPostProcessors to be applied to each
   * PersistenceUnitInfo that has been parsed by this manager.
   */
  @Nullable
  public PersistenceUnitPostProcessor[] getPersistenceUnitPostProcessors() {
    return this.persistenceUnitPostProcessors;
  }

  /**
   * Specify the Framework LoadTimeWeaver to use for class instrumentation according
   * to the JPA class transformer contract.
   * <p>It is not required to specify a LoadTimeWeaver: Most providers will be able
   * to provide a subset of their functionality without class instrumentation as well,
   * or operate with their own VM agent specified on JVM startup. Furthermore,
   * DefaultPersistenceUnitManager falls back to an InstrumentationLoadTimeWeaver
   * if Framework's agent-based instrumentation is available at runtime.
   * <p>In terms of Framework-provided weaving options, the most important ones are
   * InstrumentationLoadTimeWeaver, which requires a Framework-specific (but very general)
   * VM agent specified on JVM startup, and ReflectiveLoadTimeWeaver, which interacts
   * with an underlying ClassLoader based on specific extended methods being available
   * on it (for example, interacting with Framework's TomcatInstrumentableClassLoader).
   * Consider using the {@code context:load-time-weaver} XML tag for creating
   * such a shared LoadTimeWeaver (autodetecting the environment by default).
   *
   * @see cn.taketoday.context.classloading.InstrumentationLoadTimeWeaver
   * @see cn.taketoday.context.classloading.ReflectiveLoadTimeWeaver
   */
  @Override
  public void setLoadTimeWeaver(@Nullable LoadTimeWeaver loadTimeWeaver) {
    this.loadTimeWeaver = loadTimeWeaver;
  }

  /**
   * Return the Framework LoadTimeWeaver to use for class instrumentation according
   * to the JPA class transformer contract.
   */
  @Nullable
  public LoadTimeWeaver getLoadTimeWeaver() {
    return this.loadTimeWeaver;
  }

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.patternResourceLoader = PatternResourceLoader.fromResourceLoader(resourceLoader);
    this.componentsIndex = CandidateComponentsIndexLoader.loadIndex(resourceLoader.getClassLoader());
  }

  @Override
  public void afterPropertiesSet() {
    if (this.loadTimeWeaver == null && InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
      this.loadTimeWeaver = new InstrumentationLoadTimeWeaver(this.patternResourceLoader.getClassLoader());
    }
    preparePersistenceUnitInfos();
  }

  /**
   * Prepare the PersistenceUnitInfos according to the configuration
   * of this manager: scanning for {@code persistence.xml} files,
   * parsing all matching files, configuring and post-processing them.
   * <p>PersistenceUnitInfos cannot be obtained before this preparation
   * method has been invoked.
   *
   * @see #obtainDefaultPersistenceUnitInfo()
   * @see #obtainPersistenceUnitInfo(String)
   */
  public void preparePersistenceUnitInfos() {
    this.persistenceUnitInfoNames.clear();
    this.persistenceUnitInfos.clear();

    List<FrameworkPersistenceUnitInfo> puis = readPersistenceUnitInfos();
    for (FrameworkPersistenceUnitInfo pui : puis) {
      if (pui.getPersistenceUnitRootUrl() == null) {
        pui.setPersistenceUnitRootUrl(determineDefaultPersistenceUnitRootUrl());
      }
      if (pui.getJtaDataSource() == null && this.defaultJtaDataSource != null) {
        pui.setJtaDataSource(this.defaultJtaDataSource);
      }
      if (pui.getNonJtaDataSource() == null && this.defaultDataSource != null) {
        pui.setNonJtaDataSource(this.defaultDataSource);
      }
      if (this.sharedCacheMode != null) {
        pui.setSharedCacheMode(this.sharedCacheMode);
      }
      if (this.validationMode != null) {
        pui.setValidationMode(this.validationMode);
      }
      if (this.loadTimeWeaver != null) {
        pui.init(this.loadTimeWeaver);
      }
      else {
        pui.init(this.patternResourceLoader.getClassLoader());
      }
      postProcessPersistenceUnitInfo(pui);
      String name = pui.getPersistenceUnitName();
      if (!this.persistenceUnitInfoNames.add(name) && !isPersistenceUnitOverrideAllowed()) {
        StringBuilder msg = new StringBuilder();
        msg.append("Conflicting persistence unit definitions for name '").append(name).append("': ");
        msg.append(pui.getPersistenceUnitRootUrl()).append(", ");
        msg.append(this.persistenceUnitInfos.get(name).getPersistenceUnitRootUrl());
        throw new IllegalStateException(msg.toString());
      }
      this.persistenceUnitInfos.put(name, pui);
    }
  }

  /**
   * Read all persistence unit infos from {@code persistence.xml},
   * as defined in the JPA specification.
   */
  private List<FrameworkPersistenceUnitInfo> readPersistenceUnitInfos() {
    List<FrameworkPersistenceUnitInfo> infos = new ArrayList<>(1);
    String defaultName = this.defaultPersistenceUnitName;
    boolean buildDefaultUnit = (this.packagesToScan != null || this.mappingResources != null);
    boolean foundDefaultUnit = false;

    PersistenceUnitReader reader = new PersistenceUnitReader(this.patternResourceLoader, this.dataSourceLookup);
    FrameworkPersistenceUnitInfo[] readInfos = reader.readPersistenceUnitInfos(this.persistenceXmlLocations);
    for (FrameworkPersistenceUnitInfo readInfo : readInfos) {
      infos.add(readInfo);
      if (defaultName != null && defaultName.equals(readInfo.getPersistenceUnitName())) {
        foundDefaultUnit = true;
      }
    }

    if (buildDefaultUnit) {
      if (foundDefaultUnit) {
        if (logger.isWarnEnabled()) {
          logger.warn("Found explicit default persistence unit with name '" + defaultName + "' in persistence.xml - " +
                  "overriding local default persistence unit settings ('packagesToScan'/'mappingResources')");
        }
      }
      else {
        infos.add(buildDefaultPersistenceUnitInfo());
      }
    }
    return infos;
  }

  /**
   * Perform Framework-based scanning for entity classes.
   *
   * @see #setPackagesToScan
   */
  private FrameworkPersistenceUnitInfo buildDefaultPersistenceUnitInfo() {
    FrameworkPersistenceUnitInfo scannedUnit = new FrameworkPersistenceUnitInfo();
    if (this.defaultPersistenceUnitName != null) {
      scannedUnit.setPersistenceUnitName(this.defaultPersistenceUnitName);
    }
    scannedUnit.setExcludeUnlistedClasses(true);

    if (this.packagesToScan != null) {
      for (String pkg : this.packagesToScan) {
        scanPackage(scannedUnit, pkg);
      }
    }

    if (this.mappingResources != null) {
      for (String mappingFileName : this.mappingResources) {
        scannedUnit.addMappingFileName(mappingFileName);
      }
    }
    else {
      Resource ormXml = getOrmXmlForDefaultPersistenceUnit();
      if (ormXml != null) {
        scannedUnit.addMappingFileName(DEFAULT_ORM_XML_RESOURCE);
        if (scannedUnit.getPersistenceUnitRootUrl() == null) {
          try {
            scannedUnit.setPersistenceUnitRootUrl(
                    PersistenceUnitReader.determinePersistenceUnitRootUrl(ormXml));
          }
          catch (IOException ex) {
            logger.debug("Failed to determine persistence unit root URL from orm.xml location", ex);
          }
        }
      }
    }

    return scannedUnit;
  }

  private void scanPackage(FrameworkPersistenceUnitInfo scannedUnit, String pkg) {
    if (this.componentsIndex != null) {
      Set<String> candidates = new HashSet<>();
      for (AnnotationTypeFilter filter : entityTypeFilters) {
        candidates.addAll(this.componentsIndex.getCandidateTypes(pkg, filter.getAnnotationType().getName()));
      }
      candidates.forEach(scannedUnit::addManagedClassName);
      Set<String> managedPackages = this.componentsIndex.getCandidateTypes(pkg, "package-info");
      managedPackages.forEach(scannedUnit::addManagedPackage);
      return;
    }

    try {
      String pattern = PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX +
              ClassUtils.convertClassNameToResourcePath(pkg) + CLASS_RESOURCE_PATTERN;
      MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(this.patternResourceLoader);
      for (Resource resource : patternResourceLoader.getResources(pattern)) {
        try {
          MetadataReader reader = readerFactory.getMetadataReader(resource);
          String className = reader.getClassMetadata().getClassName();
          if (matchesFilter(reader, readerFactory)) {
            scannedUnit.addManagedClassName(className);
            if (scannedUnit.getPersistenceUnitRootUrl() == null) {
              URL url = resource.getLocation();
              if (ResourceUtils.isJarURL(url)) {
                scannedUnit.setPersistenceUnitRootUrl(ResourceUtils.extractJarFileURL(url));
              }
            }
          }
          else if (className.endsWith(PACKAGE_INFO_SUFFIX)) {
            scannedUnit.addManagedPackage(
                    className.substring(0, className.length() - PACKAGE_INFO_SUFFIX.length()));
          }
        }
        catch (FileNotFoundException ex) {
          // Ignore non-readable resource
        }
      }
    }
    catch (IOException ex) {
      throw new PersistenceException("Failed to scan classpath for unlisted entity classes", ex);
    }
  }

  /**
   * Check whether any of the configured entity type filters matches
   * the current class descriptor contained in the metadata reader.
   */
  private boolean matchesFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
    for (TypeFilter filter : entityTypeFilters) {
      if (filter.match(reader, readerFactory)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Try to determine the persistence unit root URL based on the given
   * "defaultPersistenceUnitRootLocation".
   *
   * @return the persistence unit root URL to pass to the JPA PersistenceProvider
   * @see #setDefaultPersistenceUnitRootLocation
   */
  @Nullable
  private URL determineDefaultPersistenceUnitRootUrl() {
    if (this.defaultPersistenceUnitRootLocation == null) {
      return null;
    }
    try {
      URL url = patternResourceLoader.getResource(defaultPersistenceUnitRootLocation).getLocation();
      return (ResourceUtils.isJarURL(url) ? ResourceUtils.extractJarFileURL(url) : url);
    }
    catch (IOException ex) {
      throw new PersistenceException("Unable to resolve persistence unit root URL", ex);
    }
  }

  /**
   * Determine JPA's default "META-INF/orm.xml" resource for use with Framework's default
   * persistence unit, if any.
   * <p>Checks whether a "META-INF/orm.xml" file exists in the classpath and uses it
   * if it is not co-located with a "META-INF/persistence.xml" file.
   */
  @Nullable
  private Resource getOrmXmlForDefaultPersistenceUnit() {
    Resource ormXml = patternResourceLoader.getResource(
            defaultPersistenceUnitRootLocation + DEFAULT_ORM_XML_RESOURCE);
    if (ormXml.exists()) {
      try {
        Resource persistenceXml = ormXml.createRelative(PERSISTENCE_XML_FILENAME);
        if (!persistenceXml.exists()) {
          return ormXml;
        }
      }
      catch (IOException ex) {
        // Cannot resolve relative persistence.xml file - let's assume it's not there.
        return ormXml;
      }
    }
    return null;
  }

  /**
   * Return the specified PersistenceUnitInfo from this manager's cache
   * of processed persistence units, keeping it in the cache (i.e. not
   * 'obtaining' it for use but rather just accessing it for post-processing).
   * <p>This can be used in {@link #postProcessPersistenceUnitInfo} implementations,
   * detecting existing persistence units of the same name and potentially merging them.
   *
   * @param persistenceUnitName the name of the desired persistence unit
   * @return the PersistenceUnitInfo in mutable form, or {@code null} if not available
   */
  @Nullable
  protected final MutablePersistenceUnitInfo getPersistenceUnitInfo(String persistenceUnitName) {
    PersistenceUnitInfo pui = this.persistenceUnitInfos.get(persistenceUnitName);
    return (MutablePersistenceUnitInfo) pui;
  }

  /**
   * Hook method allowing subclasses to customize each PersistenceUnitInfo.
   * <p>The default implementation delegates to all registered PersistenceUnitPostProcessors.
   * It is usually preferable to register further entity classes, jar files etc there
   * rather than in a subclass of this manager, to be able to reuse the post-processors.
   *
   * @param pui the chosen PersistenceUnitInfo, as read from {@code persistence.xml}.
   * Passed in as MutablePersistenceUnitInfo.
   * @see #setPersistenceUnitPostProcessors
   */
  protected void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
    PersistenceUnitPostProcessor[] postProcessors = getPersistenceUnitPostProcessors();
    if (postProcessors != null) {
      for (PersistenceUnitPostProcessor postProcessor : postProcessors) {
        postProcessor.postProcessPersistenceUnitInfo(pui);
      }
    }
  }

  /**
   * Return whether an override of a same-named persistence unit is allowed.
   * <p>Default is {@code false}. May be overridden to return {@code true},
   * for example if {@link #postProcessPersistenceUnitInfo} is able to handle that case.
   */
  protected boolean isPersistenceUnitOverrideAllowed() {
    return false;
  }

  @Override
  public PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() {
    if (this.persistenceUnitInfoNames.isEmpty()) {
      throw new IllegalStateException("No persistence units parsed from " +
              ObjectUtils.nullSafeToString(this.persistenceXmlLocations));
    }
    if (this.persistenceUnitInfos.isEmpty()) {
      throw new IllegalStateException("All persistence units from " +
              ObjectUtils.nullSafeToString(this.persistenceXmlLocations) + " already obtained");
    }
    if (this.persistenceUnitInfos.size() > 1 && this.defaultPersistenceUnitName != null) {
      return obtainPersistenceUnitInfo(this.defaultPersistenceUnitName);
    }
    PersistenceUnitInfo pui = this.persistenceUnitInfos.values().iterator().next();
    this.persistenceUnitInfos.clear();
    return pui;
  }

  @Override
  public PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName) {
    PersistenceUnitInfo pui = this.persistenceUnitInfos.remove(persistenceUnitName);
    if (pui == null) {
      if (!this.persistenceUnitInfoNames.contains(persistenceUnitName)) {
        throw new IllegalArgumentException(
                "No persistence unit with name '" + persistenceUnitName + "' found");
      }
      else {
        throw new IllegalStateException(
                "Persistence unit with name '" + persistenceUnitName + "' already obtained");
      }
    }
    return pui;
  }

}
