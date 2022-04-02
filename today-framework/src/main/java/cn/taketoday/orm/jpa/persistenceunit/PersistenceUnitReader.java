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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.jdbc.datasource.lookup.DataSourceLookup;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.xml.DomUtils;
import cn.taketoday.util.xml.SimpleSaxErrorHandler;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

/**
 * Internal helper class for reading JPA-compliant {@code persistence.xml} files.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 4.0
 */
final class PersistenceUnitReader {

  private static final String PERSISTENCE_VERSION = "version";

  private static final String PERSISTENCE_UNIT = "persistence-unit";

  private static final String UNIT_NAME = "name";

  private static final String MAPPING_FILE_NAME = "mapping-file";

  private static final String JAR_FILE_URL = "jar-file";

  private static final String MANAGED_CLASS_NAME = "class";

  private static final String PROPERTIES = "properties";

  private static final String PROVIDER = "provider";

  private static final String TRANSACTION_TYPE = "transaction-type";

  private static final String JTA_DATA_SOURCE = "jta-data-source";

  private static final String NON_JTA_DATA_SOURCE = "non-jta-data-source";

  private static final String EXCLUDE_UNLISTED_CLASSES = "exclude-unlisted-classes";

  private static final String SHARED_CACHE_MODE = "shared-cache-mode";

  private static final String VALIDATION_MODE = "validation-mode";

  private static final String META_INF = "META-INF";

  private static final Logger logger = LoggerFactory.getLogger(PersistenceUnitReader.class);

  private final PatternResourceLoader patternResourceLoader;

  private final DataSourceLookup dataSourceLookup;

  /**
   * Create a new PersistenceUnitReader.
   *
   * @param patternResourceLoader the PatternResourceLoader to use for loading resources
   * @param dataSourceLookup the DataSourceLookup to resolve DataSource names in
   * {@code persistence.xml} files against
   */
  public PersistenceUnitReader(PatternResourceLoader patternResourceLoader, DataSourceLookup dataSourceLookup) {
    Assert.notNull(dataSourceLookup, "DataSourceLookup is required");
    Assert.notNull(patternResourceLoader, "ResourceLoader is required");
    this.dataSourceLookup = dataSourceLookup;
    this.patternResourceLoader = patternResourceLoader;
  }

  /**
   * Parse and build all persistence unit infos defined in the specified XML file(s).
   *
   * @param persistenceXmlLocation the resource location (can be a pattern)
   * @return the resulting PersistenceUnitInfo instances
   */
  public JpaPersistenceUnitInfo[] readPersistenceUnitInfos(String persistenceXmlLocation) {
    return readPersistenceUnitInfos(new String[] { persistenceXmlLocation });
  }

  /**
   * Parse and build all persistence unit infos defined in the given XML files.
   *
   * @param persistenceXmlLocations the resource locations (can be patterns)
   * @return the resulting PersistenceUnitInfo instances
   */
  public JpaPersistenceUnitInfo[] readPersistenceUnitInfos(String[] persistenceXmlLocations) {
    ErrorHandler handler = new SimpleSaxErrorHandler(logger);
    List<JpaPersistenceUnitInfo> infos = new ArrayList<>(1);
    String resourceLocation = null;
    try {
      for (String location : persistenceXmlLocations) {
        for (Resource resource : patternResourceLoader.getResources(location)) {
          resourceLocation = resource.toString();
          try (InputStream stream = resource.getInputStream()) {
            Document document = buildDocument(handler, stream);
            parseDocument(resource, document, infos);
          }
        }
      }
    }
    catch (IOException ex) {
      throw new IllegalArgumentException("Cannot parse persistence unit from " + resourceLocation, ex);
    }
    catch (SAXException ex) {
      throw new IllegalArgumentException("Invalid XML in persistence unit from " + resourceLocation, ex);
    }
    catch (ParserConfigurationException ex) {
      throw new IllegalArgumentException("Internal error parsing persistence unit from " + resourceLocation);
    }

    return infos.toArray(new JpaPersistenceUnitInfo[0]);
  }

  /**
   * Validate the given stream and return a valid DOM document for parsing.
   */
  protected Document buildDocument(ErrorHandler handler, InputStream stream)
          throws ParserConfigurationException, SAXException, IOException {

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder parser = dbf.newDocumentBuilder();
    parser.setErrorHandler(handler);
    return parser.parse(stream);
  }

  /**
   * Parse the validated document and add entries to the given unit info list.
   */
  protected List<JpaPersistenceUnitInfo> parseDocument(
          Resource resource, Document document, List<JpaPersistenceUnitInfo> infos) throws IOException {

    Element persistence = document.getDocumentElement();
    String version = persistence.getAttribute(PERSISTENCE_VERSION);
    URL rootUrl = determinePersistenceUnitRootUrl(resource);

    List<Element> units = DomUtils.getChildElementsByTagName(persistence, PERSISTENCE_UNIT);
    for (Element unit : units) {
      infos.add(parsePersistenceUnitInfo(unit, version, rootUrl));
    }

    return infos;
  }

  /**
   * Parse the unit info DOM element.
   */
  protected JpaPersistenceUnitInfo parsePersistenceUnitInfo(
          Element persistenceUnit, String version, @Nullable URL rootUrl) throws IOException {

    JpaPersistenceUnitInfo unitInfo = new JpaPersistenceUnitInfo();

    // set JPA version (1.0 or 2.0)
    unitInfo.setPersistenceXMLSchemaVersion(version);

    // set persistence unit root URL
    unitInfo.setPersistenceUnitRootUrl(rootUrl);

    // set unit name
    unitInfo.setPersistenceUnitName(persistenceUnit.getAttribute(UNIT_NAME).trim());

    // set transaction type
    String txType = persistenceUnit.getAttribute(TRANSACTION_TYPE).trim();
    if (StringUtils.hasText(txType)) {
      unitInfo.setTransactionType(PersistenceUnitTransactionType.valueOf(txType));
    }

    // evaluate data sources
    String jtaDataSource = DomUtils.getChildElementValueByTagName(persistenceUnit, JTA_DATA_SOURCE);
    if (StringUtils.hasText(jtaDataSource)) {
      unitInfo.setJtaDataSource(this.dataSourceLookup.getDataSource(jtaDataSource.trim()));
    }

    String nonJtaDataSource = DomUtils.getChildElementValueByTagName(persistenceUnit, NON_JTA_DATA_SOURCE);
    if (StringUtils.hasText(nonJtaDataSource)) {
      unitInfo.setNonJtaDataSource(this.dataSourceLookup.getDataSource(nonJtaDataSource.trim()));
    }

    // provider
    String provider = DomUtils.getChildElementValueByTagName(persistenceUnit, PROVIDER);
    if (StringUtils.hasText(provider)) {
      unitInfo.setPersistenceProviderClassName(provider.trim());
    }

    // exclude unlisted classes
    Element excludeUnlistedClasses = DomUtils.getChildElementByTagName(persistenceUnit, EXCLUDE_UNLISTED_CLASSES);
    if (excludeUnlistedClasses != null) {
      String excludeText = DomUtils.getTextValue(excludeUnlistedClasses);
      unitInfo.setExcludeUnlistedClasses(!StringUtils.hasText(excludeText) || Boolean.parseBoolean(excludeText));
    }

    // set JPA 2.0 shared cache mode
    String cacheMode = DomUtils.getChildElementValueByTagName(persistenceUnit, SHARED_CACHE_MODE);
    if (StringUtils.hasText(cacheMode)) {
      unitInfo.setSharedCacheMode(SharedCacheMode.valueOf(cacheMode));
    }

    // set JPA 2.0 validation mode
    String validationMode = DomUtils.getChildElementValueByTagName(persistenceUnit, VALIDATION_MODE);
    if (StringUtils.hasText(validationMode)) {
      unitInfo.setValidationMode(ValidationMode.valueOf(validationMode));
    }

    parseProperties(persistenceUnit, unitInfo);
    parseManagedClasses(persistenceUnit, unitInfo);
    parseMappingFiles(persistenceUnit, unitInfo);
    parseJarFiles(persistenceUnit, unitInfo);

    return unitInfo;
  }

  /**
   * Parse the {@code property} XML elements.
   */
  protected void parseProperties(Element persistenceUnit, JpaPersistenceUnitInfo unitInfo) {
    Element propRoot = DomUtils.getChildElementByTagName(persistenceUnit, PROPERTIES);
    if (propRoot == null) {
      return;
    }
    List<Element> properties = DomUtils.getChildElementsByTagName(propRoot, "property");
    for (Element property : properties) {
      String name = property.getAttribute("name");
      String value = property.getAttribute("value");
      unitInfo.addProperty(name, value);
    }
  }

  /**
   * Parse the {@code class} XML elements.
   */
  protected void parseManagedClasses(Element persistenceUnit, JpaPersistenceUnitInfo unitInfo) {
    List<Element> classes = DomUtils.getChildElementsByTagName(persistenceUnit, MANAGED_CLASS_NAME);
    for (Element element : classes) {
      String value = DomUtils.getTextValue(element).trim();
      if (StringUtils.hasText(value)) {
        unitInfo.addManagedClassName(value);
      }
    }
  }

  /**
   * Parse the {@code mapping-file} XML elements.
   */
  protected void parseMappingFiles(Element persistenceUnit, JpaPersistenceUnitInfo unitInfo) {
    List<Element> files = DomUtils.getChildElementsByTagName(persistenceUnit, MAPPING_FILE_NAME);
    for (Element element : files) {
      String value = DomUtils.getTextValue(element).trim();
      if (StringUtils.hasText(value)) {
        unitInfo.addMappingFileName(value);
      }
    }
  }

  /**
   * Parse the {@code jar-file} XML elements.
   */
  protected void parseJarFiles(Element persistenceUnit, JpaPersistenceUnitInfo unitInfo) throws IOException {
    List<Element> jars = DomUtils.getChildElementsByTagName(persistenceUnit, JAR_FILE_URL);
    for (Element element : jars) {
      String value = DomUtils.getTextValue(element).trim();
      if (StringUtils.hasText(value)) {
        boolean found = false;
        for (Resource resource : patternResourceLoader.getResources(value)) {
          if (resource.exists()) {
            found = true;
            unitInfo.addJarFileUrl(resource.getLocation());
          }
        }
        if (!found) {
          // relative to the persistence unit root, according to the JPA spec
          URL rootUrl = unitInfo.getPersistenceUnitRootUrl();
          if (rootUrl != null) {
            unitInfo.addJarFileUrl(new URL(rootUrl, value));
          }
          else {
            logger.warn("Cannot resolve jar-file entry [{}] in persistence unit '{}' without root URL",
                    value, unitInfo.getPersistenceUnitName());
          }
        }
      }
    }
  }

  /**
   * Determine the persistence unit root URL based on the given resource
   * (which points to the {@code persistence.xml} file we're reading).
   *
   * @param resource the resource to check
   * @return the corresponding persistence unit root URL
   * @throws IOException if the checking failed
   */
  @Nullable
  static URL determinePersistenceUnitRootUrl(Resource resource) throws IOException {
    URL originalURL = resource.getLocation();

    // If we get an archive, simply return the jar URL (section 6.2 from the JPA spec)
    if (ResourceUtils.isJarURL(originalURL)) {
      return ResourceUtils.extractJarFileURL(originalURL);
    }

    // Check META-INF folder
    String urlToString = originalURL.toExternalForm();
    if (!urlToString.contains(META_INF)) {
      if (logger.isInfoEnabled()) {
        logger.info("{} should be located inside META-INF directory; cannot determine persistence unit root URL for {}",
                resource.getName(), resource);
      }
      return null;
    }
    if (urlToString.lastIndexOf(META_INF) == urlToString.lastIndexOf('/') - (1 + META_INF.length())) {
      if (logger.isInfoEnabled()) {
        logger.info("{} is not located in the root of META-INF directory; cannot determine persistence unit root URL for {}",
                resource.getName(), resource);
      }
      return null;
    }

    String persistenceUnitRoot = urlToString.substring(0, urlToString.lastIndexOf(META_INF));
    if (persistenceUnitRoot.endsWith("/")) {
      persistenceUnitRoot = persistenceUnitRoot.substring(0, persistenceUnitRoot.length() - 1);
    }
    return new URL(persistenceUnitRoot);
  }

}
