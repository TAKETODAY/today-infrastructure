/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.context.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.core.Constant;
import cn.taketoday.core.ThrowableSupplier;
import cn.taketoday.core.io.FileBasedResource;
import cn.taketoday.core.io.JarEntryResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceFilter;
import cn.taketoday.core.utils.Assert;
import cn.taketoday.core.utils.ClassUtils;
import cn.taketoday.core.utils.ResourceUtils;
import cn.taketoday.core.utils.StringUtils;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;

import static cn.taketoday.core.Constant.PACKAGE_SEPARATOR;
import static cn.taketoday.core.Constant.PATH_SEPARATOR;

/**
 * Candidate Component Scanner
 *
 * @author TODAY 2019-11-26 20:02
 */
public class CandidateComponentScanner {
  // @since 4.0
  public static final String KEY_STRATEGIES_IGNORE_JAR_PREFIX = "ignore-jar-prefix";
  private static final Logger log = LoggerFactory.getLogger(CandidateComponentScanner.class);

  private Set<Class<?>> candidates;

  private String[] ignoreScanJarPrefixs;
  private int initialCandidatesCapacity = 512;
  private Predicate<Resource> jarResourceFilter;
  private static String[] defaultIgnoreScanJarPrefixs;
  private boolean useDefaultIgnoreScanJarPrefix = true;

  private ClassLoader classLoader = ClassUtils.getClassLoader();

  private int scanningTimes = 0;

  private static CandidateComponentScanner sharedScanner = new CandidateComponentScanner();

  /** Class resource filter */
  private static final ResourceFilter CLASS_RESOURCE_FILTER = resource ->
          resource.isDirectory()
                  || (resource.getName().endsWith(Constant.CLASS_FILE_SUFFIX)
                  && !resource.getName().startsWith("package-info"));

  public static String[] getDefaultIgnoreJarPrefix() {
    if (defaultIgnoreScanJarPrefixs != null) {
      return defaultIgnoreScanJarPrefixs;
    }
    log.info("Loading 'META-INF/ignore/jar-prefix'");

    // Load the META-INF/ignore/jar-prefix to ignore some jars
    // --------------------------------------------------------------
    final HashSet<String> ignoreScanJars = new HashSet<>(64);

    readFromMetaInfoIgnore(ignoreScanJars);
    // @since 4.0 read from strategies file
    final StrategiesDetector strategiesDetector = StrategiesDetector.getSharedInstance();
    final Collection<String> strategies = strategiesDetector.getStrategies(KEY_STRATEGIES_IGNORE_JAR_PREFIX);
    ignoreScanJars.addAll(strategies);

    return defaultIgnoreScanJarPrefixs = ignoreScanJars.toArray(new String[ignoreScanJars.size()]);
  }

  @Deprecated
  private static void readFromMetaInfoIgnore(HashSet<String> ignoreScanJars) {
    try { // @since 2.1.6
      final Enumeration<URL> resources = ClassUtils.getClassLoader().getResources("META-INF/ignore/jar-prefix");
      final Charset charset = Constant.DEFAULT_CHARSET;

      while (resources.hasMoreElements()) {
        try (final BufferedReader reader = //
                new BufferedReader(new InputStreamReader(resources.nextElement().openStream(), charset))) {

          String str;
          while ((str = reader.readLine()) != null) {
            if (StringUtils.isNotEmpty(str)) { // @since 3.0 FIX empty lines
              ignoreScanJars.add(str);
            }
          }
        }
      }
    }
    catch (IOException e) {
      throw new ApplicationContextException("IOException occurred when load 'META-INF/ignore/jar-prefix'", e);
    }
  }

  public CandidateComponentScanner() {}

  public CandidateComponentScanner(int initialCapacity) {
    this.initialCandidatesCapacity = initialCapacity;
  }

  public CandidateComponentScanner(Set<Class<?>> candidates) {
    this.candidates = candidates;
  }

  /**
   * Find class by annotation.
   *
   * @param annotationClass
   *         annotation class
   *
   * @return the set of class
   */
  public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) {
    return filter(clazz -> clazz.isAnnotationPresent(annotationClass));
  }

  /**
   * Get all child classes in class path
   *
   * @param superClass
   *         super class or a interface class
   *
   * @return a {@link Collection} of impl class
   */
  public Set<Class<?>> getImplementationClasses(Class<?> superClass) {
    return filter(clazz -> superClass.isAssignableFrom(clazz) && superClass != clazz);
  }

  /**
   * Get all child classes in class path filter with package name
   *
   * @param superClass
   *         super class or a interface class
   * @param packageName
   *         package name
   *
   * @return a {@link Collection} of impl class
   */
  public Set<Class<?>> getImplementationClasses(Class<?> superClass, String packageName) {
    return filter(clazz -> clazz.getName().startsWith(packageName)
            && superClass != clazz
            && superClass.isAssignableFrom(clazz) //
    );
  }

  public final Set<Class<?>> filter(final Predicate<Class<?>> predicate) {
    return getScanningCandidates()
            .parallelStream()
            .filter(predicate)
            .collect(Collectors.toSet());
  }

  /**
   * Get {@link Collection} of class under the packages
   *
   * @param packages
   *         package name
   *
   * @return a {@link Collection} of class under the packages
   */
  public Set<Class<?>> getClasses(final String... packages) {
    return filter(clazz -> {
      final String name = clazz.getName();
      for (final String prefix : packages) {
        if (StringUtils.isEmpty(prefix) || name.startsWith(prefix)) {
          return true;
        }
      }
      return false;
    });
  }

  /**
   * Scan class with given package.
   *
   * @param packages
   *         The packages to scan
   *
   * @return Class set
   */
  public Set<Class<?>> scan(final String... packages) {
    Assert.notNull(packages, "scan packages can't be null");

    if (packages.length == 1) {
      return scanOne(packages[0]); // packages.length == 1
    }

    final Set<String> packagesToScan = new HashSet<>(8);
    for (final String location : packages) {
      if (StringUtils.isEmpty(location)) { // contains "" scan all class
        return scan();
      }
      else {
        packagesToScan.add(location);
      }
    }
    for (final String location : packagesToScan) {
      scan(location);
    }
    return getScanningCandidates();
  }

  protected Set<Class<?>> scanOne(final String location) {
    if (StringUtils.isEmpty(location)) {
      return scan();
    }
    return scan(location);
  }

  /**
   * Scan classes to classes set
   *
   * @param packageName
   *         Package name
   *
   * @return candidates class
   */
  public Set<Class<?>> scan(final String packageName) {
    final String resourceToUse = packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
    if (log.isDebugEnabled()) {
      log.debug("Scanning component candidates from package: [{}]", packageName);
    }
    try {
      final Enumeration<URL> uri = getClassLoader().getResources(resourceToUse);
      while (uri.hasMoreElements()) {
        scan(ResourceUtils.getResource(uri.nextElement()), packageName);
      }
      scanningTimes++;
      return getScanningCandidates();
    }
    catch (IOException e) {
      throw new ApplicationContextException("IO exception occur With Msg: [" + e + ']', e);
    }
  }

  /**
   * Scan class in a {@link Resource}
   *
   * @param resource
   *         {@link Resource} in class maybe a jar file or class directory
   * @param packageName
   *         if {@link Resource} is a directory will use this packageName
   *
   * @throws IOException
   *         if the resource is not available
   * @since 2.1.6
   */
  protected void scan(final Resource resource, final String packageName) throws IOException {
    if (log.isTraceEnabled()) {
      log.trace("Scanning candidate components in [{}]", resource.getLocation());
    }
    if (resource instanceof FileBasedResource) {
      if (resource.isDirectory()) {
        findInDirectory(resource);
        return;
      }
      if (resource.getName().endsWith(".jar")) {
        scanInJarFile(resource, packageName, () -> new JarFile(resource.getFile()));
      }
    }
    else if (resource instanceof JarEntryResource) {
      scanInJarFile(resource, packageName, ((JarEntryResource) resource)::getJarFile);
    }
  }

  protected void scanInJarFile(final Resource resource,
                               final String packageName,
                               final ThrowableSupplier<JarFile, IOException> jarFileSupplier) throws IOException //
  {
    if (getJarResourceFilter().test(resource)) {
      if (log.isTraceEnabled()) {
        log.trace("Scan in jar file: [{}]", resource.getLocation());
      }
      try (final JarFile jarFile = jarFileSupplier.get()) {
        final Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
          loadClassFromJarEntry(jarEntries.nextElement(), packageName);
        }
      }
    }
  }

  /**
   * Scan all the classpath classes
   *
   * @since 2.1.6
   */
  public Set<Class<?>> scan() {

    final ClassLoader classLoader = getClassLoader();
    log.debug("Use ClassLoader [{}] to scan", classLoader);
    try {
      final String blank = Constant.BLANK;
      if (classLoader instanceof URLClassLoader) {
        // fix: protocol is file not a jar protocol
        for (final URL url : ((URLClassLoader) classLoader).getURLs()) {
          scan(ResourceUtils.getResource(url), blank);
        }
      }
      else {
        final URL resource = classLoader.getResource(blank);
        Assert.notNull(resource, "Could't found class root path");
        scan(ResourceUtils.getResource(resource), blank);
      }
      scanningTimes++;
      return getScanningCandidates();
    }
    catch (IOException e) {
      throw new ApplicationContextException("IO exception occur When scan all the classpath classes, With Msg: [" + e + ']', e);
    }
  }

  /**
   * Load classes from a {@link JarEntry}
   *
   * @param jarEntry
   *         The entry of jar
   */
  public void loadClassFromJarEntry(final JarEntry jarEntry, final String packageName) {

    if (jarEntry.isDirectory()) {
      return;
    }
    final String jarEntryName = jarEntry.getName(); // cn/taketoday/xxx/yyy.class
    if (jarEntryName.endsWith(Constant.CLASS_FILE_SUFFIX)) {

      // fix #10 classes loading from a jar can't be load
      final String nameToUse = jarEntryName.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);

      if (StringUtils.isEmpty(packageName) || nameToUse.startsWith(packageName)) {
        try {
          final String className = nameToUse.substring(0, nameToUse.lastIndexOf(PACKAGE_SEPARATOR));
          getScanningCandidates().add(getClassLoader().loadClass(className));
        }
        catch (ClassNotFoundException | Error ignored) {
        }
      }
    }
  }

  /**
   * <p>
   * Find in directory.
   * </p>
   * Note: don't need packageName
   *
   * @throws IOException
   *         if the resource is not available
   */
  protected void findInDirectory(final Resource directory) throws IOException {

    if (!directory.exists()) {
      log.error("The location: [{}] you provided that does not exist", directory.getLocation());
      return;
    }

    if (log.isTraceEnabled()) {
      log.trace("Enter: [{}]", directory.getLocation());
    }

    final ClassLoader classLoader = getClassLoader();
    final Set<Class<?>> candidates = getScanningCandidates();

    for (final Resource resource : directory.list(CLASS_RESOURCE_FILTER)) {
      if (resource.isDirectory()) { // recursive
        findInDirectory(resource);
      }
      else {
        try {
          candidates.add(classLoader.loadClass(ClassUtils.getClassName(resource)));
        }
        catch (ClassNotFoundException | Error ignored) {
        }
      }
    }
  }

  public void clear() {
    this.scanningTimes = 0;
    if (candidates != null) {
      candidates.clear();
    }
  }

  /**
   * The class path resources loader
   *
   * @return The class path resources loader
   */
  public ClassLoader getClassLoader() {
    final ClassLoader classLoader = this.classLoader;
    if (classLoader == null) {
      return this.classLoader = ClassUtils.getClassLoader();
    }
    return classLoader;
  }

  public CandidateComponentScanner setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  public Set<Class<?>> getCandidates() {
    return candidates;
  }

  public CandidateComponentScanner setCandidates(Set<Class<?>> candidates) {
    this.candidates = candidates;
    return this;
  }

  /**
   * Get Scanning Candidates
   *
   * <p>
   * this method unlike {@link #getCandidates()} returns null if Candidates have
   * not been set or scanned
   *
   * @return Get Scanning Candidates never be null
   */
  public final Set<Class<?>> getScanningCandidates() {
    final Set<Class<?>> candidates = getCandidates();
    if (candidates == null) {
      return this.candidates = new HashSet<>(initialCandidatesCapacity);
    }
    return candidates;
  }

  public String[] getIgnoreScanJarPrefixs() {
    return ignoreScanJarPrefixs;
  }

  public CandidateComponentScanner setIgnoreScanJarPrefixs(String... ignoreScanJarPrefixs) {
    this.ignoreScanJarPrefixs = ignoreScanJarPrefixs;
    return this;
  }

  public boolean isUseDefaultIgnoreScanJarPrefix() {
    return useDefaultIgnoreScanJarPrefix;
  }

  public CandidateComponentScanner setUseDefaultIgnoreScanJarPrefix(boolean useDefaultIgnoreScanJarPrefix) {
    this.useDefaultIgnoreScanJarPrefix = useDefaultIgnoreScanJarPrefix;
    return this;
  }

  public Predicate<Resource> getJarResourceFilter() {
    final Predicate<Resource> jarResourceFilter = this.jarResourceFilter;
    if (jarResourceFilter == null) {
      return this.jarResourceFilter = new DefaultJarResourcePredicate(this);
    }
    return jarResourceFilter;
  }

  public CandidateComponentScanner setJarResourceFilter(Predicate<Resource> jarResourceFilter) {
    this.jarResourceFilter = jarResourceFilter;
    return this;
  }

  /**
   * Get {@link ApplicationContext} startup Component Scanner
   *
   * @return {@link ApplicationContext} startup Component Scanner
   */
  public static CandidateComponentScanner getSharedInstance() {
    return sharedScanner;
  }

  public static void setSharedInstance(CandidateComponentScanner sharedScanner) {
    CandidateComponentScanner.sharedScanner = sharedScanner;
  }

  public int getInitialCandidatesCapacity() {
    return initialCandidatesCapacity;
  }

  public CandidateComponentScanner setInitialCandidatesCapacity(int initialCandidatesCapacity) {
    this.initialCandidatesCapacity = initialCandidatesCapacity;
    return this;
  }

  public final int getScanningTimes() {
    return scanningTimes;
  }

  static final class DefaultJarResourcePredicate implements Predicate<Resource> {
    private final CandidateComponentScanner scanner;

    DefaultJarResourcePredicate(CandidateComponentScanner scanner) {
      this.scanner = scanner;
    }

    @Override
    public boolean test(Resource resource) {

      if (scanner.isUseDefaultIgnoreScanJarPrefix()) {

        final String fileName = resource.getName();
        for (final String ignoreJarName : getDefaultIgnoreJarPrefix()) {
          if (fileName.startsWith(ignoreJarName)) {
            return false;
          }
        }
      }

      final String[] ignoreScanJarPrefixs = scanner.getIgnoreScanJarPrefixs();
      if (ignoreScanJarPrefixs != null) {
        final String fileName = resource.getName();

        for (final String ignoreJarName : ignoreScanJarPrefixs) {
          if (fileName.startsWith(ignoreJarName)) {
            return false;
          }
        }
      }

      return true;
    }
  }
}
