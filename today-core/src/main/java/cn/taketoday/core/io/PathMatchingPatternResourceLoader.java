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
package cn.taketoday.core.io;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ResolvedModule;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.PathMatcher;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

import static cn.taketoday.lang.Constant.BLANK;

/**
 * A {@link ResourceLoader} implementation that is able to resolve a specified
 * resource location path into one or more matching Resources. The source path
 * may be a simple path which has a one-to-one mapping to a target
 * {@link Resource}, or alternatively may contain the special
 * "{@code classpath*:}" prefix and/or internal Ant-style regular expressions
 * (matched using {@link AntPathMatcher} utility). Both of the latter
 * are effectively wildcards.
 *
 * <p>
 * <b>No Wildcards:</b>
 *
 * <p>
 * In the simple case, if the specified location path does not start with the
 * {@code "classpath*:}" prefix, and does not contain a PathMatcher pattern,
 * this resolver will simply return a single resource via a
 * {@code getResource()} call on the underlying {@code ResourceLoader}. Examples
 * are real URLs such as "{@code file:C:/context.xml}", pseudo-URLs such as
 * "{@code classpath:/context.xml}", and simple unprefixed paths such as
 * "{@code /WEB-INF/context.xml}". The latter will resolve in a fashion specific
 * to the underlying {@code ResourceLoader} (e.g. {@code ServletContextResource}
 * for a {@code WebApplicationContext}).
 *
 * <p>
 * <b>Ant-style Patterns:</b>
 *
 * <p>
 * When the path location contains an Ant-style pattern, e.g.:
 * <pre class="code">
 * /WEB-INF/*-context.xml
 * com/mycompany/**&#47;applicationContext.xml
 * file:C:/some/path/*-context.xml
 * classpath:com/mycompany/**&#47;applicationContext.xml</pre> the resolver
 * follows a more complex but defined procedure to try to resolve the wildcard.
 * It produces a {@code Resource} for the path up to the last non-wildcard
 * segment and obtains a {@code URL} from it. If this URL is not a
 * "{@code jar:}" URL or container-specific variant (e.g. "{@code zip:}" in
 * WebLogic, "{@code wsjar}" in WebSphere", etc.), then a {@code java.io.File}
 * is obtained from it, and used to resolve the wildcard by walking the
 * filesystem. In the case of a jar URL, the resolver either gets a
 * {@code java.net.JarURLConnection} from it, or manually parses the jar URL,
 * and then traverses the contents of the jar file, to resolve the wildcards.
 *
 * <p>
 * <b>Implications on portability:</b>
 *
 * <p>
 * If the specified path is already a file URL (either explicitly, or implicitly
 * because the base {@code ResourceLoader} is a filesystem one, then wildcarding
 * is guaranteed to work in a completely portable fashion.
 *
 * <p>
 * If the specified path is a classpath location, then the resolver must obtain
 * the last non-wildcard path segment URL via a
 * {@code Classloader.getResource()} call. Since this is just a node of the path
 * (not the file at the end) it is actually undefined (in the ClassLoader
 * Javadocs) exactly what sort of a URL is returned in this case. In practice,
 * it is usually a {@code java.io.File} representing the directory, where the
 * classpath resource resolves to a filesystem location, or a jar URL of some
 * sort, where the classpath resource resolves to a jar location. Still, there
 * is a portability concern on this operation.
 *
 * <p>
 * If a jar URL is obtained for the last non-wildcard segment, the resolver must
 * be able to get a {@code java.net.JarURLConnection} from it, or manually parse
 * the jar URL, to be able to walk the contents of the jar, and resolve the
 * wildcard. This will work in most environments, but will fail in others, and
 * it is strongly recommended that the wildcard resolution of resources coming
 * from jars be thoroughly tested in your specific environment before you rely
 * on it.
 *
 * <p>
 * <b>{@code classpath*:} Prefix:</b>
 *
 * <p>
 * There is special support for retrieving multiple class path resources with
 * the same name, via the "{@code classpath*:}" prefix. For example,
 * "{@code classpath*:META-INF/beans.xml}" will find all "beans.xml" files in
 * the class path, be it in "classes" directories or in JAR files. This is
 * particularly useful for autodetecting config files of the same name at the
 * same location within each jar file. Internally, this happens via a
 * {@code ClassLoader.getResources()} call, and is completely portable.
 *
 * <p>
 * The "classpath*:" prefix can also be combined with a PathMatcher pattern in
 * the rest of the location path, for example "classpath*:META-INF/*-beans.xml".
 * In this case, the resolution strategy is fairly simple: a
 * {@code ClassLoader.getResources()} call is used on the last non-wildcard path
 * segment to get all the matching resources in the class loader hierarchy, and
 * then off each resource the same PathMatcher resolution strategy described
 * above is used for the wildcard subpath.
 *
 * <p>
 * <b>Other notes:</b>
 *
 * <p>As of 4.0, if {@link #getResources(String)} is invoked with a location
 * pattern using the "classpath*:" prefix it will first search  all modules
 * in the {@linkplain ModuleLayer#boot() boot layer}, excluding {@linkplain
 * ModuleFinder#ofSystem() system modules}. It will then search the classpath
 * using {@link ClassLoader} APIs as described previously and return the
 * combined results. Consequently, some of the limitations of classpath
 * searches may not apply when applications are deployed as modules.
 *
 * <p>
 * <b>WARNING:</b> Note that "{@code classpath*:}" when combined with Ant-style
 * patterns will only work reliably with at least one root directory before the
 * pattern starts, unless the actual target files reside in the file system.
 * This means that a pattern like "{@code classpath*:*.xml}" will <i>not</i>
 * retrieve files from the root of jar files but rather only from the root of
 * expanded directories. This originates from a limitation in the JDK's
 * {@code ClassLoader.getResources()} method which only returns file system
 * locations for a passed-in empty String (indicating potential roots to
 * search). This {@code ResourcePatternResolver} implementation is trying to
 * mitigate the jar root lookup limitation through {@link URLClassLoader}
 * introspection and "java.class.path" manifest evaluation; however, without
 * portability guarantees.
 *
 * <p>
 * <b>WARNING:</b> Ant-style patterns with "classpath:" resources are not
 * guaranteed to find matching resources if the root package to search is
 * available in multiple class path locations. This is because a resource such
 * as <pre class="code">
 *     com/mycompany/package1/service-context.xml
 * </pre> may be in only one location, but when a path such as
 * <pre class="code">
 *     classpath:com/mycompany/**&#47;service-context.xml
 * </pre> is used to try to resolve it, the resolver will work off the (first)
 * URL returned by {@code getResource("com/mycompany");}. If this base package
 * node exists in multiple classloader locations, the actual end resource may
 * not be underneath. Therefore, preferably, use "{@code classpath*:}" with the
 * same Ant-style pattern in such a case, which will search <i>all</i> class
 * path locations that contain the root package.
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Marius Bogoevici
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @author TODAY 2019-12-05 12:51
 * @see AntPathMatcher
 * @see ClassLoader#getResources(String)
 * @since 2.1.7
 */
public class PathMatchingPatternResourceLoader implements PatternResourceLoader {
  private static final Logger log = LoggerFactory.getLogger(PathMatchingPatternResourceLoader.class);

  /**
   * {@link Set} of {@linkplain ModuleFinder#ofSystem() system module} names.
   *
   * @see #isNotSystemModule
   * @since 4.0
   */
  private static final Set<String> systemModuleNames = ModuleFinder.ofSystem().findAll()
          .stream().map(moduleReference -> moduleReference.descriptor().name()).collect(Collectors.toSet());

  /**
   * {@link Predicate} that tests whether the supplied {@link ResolvedModule}
   * is not a {@linkplain ModuleFinder#ofSystem() system module}.
   *
   * @see #systemModuleNames
   * @since 4.0
   */
  private static final Predicate<ResolvedModule> isNotSystemModule =
          Predicate.not(resolvedModule -> systemModuleNames.contains(resolvedModule.name()));

  private PathMatcher pathMatcher = new AntPathMatcher();
  private final ResourceLoader resourceLoader;

  /**
   * Create a new PathMatchingResourcePatternResolver with a DefaultResourceLoader.
   * <p>ClassLoader access will happen via the thread context class loader.
   *
   * @see DefaultResourceLoader
   */
  public PathMatchingPatternResourceLoader() {
    this.resourceLoader = new DefaultResourceLoader();
  }

  /**
   * Create a new PathMatchingResourcePatternResolver.
   * <p>ClassLoader access will happen via the thread context class loader.
   *
   * @param resourceLoader the ResourceLoader to load root directories and
   * actual resources with
   */
  public PathMatchingPatternResourceLoader(ResourceLoader resourceLoader) {
    Assert.notNull(resourceLoader, "ResourceLoader must not be null");
    this.resourceLoader = resourceLoader;
  }

  /**
   * Create a new PathMatchingResourcePatternResolver with a DefaultResourceLoader.
   *
   * @param classLoader the ClassLoader to load classpath resources with,
   * or {@code null} for using the thread context class loader
   * at the time of actual resource access
   * @see DefaultResourceLoader
   */
  public PathMatchingPatternResourceLoader(@Nullable ClassLoader classLoader) {
    this.resourceLoader = new DefaultResourceLoader(classLoader);
  }

  @Override
  public ClassLoader getClassLoader() {
    return resourceLoader.getClassLoader();
  }

  /** @since 4.0 */
  public ResourceLoader getRootLoader() {
    return resourceLoader;
  }

  /**
   * Set the PathMatcher implementation to use for this resource pattern resolver.
   * Default is AntPathMatcher.
   *
   * @see AntPathMatcher
   */
  public void setPathMatcher(PathMatcher pathMatcher) {
    Assert.notNull(pathMatcher, "PathMatcher must not be null");
    this.pathMatcher = pathMatcher;
  }

  /**
   * Return the PathMatcher that this resource pattern resolver uses.
   */
  public PathMatcher getPathMatcher() {
    return this.pathMatcher;
  }

  @NonNull
  @Override
  public Resource getResource(String location) {
    return resourceLoader.getResource(location);
  }

  @Override
  public Set<Resource> getResources(String locationPattern) throws IOException {
    LinkedHashSet<Resource> result = new LinkedHashSet<>();
    scan(locationPattern, result::add);
    return result;
  }

  @Override
  public void scan(String locationPattern, ResourceConsumer consumer) throws IOException {
    Assert.notNull(locationPattern, "Location pattern must not be null");

    if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
      // a class path resource (multiple resources for same name possible)
      String locationPatternWithoutPrefix = locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length());
      // Search the module path first.
      findAllModulePathResources(locationPatternWithoutPrefix, consumer);
      if (getPathMatcher().isPattern(locationPatternWithoutPrefix)) {
        // a class path resource pattern
        findPathMatchingResources(locationPattern, consumer);
      }
      else {
        // all class path resources with the given name
        findAllClassPathResources(locationPatternWithoutPrefix, consumer);
      }
    }
    else {
      // Generally only look for a pattern after a prefix here,
      // and on Tomcat only after the "*/" separator for its "war:" protocol.
      int prefixEnd = locationPattern.startsWith("war:") ?
                      locationPattern.indexOf("*/") :
                      locationPattern.indexOf(':');

      if (getPathMatcher().isPattern(prefixEnd > -1 ? locationPattern.substring(prefixEnd + 1) : locationPattern)) {
        findPathMatchingResources(locationPattern, consumer); // a file pattern
      }
      else {
        // a single resource with the given name
        Resource resource = getResource(locationPattern);
        consumer.accept(resource);
      }
    }
  }

  /**
   * Find all class location resources with the given location via the
   * ClassLoader. Delegates to {@link #doFindAllClassPathResources(String, ResourceConsumer)}.
   *
   * @param location the absolute path within the classpath
   * @param consumer Resource consumer
   * @throws IOException in case of I/O errors
   * @see java.lang.ClassLoader#getResources
   * @see #convertClassLoaderURL
   */
  protected void findAllClassPathResources(String location, ResourceConsumer consumer) throws IOException {
    String path = stripLeadingSlash(location);
    doFindAllClassPathResources(path, consumer);
  }

  /**
   * Find all class location resources with the given path via the ClassLoader.
   * Called by {@link #findAllClassPathResources(String, ResourceConsumer)}.
   *
   * @param path the absolute path within the classpath (never a leading slash)
   * @param consumer Resource consumer
   */
  protected void doFindAllClassPathResources(String path, ResourceConsumer consumer) throws IOException {
    ClassLoader cl = getClassLoader();
    if (StringUtils.isEmpty(path)) {
      // The above result is likely to be incomplete, i.e. only containing file system references.
      // We need to have pointers to each of the jar files on the classpath as well...
      addAllClassLoaderJarRoots(cl, consumer);
    }
    else {
      Enumeration<URL> resourceUrls = cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path);
      while (resourceUrls.hasMoreElements()) {
        URL url = resourceUrls.nextElement();
        consumer.accept(convertClassLoaderURL(url));
      }
    }
  }

  /**
   * Convert the given URL as returned from the ClassLoader into a
   * {@link Resource}.
   * <p>
   * The default implementation simply creates a {@link UrlResource} instance.
   *
   * @param url a URL as returned from the ClassLoader
   * @return the corresponding Resource object
   * @see java.lang.ClassLoader#getResources
   * @see Resource
   */
  protected Resource convertClassLoaderURL(URL url) {
    return ResourceUtils.getResource(url);
  }

  /**
   * Search all {@link URLClassLoader} URLs for jar file references and add them
   * to the given set of resources in the form of pointers to the root of the jar
   * file content.
   *
   * @param classLoader the ClassLoader to search (including its ancestors)
   * @param consumer Resource consumer
   */
  protected void addAllClassLoaderJarRoots(@Nullable ClassLoader classLoader, ResourceConsumer consumer) {
    if (classLoader instanceof URLClassLoader urlClassLoader) {
      try {
        for (URL url : urlClassLoader.getURLs()) {
          try { // jar file
            String path = url.getPath();
            if (path.endsWith(ResourceUtils.JAR_FILE_EXTENSION)) {
              JarEntryResource jarResource = new JarEntryResource(path);
              if (jarResource.exists()) {
                consumer.accept(jarResource);
              }
            }
          }
          catch (IOException ex) {
            log.debug("Cannot search for matching files underneath [{}] because it cannot be converted to a valid 'jar:' URL: {}",
                    url, ex.getMessage());
          }
        }
      }
      catch (Exception ex) {
        log.debug("Cannot introspect jar files since ClassLoader [{}] does not support 'getURLs()': {}",
                classLoader, ex);
      }
    }

    if (classLoader == ClassLoader.getSystemClassLoader()) {
      // "java.class.path" manifest evaluation...
      addClassPathManifestEntries(consumer);
    }

    if (classLoader != null) {
      try {
        addAllClassLoaderJarRoots(classLoader.getParent(), consumer); // Hierarchy traversal...
      }
      catch (Exception ex) {
        log.debug("Cannot introspect jar files in parent ClassLoader since [{}] does not support 'getParent()': {}",
                classLoader, ex.toString(), ex);
      }
    }
  }

  /**
   * Determine jar file references from the "java.class.path." manifest property
   * and add them to the given set of resources in the form of pointers to the
   * root of the jar file content.
   *
   * @param consumer Resource consumer
   */
  protected void addClassPathManifestEntries(ResourceConsumer consumer) {
    try {
      String javaClassPath = System.getProperty("java.class.path");
      String separator = System.getProperty("path.separator");

      for (String path : StringUtils.delimitedListToStringArray(javaClassPath, separator)) {
        try {
          if (!path.endsWith(ResourceUtils.JAR_FILE_EXTENSION)) {
            continue;
          }

          File jarFile = new File(path);
          String filePath = jarFile.getAbsolutePath();
          int prefixIndex = filePath.indexOf(':');
          if (prefixIndex == 1) {
            // Possibly "c:" drive prefix on Windows, to be upper-cased for proper duplicate detection
            filePath = StringUtils.capitalize(filePath);
          }
          String url = new StringBuilder(filePath.length() + 11)//JAR_ENTRY_URL_PREFIX+JAR_URL_SEPARATOR=11
                  .append(ResourceUtils.JAR_ENTRY_URL_PREFIX)
                  .append(filePath)
                  .append(ResourceUtils.JAR_URL_SEPARATOR).toString();

          JarEntryResource jarResource = new JarEntryResource(new URL(url), jarFile, BLANK);
          // Potentially overlapping with URLClassLoader.getURLs() result above!
          consumer.accept(jarResource);
        }
        catch (MalformedURLException ex) {
          log.debug("Cannot search for matching files underneath [{}] because it cannot be converted to a valid 'jar:' URL: {}",
                  path, ex.getMessage(), ex);
        }
      }
    }
    catch (Exception ex) {
      log.debug("Failed to evaluate 'java.class.path' manifest entries: ", ex);
    }
  }

  /**
   * Find all resources that match the given location pattern via the Ant-style
   * PathMatcher. Supports resources in jar files and zip files and in the file
   * system.
   *
   * @param locationPattern the location pattern to match
   * @throws IOException in case of I/O errors
   * @see #doFindPathMatchingJarResources
   * @see #doFindPathMatchingFileResources
   * @see PathMatcher
   */
  protected void findPathMatchingResources(String locationPattern, ResourceConsumer consumer) throws IOException {
    String rootDirPath = determineRootDir(locationPattern);
    String subPattern = locationPattern.substring(rootDirPath.length());
    scan(rootDirPath, rootDirResource -> rootDirResource(subPattern, rootDirResource, consumer));
  }

  protected void rootDirResource(
          String subPattern, Resource rootResource, ResourceConsumer consumer) throws IOException {
    if (rootResource instanceof JarResource) {
      doFindPathMatchingJarResources((JarResource) rootResource, subPattern, consumer);
    }
    else if (rootResource instanceof ClassPathResource) {
      Resource originalResource = ((ClassPathResource) rootResource).getOriginalResource();
      rootDirResource(subPattern, originalResource, consumer);
    }
    else {
      doFindPathMatchingFileResources(rootResource, subPattern, consumer);
    }
  }

  /**
   * Determine the root directory for the given location.
   * <p>
   * Used for determining the starting point for file matching, resolving the root
   * directory location to a {@code java.io.File} and passing it into
   * {@code retrieveMatchingFiles}, with the remainder of the location as pattern.
   * <p>
   * Will return "/WEB-INF/" for the pattern "/WEB-INF/*.xml", for example.
   *
   * @param location the location to check
   * @return the part of the location that denotes the root directory
   */
  protected String determineRootDir(String location) {
    int prefixEnd = location.indexOf(':') + 1;
    int rootDirEnd = location.length();
    PathMatcher pathMatcher = getPathMatcher();
    while (rootDirEnd > prefixEnd && pathMatcher.isPattern(location.substring(prefixEnd, rootDirEnd))) {
      rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
    }
    if (rootDirEnd == 0) {
      rootDirEnd = prefixEnd;
    }
    return location.substring(0, rootDirEnd);
  }

  /**
   * Find all resources in jar files that match the given location pattern via the
   * Ant-style PathMatcher.
   *
   * @param rootDirResource the root directory as Resource
   * @param subPattern the sub pattern to match (below the root directory)
   * @throws IOException in case of I/O errors
   * @see java.net.JarURLConnection
   * @see PathMatcher
   */
  protected void doFindPathMatchingJarResources(
          JarResource rootDirResource, String subPattern, ResourceConsumer consumer) throws IOException {

    URL rootDirURL = rootDirResource.getURL();

    // Should usually be the case for traditional JAR files.
    JarURLConnection jarCon = (JarURLConnection) rootDirURL.openConnection();
    ResourceUtils.useCachesIfNecessary(jarCon);
    JarEntry jarEntry = jarCon.getJarEntry();
    String rootEntryPath = jarEntry != null ? jarEntry.getName() : BLANK;
    boolean closeJarFile = !jarCon.getUseCaches();

    JarFile jarFile = rootDirResource.getJarFile();

    try {
      if (log.isDebugEnabled()) {
        String jarFileUrl = jarCon.getJarFileURL().toExternalForm();
        log.trace("Looking for matching resources in jar file [{}]", jarFileUrl);
      }
      if (!BLANK.equals(rootEntryPath) && !StringUtils.matchesLast(rootEntryPath, '/') /*rootEntryPath.endsWith("/")*/) {
        // Root entry path must end with slash to allow for proper matching.
        // The Sun JRE does not return a slash here, but BEA JRockit does.
        rootEntryPath = rootEntryPath.concat("/");
      }

      PathMatcher pathMatcher = getPathMatcher();
      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        String entryPath = entries.nextElement().getName();
        if (entryPath.startsWith(rootEntryPath)) {
          String relativePath = entryPath.substring(rootEntryPath.length());
          if (pathMatcher.match(subPattern, relativePath)) {
            Resource relative = rootDirResource.createRelative(relativePath);
            consumer.accept(relative);
          }
        }
      }
    }
    finally {
      if (closeJarFile) {
        jarFile.close();
      }
    }
  }

  /**
   * Find all resources in the file system of the supplied root directory that
   * match the given location sub pattern via the Ant-style PathMatcher.
   *
   * @param rootDirResource the root directory as a Resource
   * @param subPattern the sub pattern to match (below the root directory)
   * @param consumer Resource how to use
   * @throws IOException in case of I/O errors
   * @see PathMatcher
   */
  protected void doFindPathMatchingFileResources(
          Resource rootDirResource, String subPattern, ResourceConsumer consumer) throws IOException {

    URI rootDirUri;
    try {
      rootDirUri = rootDirResource.getURI();
    }
    catch (Exception ex) {
      log.info("Failed to resolve %s as URI: {}", rootDirResource, ex);
      return;
    }

    FileSystem fileSystem = null;
    try {
      Path rootPath = null;
      if (rootDirUri.isAbsolute() && !rootDirUri.isOpaque()) {
        // Prefer Path resolution from URI if possible
        try {
          try {
            rootPath = Path.of(rootDirUri);
          }
          catch (FileSystemNotFoundException ex) {
            // If the file system was not found, assume it's a custom file system that needs to be installed.
            fileSystem = FileSystems.newFileSystem(rootDirUri, Collections.emptyMap(), ClassUtils.getDefaultClassLoader());
            rootPath = Path.of(rootDirUri);
          }
        }
        catch (Exception ex) {
          log.debug("Failed to resolve {} in file system: {}", rootDirUri, ex);
          // Fallback via Resource.getFile() below
        }
      }
      if (rootPath == null) {
        // Resource.getFile() resolution as a fallback -
        // for custom URI formats and custom Resource implementations
        rootPath = Path.of(rootDirResource.getFile().getAbsolutePath());
      }

      String rootDir = StringUtils.cleanPath(rootPath.toString());
      if (!rootDir.endsWith("/")) {
        rootDir += "/";
      }

      String resourcePattern = rootDir + StringUtils.cleanPath(subPattern);

      Predicate<Path> isMatchingFile = path -> Files.isRegularFile(path)
              && pathMatcher.match(resourcePattern, StringUtils.cleanPath(path.toString()));

      if (log.isDebugEnabled()) {
        log.trace("Searching directory [{}] for files matching pattern [{}]",
                rootPath.toAbsolutePath(), subPattern);
      }

      try (Stream<Path> files = Files.walk(rootPath)) {
        files.filter(isMatchingFile).sorted().forEach(file -> {
          try {
            consumer.accept(new FileSystemResource(file));
          }
          catch (Exception e) {
            throw ExceptionUtils.sneakyThrow(e);
          }
        });
      }
      catch (Exception ex) {
        log.debug("Failed to complete search in directory [{}] for files matching pattern [{}]: {}", rootPath.toAbsolutePath(), subPattern, ex);
        throw ex;
      }
    }
    finally {
      if (fileSystem != null) {
        fileSystem.close();
      }
    }
  }

  /**
   * Resolve the given location pattern into {@code Resource} objects for all
   * matching resources found in the module path.
   * <p>The location pattern may be an explicit resource path such as
   * {@code "com/example/config.xml"} or a pattern such as
   * <code>"com/example/**&#47;config-*.xml"</code> to be matched using the
   * configured {@link #getPathMatcher() PathMatcher}.
   * <p>The default implementation scans all modules in the {@linkplain ModuleLayer#boot()
   * boot layer}, excluding {@linkplain ModuleFinder#ofSystem() system modules}.
   *
   * @param locationPattern the location pattern to resolve
   * @param consumer consume the corresponding {@code Resource}
   * @throws IOException in case of I/O errors
   * @see ModuleLayer#boot()
   * @see ModuleFinder#ofSystem()
   * @see ModuleReader
   * @see PathMatcher#match(String, String)
   * @since 4.0
   */
  protected void findAllModulePathResources(String locationPattern, ResourceConsumer consumer) throws IOException {
    LinkedHashSet<Resource> result = null;
    if (log.isDebugEnabled()) {
      result = new LinkedHashSet<>(16);
      consumer = consumer.andThen(result::add);
    }

    String resourcePattern = stripLeadingSlash(locationPattern);
    PathMatcher pathMatcher = getPathMatcher();
    boolean pattern = pathMatcher.isPattern(resourcePattern);
    var moduleIterator = ModuleLayer.boot().configuration()
            .modules().stream().filter(isNotSystemModule).iterator();
    while (moduleIterator.hasNext()) {
      ResolvedModule resolvedModule = moduleIterator.next();
      // NOTE: a ModuleReader and a Stream returned from ModuleReader.list() must be closed.
      try (ModuleReader moduleReader = resolvedModule.reference().open();
              Stream<String> names = moduleReader.list()) {
        Iterator<String> iterator = names.iterator();
        while (iterator.hasNext()) {
          String name = iterator.next();
          if (pattern) {
            if (pathMatcher.match(resourcePattern, name)) {
              acceptResource(moduleReader, name, consumer);
            }
          }
          else if (resourcePattern.equals(name)) {
            acceptResource(moduleReader, name, consumer);
          }
        }
      }
      catch (IOException ex) {
        log.debug("Failed to read contents of module [{}]", resolvedModule, ex);
        throw ex;
      }
    }

    if (log.isDebugEnabled()) {
      log.trace("Resolved module-path location pattern [{}] to resources {}", resourcePattern, result);
    }
  }

  private static void acceptResource(
          ModuleReader moduleReader, String name, ResourceConsumer consumer) throws IOException {
    Resource resource;
    try {
      Optional<URI> uriOptional = moduleReader.find(name);
      if (uriOptional.isPresent()) {
        // If it's a "file:" URI, use FileSystemResource to avoid duplicates
        // for the same path discovered via class-path scanning.
        URI uri = uriOptional.get();
        resource = ResourceUtils.URL_PROTOCOL_FILE.equals(uri.getScheme())
                   ? new FileSystemResource(uri.getPath())
                   : UrlResource.from(uri);
      }
      else {
        return;
      }
    }
    catch (Exception ex) {
      log.debug("Failed to find resource [{}] in module path", name, ex);
      return;
    }
    consumer.accept(resource);
  }

  private static String stripLeadingSlash(String path) {
    return path.startsWith("/") ? path.substring(1) : path;
  }

}
