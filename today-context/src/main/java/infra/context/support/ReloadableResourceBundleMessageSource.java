/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import infra.context.ResourceLoaderAware;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.DefaultPropertiesPersister;
import infra.util.PropertiesPersister;
import infra.util.StringUtils;

/**
 * Framework-specific {@link infra.context.MessageSource} implementation
 * that accesses resource bundles using specified basenames, participating in the
 * Framework {@link infra.context.ApplicationContext}'s resource loading.
 *
 * <p>In contrast to the JDK-based {@link ResourceBundleMessageSource}, this class uses
 * {@link java.util.Properties} instances as its custom data structure for messages,
 * loading them via a {@link PropertiesPersister} strategy
 * from Framework {@link Resource} handles. This strategy is not only capable of
 * reloading files based on timestamp changes, but also of loading properties files
 * with a specific character encoding. It will detect XML property files as well.
 *
 * <p>Note that the basenames set as {@link #setBasenames "basenames"} property
 * are treated in a slightly different fashion than the "basenames" property of
 * {@link ResourceBundleMessageSource}. It follows the basic ResourceBundle rule of not
 * specifying file extension or language codes, but can refer to any Framework resource
 * location (instead of being restricted to classpath resources). With a "classpath:"
 * prefix, resources can still be loaded from the classpath, but "cacheSeconds" values
 * other than "-1" (caching forever) might not work reliably in this case.
 *
 * <p>For a typical web application, message files could be placed in {@code WEB-INF}:
 * e.g. a "WEB-INF/messages" basename would find a "WEB-INF/messages.properties",
 * "WEB-INF/messages_en.properties" etc arrangement as well as "WEB-INF/messages.xml",
 * "WEB-INF/messages_en.xml" etc. Note that message definitions in a <i>previous</i>
 * resource bundle will override ones in a later bundle, due to sequential lookup.
 *
 * <p>This MessageSource can easily be used outside of an
 * {@link infra.context.ApplicationContext}: it will use a
 * {@link DefaultResourceLoader} as default,
 * simply getting overridden with the ApplicationContext's resource loader
 * if running in a context. It does not have any other specific dependencies.
 *
 * <p>Thanks to Thomas Achleitner for providing the initial implementation of
 * this message source!
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setCacheSeconds
 * @see #setBasenames
 * @see #setDefaultEncoding
 * @see #setFileEncodings
 * @see #setPropertiesPersister
 * @see #setResourceLoader
 * @see DefaultResourceLoader
 * @see ResourceBundleMessageSource
 * @see java.util.ResourceBundle
 * @since 4.0
 */
public class ReloadableResourceBundleMessageSource extends AbstractResourceBasedMessageSource implements ResourceLoaderAware {

  private static final String XML_EXTENSION = ".xml";

  private List<String> fileExtensions = List.of(".properties", XML_EXTENSION);

  @Nullable
  private Properties fileEncodings;

  private boolean concurrentRefresh = true;

  private PropertiesPersister propertiesPersister = DefaultPropertiesPersister.INSTANCE;

  private ResourceLoader resourceLoader = new DefaultResourceLoader();

  // Cache to hold filename lists per Locale
  private final ConcurrentMap<String, Map<Locale, List<String>>> cachedFilenames = new ConcurrentHashMap<>();

  // Cache to hold already loaded properties per filename
  private final ConcurrentMap<String, PropertiesHolder> cachedProperties = new ConcurrentHashMap<>();

  // Cache to hold already loaded properties per filename
  private final ConcurrentMap<Locale, PropertiesHolder> cachedMergedProperties = new ConcurrentHashMap<>();

  /**
   * Set the list of supported file extensions.
   * <p>The default is a list containing {@code .properties} and {@code .xml}.
   *
   * @param fileExtensions the file extensions (starts with a dot)
   */
  public void setFileExtensions(List<String> fileExtensions) {
    Assert.isTrue(CollectionUtils.isNotEmpty(fileExtensions), "At least one file extension is required");
    for (String extension : fileExtensions) {
      if (!extension.startsWith(".")) {
        throw new IllegalArgumentException("File extension '" + extension + "' should start with '.'");
      }
    }
    this.fileExtensions = Collections.unmodifiableList(fileExtensions);
  }

  /**
   * Set per-file charsets to use for parsing properties files.
   * <p>Only applies to classic properties files, not to XML files.
   *
   * @param fileEncodings a Properties with filenames as keys and charset
   * names as values. Filenames have to match the basename syntax,
   * with optional locale-specific components: e.g. "WEB-INF/messages"
   * or "WEB-INF/messages_en".
   * @see #setBasenames
   * @see PropertiesPersister#load
   */
  public void setFileEncodings(Properties fileEncodings) {
    this.fileEncodings = fileEncodings;
  }

  /**
   * Specify whether to allow for concurrent refresh behavior, i.e. one thread
   * locked in a refresh attempt for a specific cached properties file whereas
   * other threads keep returning the old properties for the time being, until
   * the refresh attempt has completed.
   * <p>Default is "true"
   *
   * @see #setCacheSeconds
   */
  public void setConcurrentRefresh(boolean concurrentRefresh) {
    this.concurrentRefresh = concurrentRefresh;
  }

  /**
   * Set the PropertiesPersister to use for parsing properties files.
   * <p>The default is {@code DefaultPropertiesPersister}.
   *
   * @see DefaultPropertiesPersister#INSTANCE
   */
  public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
    this.propertiesPersister =
            (propertiesPersister != null ? propertiesPersister : DefaultPropertiesPersister.INSTANCE);
  }

  /**
   * Set the ResourceLoader to use for loading bundle properties files.
   * <p>The default is a DefaultResourceLoader. Will get overridden by the
   * ApplicationContext if running in a context, as it implements the
   * ResourceLoaderAware interface. Can be manually overridden when
   * running outside an ApplicationContext.
   *
   * @see DefaultResourceLoader
   * @see ResourceLoaderAware
   */
  @Override
  public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
    this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
  }

  /**
   * Resolves the given message code as key in the retrieved bundle files,
   * returning the value found in the bundle as-is (without MessageFormat parsing).
   */
  @Override
  protected String resolveCodeWithoutArguments(String code, Locale locale) {
    if (getCacheMillis() < 0) {
      PropertiesHolder propHolder = getMergedProperties(locale);
      return propHolder.getProperty(code);
    }
    else {
      for (String basename : getBasenameSet()) {
        List<String> filenames = calculateAllFilenames(basename, locale);
        for (String filename : filenames) {
          PropertiesHolder propHolder = getProperties(filename);
          String result = propHolder.getProperty(code);
          if (result != null) {
            return result;
          }
        }
      }
    }
    return null;
  }

  /**
   * Resolves the given message code as key in the retrieved bundle files,
   * using a cached MessageFormat instance per message code.
   */
  @Override
  @Nullable
  protected MessageFormat resolveCode(String code, Locale locale) {
    if (getCacheMillis() < 0) {
      PropertiesHolder propHolder = getMergedProperties(locale);
      return propHolder.getMessageFormat(code, locale);
    }
    else {
      for (String basename : getBasenameSet()) {
        List<String> filenames = calculateAllFilenames(basename, locale);
        for (String filename : filenames) {
          PropertiesHolder propHolder = getProperties(filename);
          MessageFormat result = propHolder.getMessageFormat(code, locale);
          if (result != null) {
            return result;
          }
        }
      }
    }
    return null;
  }

  /**
   * Get a PropertiesHolder that contains the actually visible properties
   * for a Locale, after merging all specified resource bundles.
   * Either fetches the holder from the cache or freshly loads it.
   * <p>Only used when caching resource bundle contents forever, i.e.
   * with cacheSeconds &lt; 0. Therefore, merged properties are always
   * cached forever.
   */
  protected PropertiesHolder getMergedProperties(Locale locale) {
    PropertiesHolder mergedHolder = this.cachedMergedProperties.get(locale);
    if (mergedHolder != null) {
      return mergedHolder;
    }
    mergedHolder = mergeProperties(collectPropertiesToMerge(locale));
    PropertiesHolder existing = this.cachedMergedProperties.putIfAbsent(locale, mergedHolder);
    if (existing != null) {
      mergedHolder = existing;
    }
    return mergedHolder;
  }

  /**
   * Determine the properties to merge based on the specified basenames.
   *
   * @param locale the locale
   * @return the list of properties holders
   * @see #getBasenameSet()
   * @see #calculateAllFilenames
   * @see #mergeProperties
   */
  protected List<PropertiesHolder> collectPropertiesToMerge(Locale locale) {
    String[] basenames = StringUtils.toStringArray(getBasenameSet());
    ArrayList<PropertiesHolder> holders = new ArrayList<>(basenames.length);
    for (int i = basenames.length - 1; i >= 0; i--) {
      List<String> filenames = calculateAllFilenames(basenames[i], locale);
      for (int j = filenames.size() - 1; j >= 0; j--) {
        String filename = filenames.get(j);
        PropertiesHolder propHolder = getProperties(filename);
        if (propHolder.getProperties() != null) {
          holders.add(propHolder);
        }
      }
    }
    return holders;
  }

  /**
   * Merge the given properties holders into a single holder.
   *
   * @param holders the list of properties holders
   * @return a single merged properties holder
   * @see #newProperties()
   * @see #getMergedProperties
   * @see #collectPropertiesToMerge
   */
  protected PropertiesHolder mergeProperties(List<PropertiesHolder> holders) {
    Properties mergedProps = newProperties();
    long latestTimestamp = -1;
    for (PropertiesHolder holder : holders) {
      mergedProps.putAll(holder.getProperties());
      if (holder.getFileTimestamp() > latestTimestamp) {
        latestTimestamp = holder.getFileTimestamp();
      }
    }
    return new PropertiesHolder(mergedProps, latestTimestamp);
  }

  /**
   * Calculate all filenames for the given bundle basename and Locale.
   * Will calculate filenames for the given Locale, the system Locale
   * (if applicable), and the default file.
   *
   * @param basename the basename of the bundle
   * @param locale the locale
   * @return the List of filenames to check
   * @see #setFallbackToSystemLocale
   * @see #calculateFilenamesForLocale
   */
  protected List<String> calculateAllFilenames(String basename, Locale locale) {
    Map<Locale, List<String>> localeMap = this.cachedFilenames.get(basename);
    if (localeMap != null) {
      List<String> filenames = localeMap.get(locale);
      if (filenames != null) {
        return filenames;
      }
    }

    // Filenames for given Locale
    List<String> filenames = new ArrayList<>(7);
    filenames.addAll(calculateFilenamesForLocale(basename, locale));

    // Filenames for default Locale, if any
    Locale defaultLocale = getDefaultLocale();
    if (defaultLocale != null && !defaultLocale.equals(locale)) {
      List<String> fallbackFilenames = calculateFilenamesForLocale(basename, defaultLocale);
      for (String fallbackFilename : fallbackFilenames) {
        if (!filenames.contains(fallbackFilename)) {
          // Entry for fallback locale that isn't already in filenames list.
          filenames.add(fallbackFilename);
        }
      }
    }

    // Filename for default bundle file
    filenames.add(basename);

    if (localeMap == null) {
      localeMap = new ConcurrentHashMap<>();
      Map<Locale, List<String>> existing = this.cachedFilenames.putIfAbsent(basename, localeMap);
      if (existing != null) {
        localeMap = existing;
      }
    }
    localeMap.put(locale, filenames);
    return filenames;
  }

  /**
   * Calculate the filenames for the given bundle basename and Locale,
   * appending language code, country code, and variant code.
   * <p>For example, basename "messages", Locale "de_AT_oo" &rarr; "messages_de_AT_OO",
   * "messages_de_AT", "messages_de".
   * <p>Follows the rules defined by {@link java.util.Locale#toString()}.
   *
   * @param basename the basename of the bundle
   * @param locale the locale
   * @return the List of filenames to check
   */
  protected List<String> calculateFilenamesForLocale(String basename, Locale locale) {
    ArrayList<String> result = new ArrayList<>(3);
    String language = locale.getLanguage();
    String country = locale.getCountry();
    String variant = locale.getVariant();
    StringBuilder temp = new StringBuilder(basename);

    temp.append('_');
    if (!language.isEmpty()) {
      temp.append(language);
      result.add(0, temp.toString());
    }

    temp.append('_');
    if (!country.isEmpty()) {
      temp.append(country);
      result.add(0, temp.toString());
    }

    if (!variant.isEmpty() && (!language.isEmpty() || !country.isEmpty())) {
      temp.append('_').append(variant);
      result.add(0, temp.toString());
    }

    return result;
  }

  /**
   * Get a PropertiesHolder for the given filename, either from the
   * cache or freshly loaded.
   *
   * @param filename the bundle filename (basename + Locale)
   * @return the current PropertiesHolder for the bundle
   */
  protected PropertiesHolder getProperties(String filename) {
    PropertiesHolder propHolder = this.cachedProperties.get(filename);
    long originalTimestamp = -2;

    if (propHolder != null) {
      originalTimestamp = propHolder.getRefreshTimestamp();
      if (originalTimestamp == -1 || originalTimestamp > System.currentTimeMillis() - getCacheMillis()) {
        // Up to date
        return propHolder;
      }
    }
    else {
      propHolder = new PropertiesHolder();
      PropertiesHolder existingHolder = this.cachedProperties.putIfAbsent(filename, propHolder);
      if (existingHolder != null) {
        propHolder = existingHolder;
      }
    }

    // At this point, we need to refresh...
    if (this.concurrentRefresh && propHolder.getRefreshTimestamp() >= 0) {
      // A populated but stale holder -> could keep using it.
      if (!propHolder.refreshLock.tryLock()) {
        // Getting refreshed by another thread already ->
        // let's return the existing properties for the time being.
        return propHolder;
      }
    }
    else {
      propHolder.refreshLock.lock();
    }
    try {
      PropertiesHolder existingHolder = this.cachedProperties.get(filename);
      if (existingHolder != null && existingHolder.getRefreshTimestamp() > originalTimestamp) {
        return existingHolder;
      }
      return refreshProperties(filename, propHolder);
    }
    finally {
      propHolder.refreshLock.unlock();
    }
  }

  /**
   * Refresh the PropertiesHolder for the given bundle filename.
   * <p>The holder can be {@code null} if not cached before, or a timed-out cache entry
   * (potentially getting re-validated against the current last-modified timestamp).
   *
   * @param filename the bundle filename (basename + Locale)
   * @param propHolder the current PropertiesHolder for the bundle
   * @see #resolveResource(String)
   */
  protected PropertiesHolder refreshProperties(String filename, @Nullable PropertiesHolder propHolder) {
    long refreshTimestamp = (getCacheMillis() < 0 ? -1 : System.currentTimeMillis());

    Resource resource = resolveResource(filename);
    if (resource != null) {
      long fileTimestamp = -1;
      if (getCacheMillis() >= 0) {
        // Last-modified timestamp of file will just be read if caching with timeout.
        try {
          fileTimestamp = resource.lastModified();
          if (propHolder != null && propHolder.getFileTimestamp() == fileTimestamp) {
            if (logger.isDebugEnabled()) {
              logger.debug("Re-caching properties for filename [{}] - file hasn't been modified", filename);
            }
            propHolder.setRefreshTimestamp(refreshTimestamp);
            return propHolder;
          }
        }
        catch (IOException ex) {
          // Probably a class path resource: cache it forever.
          logger.debug("{} could not be resolved in the file system - assuming that it hasn't changed", resource, ex);
          fileTimestamp = -1;
        }
      }
      try {
        Properties props = loadProperties(resource, filename);
        propHolder = new PropertiesHolder(props, fileTimestamp);
      }
      catch (IOException ex) {
        if (logger.isWarnEnabled()) {
          logger.warn("Could not parse properties file [{}]", resource.getName(), ex);
        }
        // Empty holder representing "not valid".
        propHolder = new PropertiesHolder();
      }
    }

    else {
      // Resource does not exist.
      if (logger.isDebugEnabled()) {
        logger.debug("No properties file found for [{}] - neither plain properties nor XML", filename);
      }
      // Empty holder representing "not found".
      propHolder = new PropertiesHolder();
    }

    propHolder.setRefreshTimestamp(refreshTimestamp);
    this.cachedProperties.put(filename, propHolder);
    return propHolder;
  }

  /**
   * Resolve the specified bundle {@code filename} into a concrete {@link Resource},
   * potentially checking multiple sources or file extensions.
   * <p>If no suitable concrete {@code Resource} can be resolved, this method
   * returns a {@code Resource} for which {@link Resource#exists()} returns
   * {@code false}, which gets subsequently ignored.
   * <p>This can be leveraged to check the last modification timestamp or to load
   * properties from alternative sources &mdash; for example, from an XML BLOB
   * in a database, or from properties serialized using a custom format such as
   * JSON.
   * <p>The default implementation delegates to the configured
   * {@link #setResourceLoader(ResourceLoader) ResourceLoader} to resolve
   * resources, checking in order for existing {@code Resource} with extensions defined
   * by {@link #setFileExtensions(List)} ({@code .properties} and {@code .xml}
   * by default).
   * <p>When overriding this method, {@link #loadProperties(Resource, String)}
   * <strong>must</strong> be capable of loading properties from any type of
   * {@code Resource} returned by this method. As a consequence, implementors
   * are strongly encouraged to also override {@code loadProperties()}.
   * <p>As an alternative to overriding this method, you can configure a
   * {@link #setPropertiesPersister(PropertiesPersister) PropertiesPersister}
   * that is capable of dealing with all resources returned by this method.
   * Please note, however, that the default {@code loadProperties()} implementation
   * uses {@link PropertiesPersister#loadFromXml(Properties, InputStream) loadFromXml}
   * for XML resources and otherwise uses the two
   * {@link PropertiesPersister#load(Properties, InputStream) load} methods
   * for other types of resources.
   *
   * @param filename the bundle filename (basename + Locale)
   * @return the {@code Resource} to use
   */
  @Nullable
  protected Resource resolveResource(String filename) {
    for (String fileExtension : this.fileExtensions) {
      Resource resource = this.resourceLoader.getResource(filename + fileExtension);
      if (resource.exists()) {
        return resource;
      }
    }
    return null;
  }

  /**
   * Load the properties from the given resource.
   *
   * @param resource the resource to load from
   * @param filename the original bundle filename (basename + Locale)
   * @return the populated Properties instance
   * @throws IOException if properties loading failed
   */
  protected Properties loadProperties(Resource resource, String filename) throws IOException {
    Properties props = newProperties();
    try (InputStream is = resource.getInputStream()) {
      String resourceFilename = resource.getName();
      if (resourceFilename != null && resourceFilename.endsWith(XML_EXTENSION)) {
        if (logger.isDebugEnabled()) {
          logger.debug("Loading properties [{}]", resource.getName());
        }
        this.propertiesPersister.loadFromXml(props, is);
      }
      else {
        String encoding = null;
        if (this.fileEncodings != null) {
          encoding = this.fileEncodings.getProperty(filename);
        }
        if (encoding == null) {
          encoding = getDefaultEncoding();
        }
        if (encoding != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("Loading properties [{}] with encoding '{}'", resource.getName(), encoding);
          }
          this.propertiesPersister.load(props, new InputStreamReader(is, encoding));
        }
        else {
          if (logger.isDebugEnabled()) {
            logger.debug("Loading properties [{}]", resource.getName());
          }
          this.propertiesPersister.load(props, is);
        }
      }
      return props;
    }
  }

  /**
   * Template method for creating a plain new {@link Properties} instance.
   * The default implementation simply calls {@link Properties#Properties()}.
   * <p>Allows for returning a custom {@link Properties} extension in subclasses.
   * Overriding methods should just instantiate a custom {@link Properties} subclass,
   * with no further initialization or population to be performed at that point.
   *
   * @return a plain Properties instance
   */
  protected Properties newProperties() {
    return new Properties();
  }

  /**
   * Clear the resource bundle cache.
   * Subsequent resolve calls will lead to reloading of the properties files.
   */
  public void clearCache() {
    logger.debug("Clearing entire resource bundle cache");
    this.cachedProperties.clear();
    this.cachedMergedProperties.clear();
  }

  /**
   * Clear the resource bundle caches of this MessageSource and all its ancestors.
   *
   * @see #clearCache
   */
  public void clearCacheIncludingAncestors() {
    clearCache();
    if (getParentMessageSource() instanceof ReloadableResourceBundleMessageSource reloadableMsgSrc) {
      reloadableMsgSrc.clearCacheIncludingAncestors();
    }
  }

  @Override
  public String toString() {
    return getClass().getName() + ": basenames=" + getBasenameSet();
  }

  /**
   * PropertiesHolder for caching.
   * Stores the last-modified timestamp of the source file for efficient
   * change detection, and the timestamp of the last refresh attempt
   * (updated every time the cache entry gets re-validated).
   */
  protected class PropertiesHolder {

    @Nullable
    private final Properties properties;

    private final long fileTimestamp;

    private volatile long refreshTimestamp = -2;

    private final ReentrantLock refreshLock = new ReentrantLock();

    /** Cache to hold already generated MessageFormats per message code. */
    private final ConcurrentMap<String, Map<Locale, MessageFormat>> cachedMessageFormats =
            new ConcurrentHashMap<>();

    public PropertiesHolder() {
      this.properties = null;
      this.fileTimestamp = -1;
    }

    public PropertiesHolder(Properties properties, long fileTimestamp) {
      this.properties = properties;
      this.fileTimestamp = fileTimestamp;
    }

    @Nullable
    public Properties getProperties() {
      return this.properties;
    }

    public long getFileTimestamp() {
      return this.fileTimestamp;
    }

    public void setRefreshTimestamp(long refreshTimestamp) {
      this.refreshTimestamp = refreshTimestamp;
    }

    public long getRefreshTimestamp() {
      return this.refreshTimestamp;
    }

    @Nullable
    public String getProperty(String code) {
      if (this.properties == null) {
        return null;
      }
      return this.properties.getProperty(code);
    }

    @Nullable
    public MessageFormat getMessageFormat(String code, Locale locale) {
      if (this.properties == null) {
        return null;
      }
      Map<Locale, MessageFormat> localeMap = this.cachedMessageFormats.get(code);
      if (localeMap != null) {
        MessageFormat result = localeMap.get(locale);
        if (result != null) {
          return result;
        }
      }
      String msg = this.properties.getProperty(code);
      if (msg != null) {
        if (localeMap == null) {
          localeMap = new ConcurrentHashMap<>();
          Map<Locale, MessageFormat> existing = this.cachedMessageFormats.putIfAbsent(code, localeMap);
          if (existing != null) {
            localeMap = existing;
          }
        }
        MessageFormat result = createMessageFormat(msg, locale);
        localeMap.put(locale, result);
        return result;
      }
      return null;
    }
  }

}
