/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.context.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import cn.taketoday.context.AntPathMatcher;
import cn.taketoday.context.Constant;
import cn.taketoday.context.PathMatcher;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * A {@link ResourceResolver} implementation that is able to resolve a specified
 * resource location path into one or more matching Resources. The source path
 * may be a simple path which has a one-to-one mapping to a target
 * {@link core.io.Resource}, or alternatively may contain the special
 * "{@code classpath*:}" prefix and/or internal Ant-style regular expressions
 * (matched using Spring's {@link AntPathMatcher} utility). Both of the latter
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
 * @since 2.1.7
 * @see AntPathMatcher
 * @see ClassLoader#getResources(String)
 * @author TODAY <br>
 *         2019-12-05 12:51
 */
public class PathMatchingResourcePatternResolver implements ResourceResolver {

    private static final Logger log = LoggerFactory.getLogger(PathMatchingResourcePatternResolver.class);

    private final ClassLoader classLoader;

    private PathMatcher pathMatcher = new AntPathMatcher();

    public PathMatchingResourcePatternResolver() {
        this(ClassUtils.getClassLoader());
    }

    public PathMatchingResourcePatternResolver(ClassLoader classLoader) {
        this.classLoader = classLoader == null ? ClassUtils.getClassLoader() : classLoader;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Set the PathMatcher implementation to use for this resource pattern resolver.
     * Default is AntPathMatcher.
     * 
     * @see AntPathMatcher
     */
    public void setPathMatcher(PathMatcher pathMatcher) {
        Objects.requireNonNull(pathMatcher, "PathMatcher must not be null");
        this.pathMatcher = pathMatcher;
    }

    /**
     * Return the PathMatcher that this resource pattern resolver uses.
     */
    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }

    @Override
    public Resource getResource(String location) {
        return ResourceUtils.getResource(location);
    }

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        Objects.requireNonNull(locationPattern, "Location pattern must not be null");
        if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
            // a class path resource (multiple resources for same name possible)
            if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
                // a class path resource pattern
                return findPathMatchingResources(locationPattern);
            }
            else {
                // all class path resources with the given name
                return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
            }
        }
        else {
            // Generally only look for a pattern after a prefix here,
            // and on Tomcat only after the "*/" separator for its "war:" protocol.
            final int prefixEnd = locationPattern.startsWith("war:")
                    ? locationPattern.indexOf("*/")
                    : locationPattern.indexOf(':');

            if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd + 1))) {
                return findPathMatchingResources(locationPattern); // a file pattern
            }
            else {
                // a single resource with the given name
                return new Resource[] { getResource(locationPattern) };
            }
        }
    }

    /**
     * Find all class location resources with the given location via the
     * ClassLoader. Delegates to {@link #doFindAllClassPathResources(String)}.
     * 
     * @param location
     *            the absolute path within the classpath
     * @return the result as Resource array
     * @throws IOException
     *             in case of I/O errors
     * @see java.lang.ClassLoader#getResources
     * @see #convertClassLoaderURL
     */
    protected Resource[] findAllClassPathResources(String location) throws IOException {
        String path = location;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        Set<Resource> result = doFindAllClassPathResources(path);
        if (log.isTraceEnabled()) {
            log.trace("Resolved classpath location [{}] to resources {}", location, result);
        }
        return result.toArray(new Resource[result.size()]);
    }

    /**
     * Find all class location resources with the given path via the ClassLoader.
     * Called by {@link #findAllClassPathResources(String)}.
     * 
     * @param path
     *            the absolute path within the classpath (never a leading slash)
     * @return a mutable Set of matching Resource instances
     */
    protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
        Set<Resource> result = new LinkedHashSet<>(16);
        final ClassLoader cl = getClassLoader();
        final Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
        while (resourceUrls.hasMoreElements()) {
            final URL url = resourceUrls.nextElement();
            result.add(convertClassLoaderURL(url));
        }
        if (Constant.BLANK.equals(path)) {
            // The above result is likely to be incomplete, i.e. only containing file system references.
            // We need to have pointers to each of the jar files on the classpath as well...
            addAllClassLoaderJarRoots(cl, result);
        }
        return result;
    }

    /**
     * Convert the given URL as returned from the ClassLoader into a
     * {@link Resource}.
     * <p>
     * The default implementation simply creates a {@link UrlResource} instance.
     * 
     * @param url
     *            a URL as returned from the ClassLoader
     * @return the corresponding Resource object
     * @see java.lang.ClassLoader#getResources
     * @see Resource
     */
    protected Resource convertClassLoaderURL(URL url) {
        return new UrlBasedResource(url);
    }

    /**
     * Search all {@link URLClassLoader} URLs for jar file references and add them
     * to the given set of resources in the form of pointers to the root of the jar
     * file content.
     * 
     * @param classLoader
     *            the ClassLoader to search (including its ancestors)
     * @param result
     *            the set of resources to add jar roots to
     */
    protected void addAllClassLoaderJarRoots(ClassLoader classLoader, Set<Resource> result) {
        if (classLoader instanceof URLClassLoader) {
            try {
                for (URL url : ((URLClassLoader) classLoader).getURLs()) {
                    try {
                        final UrlBasedResource jarResource = Constant.URL_PROTOCOL_JAR.equals(url.getProtocol())
                                ? new UrlBasedResource(url)
                                : new UrlBasedResource(new StringBuilder(Constant.JAR_URL_PREFIX)//@off
                                                            .append(url)
                                                            .append(Constant.JAR_URL_SEPARATOR).toString()); //@on
                        if (jarResource.exists()) {
                            result.add(jarResource);
                        }
                    }
                    catch (MalformedURLException ex) {
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
            addClassPathManifestEntries(result);
        }

        if (classLoader != null) {
            try {
                // Hierarchy traversal...
                addAllClassLoaderJarRoots(classLoader.getParent(), result);
            }
            catch (Exception ex) {
                log.debug("Cannot introspect jar files in parent ClassLoader since [{}] does not support 'getParent()': {}",
                          classLoader, ex);
            }
        }
    }

    /**
     * Determine jar file references from the "java.class.path." manifest property
     * and add them to the given set of resources in the form of pointers to the
     * root of the jar file content.
     * 
     * @param result
     *            the set of resources to add jar roots to
     */
    protected void addClassPathManifestEntries(Set<Resource> result) {
        try {
            final String javaClassPathProperty = System.getProperty("java.class.path");
            for (final String path : StringUtils.delimitedListToStringArray(javaClassPathProperty, System.getProperty("path.separator"))) {
                try {

                    String filePath = new File(path).getAbsolutePath();
                    int prefixIndex = filePath.indexOf(':');
                    if (prefixIndex == 1) {
                        // Possibly "c:" drive prefix on Windows, to be upper-cased for proper duplicate detection
                        filePath = StringUtils.capitalize(filePath);
                    }
                    final String url = new StringBuilder()
                            .append(Constant.JAR_URL_PREFIX)
                            .append(Constant.FILE_URL_PREFIX)
                            .append(filePath)
                            .append(Constant.JAR_URL_SEPARATOR).toString();

                    UrlBasedResource jarResource = new UrlBasedResource(url);
                    // Potentially overlapping with URLClassLoader.getURLs() result above!
                    if (!result.contains(jarResource) && !hasDuplicate(filePath, result) && jarResource.exists()) {
                        result.add(jarResource);
                    }
                }
                catch (MalformedURLException ex) {
                    log.debug("Cannot search for matching files underneath [{}] because it cannot be converted to a valid 'jar:' URL: {}",
                              path, ex.getMessage());
                }
            }
        }
        catch (Exception ex) {
            log.debug("Failed to evaluate 'java.class.path' manifest entries: " + ex);
        }
    }

    /**
     * Check whether the given file path has a duplicate but differently structured
     * entry in the existing result, i.e. with or without a leading slash.
     * 
     * @param filePath
     *            the file path (with or without a leading slash)
     * @param result
     *            the current result
     * @return {@code true} if there is a duplicate (i.e. to ignore the given file
     *         path), {@code false} to proceed with adding a corresponding resource
     *         to the current result
     */
    private boolean hasDuplicate(final String filePath, final Set<Resource> result) {
        if (result.isEmpty()) {
            return false;
        }
        final String duplicatePath = StringUtils.checkUrl(filePath);
        try {
            return result.contains(new UrlBasedResource(new StringBuilder(Constant.JAR_URL_PREFIX) //@off
                                                                .append(Constant.FILE_URL_PREFIX)
                                                                .append(duplicatePath)
                                                                .append(Constant.JAR_URL_SEPARATOR).toString())); //@on
        }
        catch (MalformedURLException ex) {
            return false; // Ignore: just for testing against duplicate.
        }
    }

    /**
     * Find all resources that match the given location pattern via the Ant-style
     * PathMatcher. Supports resources in jar files and zip files and in the file
     * system.
     * 
     * @param locationPattern
     *            the location pattern to match
     * @return the result as Resource array
     * @throws IOException
     *             in case of I/O errors
     * @see #doFindPathMatchingJarResources
     * @see #doFindPathMatchingFileResources
     * @see PathMatcher
     */
    protected Resource[] findPathMatchingResources(final String locationPattern) throws IOException {

        final String rootDirPath = determineRootDir(locationPattern);
        final String subPattern = locationPattern.substring(rootDirPath.length());
        final Resource[] rootDirResources = getResources(rootDirPath);
        final Set<Resource> result = new LinkedHashSet<>(16);

        for (final Resource rootDirResource : rootDirResources) {
            if (rootDirResource instanceof JarResource) {
                result.addAll(doFindPathMatchingJarResources((JarResource) rootDirResource, subPattern));
            }
            if (rootDirResource instanceof ClassPathResource) {
                final Resource originalResource = ((ClassPathResource) rootDirResource).getOriginalResource();
                if (originalResource instanceof JarResource) {
                    result.addAll(doFindPathMatchingJarResources((JarResource) originalResource, subPattern));
                }
                else if (originalResource instanceof FileBasedResource) {
                    result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
                }
            }
            else {
                result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("Resolved location pattern [{}] to resources {}", locationPattern, result);
        }
        return result.toArray(new Resource[result.size()]);
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
     * @param location
     *            the location to check
     * @return the part of the location that denotes the root directory
     * @see #retrieveMatchingFiles
     */
    protected String determineRootDir(final String location) {
        final int prefixEnd = location.indexOf(':') + 1;
        int rootDirEnd = location.length();
        while (rootDirEnd > prefixEnd && getPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
            rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
        }
        if (rootDirEnd == 0) {
            rootDirEnd = prefixEnd;
        }
        return location.substring(0, rootDirEnd);
    }

    /**
     * Return whether the given resource handle indicates a jar resource that the
     * {@code doFindPathMatchingJarResources} method can handle.
     * <p>
     * By default, the URL protocols "jar", "zip", "vfszip and "wsjar" will be
     * treated as jar resources. This template method allows for detecting further
     * kinds of jar-like resources, e.g. through {@code instanceof} checks on the
     * resource handle type.
     * 
     * @param resource
     *            the resource handle to check (usually the root directory to start
     *            path matching from)
     * @see #doFindPathMatchingJarResources
     * @see ResourceUtils#isJarURL
     */
    protected boolean isJarResource(Resource resource) throws IOException {
        return false;
    }

    /**
     * Find all resources in jar files that match the given location pattern via the
     * Ant-style PathMatcher.
     * 
     * @param rootDirResource
     *            the root directory as Resource
     * @param rootDirURL
     *            the pre-resolved root directory URL
     * @param subPattern
     *            the sub pattern to match (below the root directory)
     * @return a mutable Set of matching Resource instances
     * @throws IOException
     *             in case of I/O errors
     * @see java.net.JarURLConnection
     * @see PathMatcher
     */
    protected Set<Resource> doFindPathMatchingJarResources(final JarResource rootDirResource,
                                                           final String subPattern) throws IOException {

        final URL rootDirURL = rootDirResource.getLocation();

        // Should usually be the case for traditional JAR files.
        JarURLConnection jarCon = (JarURLConnection) rootDirURL.openConnection();
        ResourceUtils.useCachesIfNecessary(jarCon);
        final JarEntry jarEntry = jarCon.getJarEntry();
        String rootEntryPath = (jarEntry != null ? jarEntry.getName() : Constant.BLANK);
        final boolean closeJarFile = !jarCon.getUseCaches();

        final JarFile jarFile = rootDirResource.getJarFile();

        try {
            if (log.isTraceEnabled()) {
                final String jarFileUrl = jarCon.getJarFileURL().toExternalForm();
                log.trace("Looking for matching resources in jar file [{}]", jarFileUrl);
            }
            if (!Constant.BLANK.equals(rootEntryPath) && !rootEntryPath.endsWith("/")) {
                // Root entry path must end with slash to allow for proper matching.
                // The Sun JRE does not return a slash here, but BEA JRockit does.
                rootEntryPath = rootEntryPath.concat("/");
            }

            final PathMatcher pathMatcher = getPathMatcher();

            final Set<Resource> result = new LinkedHashSet<>(8);
            final Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                final String entryPath = entry.getName();
                if (entryPath.startsWith(rootEntryPath)) {
                    final String relativePath = entryPath.substring(rootEntryPath.length());
                    if (pathMatcher.match(subPattern, relativePath)) {
                        result.add(rootDirResource.createRelative(relativePath));
                    }
                }
            }
            return result;
        }
        finally {
            if (closeJarFile) {
                jarFile.close();
            }
        }
    }

    /**
     * Find all resources in the file system that match the given location pattern
     * via the Ant-style PathMatcher.
     * 
     * @param rootDirResource
     *            the root directory as Resource
     * @param subPattern
     *            the sub pattern to match (below the root directory)
     * @return a mutable Set of matching Resource instances
     * @throws IOException
     *             in case of I/O errors
     * @see #retrieveMatchingFiles
     * @see PathMatcher
     */
    protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern) throws IOException {

        try {
            final File rootDir = rootDirResource.getFile().getAbsoluteFile();
            return doFindMatchingFileSystemResources(rootDir, subPattern);
        }
        catch (FileNotFoundException ex) {
            log.error("Cannot search for matching files underneath {} in the file system: {}",
                      rootDirResource, ex.toString(), ex);
            return Collections.emptySet();
        }
        catch (Exception ex) {
            log.error("Failed to resolve {} in the file system: {}", rootDirResource, ex);
            return Collections.emptySet();
        }
    }

    /**
     * Find all resources in the file system that match the given location pattern
     * via the Ant-style PathMatcher.
     * 
     * @param rootDir
     *            the root directory in the file system
     * @param subPattern
     *            the sub pattern to match (below the root directory)
     * @return a mutable Set of matching Resource instances
     * @throws IOException
     *             in case of I/O errors
     * @see #retrieveMatchingFiles
     * @see PathMatcher
     */
    protected Set<Resource> doFindMatchingFileSystemResources(File rootDir, String subPattern) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("Looking for matching resources in directory tree [{}]", rootDir.getPath());
        }
        final Set<File> matchingFiles = retrieveMatchingFiles(rootDir, subPattern);
        final Set<Resource> result = new LinkedHashSet<>(matchingFiles.size());
        for (final File file : matchingFiles) {
            result.add(new FileBasedResource(file));
        }
        return result;
    }

    /**
     * Retrieve files that match the given path pattern, checking the given
     * directory and its subdirectories.
     * 
     * @param rootDir
     *            the directory to start from
     * @param pattern
     *            the pattern to match against, relative to the root directory
     * @return a mutable Set of matching Resource instances
     * @throws IOException
     *             if directory contents could not be retrieved
     */
    protected Set<File> retrieveMatchingFiles(final File rootDir, final String pattern) throws IOException {
        if (!rootDir.exists()) {
            // Silently skip non-existing directories.
            if (log.isDebugEnabled()) {
                log.debug("Skipping [{}] because it does not exist", rootDir.getAbsolutePath());
            }
            return Collections.emptySet();
        }
        if (!rootDir.isDirectory()) {
            // Complain louder if it exists but is no directory.
            if (log.isInfoEnabled()) {
                log.info("Skipping [{}] because it does not denote a directory", rootDir.getAbsolutePath());
            }
            return Collections.emptySet();
        }
        if (!rootDir.canRead()) {
            if (log.isInfoEnabled()) {
                log.info("Skipping search for matching files underneath directory [{}] because the application is not allowed to read the directory",
                         rootDir.getAbsolutePath());
            }
            return Collections.emptySet();
        }
        String fullPattern = StringUtils.replace(rootDir.getAbsolutePath(), File.separator, "/");

        fullPattern = StringUtils.checkUrl(fullPattern).concat(StringUtils.replace(pattern, File.separator, "/"));

        Set<File> result = new LinkedHashSet<>(8);
        doRetrieveMatchingFiles(fullPattern, rootDir, result);
        return result;
    }

    /**
     * Recursively retrieve files that match the given pattern, adding them to the
     * given result list.
     * 
     * @param fullPattern
     *            the pattern to match against, with prepended root directory path
     * @param dir
     *            the current directory
     * @param result
     *            the Set of matching File instances to add to
     * @throws IOException
     *             if directory contents could not be retrieved
     */
    protected void doRetrieveMatchingFiles(final String fullPattern, final File dir, final Set<File> result) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("Searching directory [{}] for files matching pattern [{}]", dir.getAbsolutePath(), fullPattern);
        }

        final PathMatcher pathMatcher = getPathMatcher();
        for (final File content : listDirectory(dir)) {
            final String currPath = StringUtils.replace(content.getAbsolutePath(), File.separator, "/");
            if (content.isDirectory() && pathMatcher.matchStart(fullPattern, currPath.concat("/"))) {
                if (content.canRead()) {
                    doRetrieveMatchingFiles(fullPattern, content, result);
                }
                else if (log.isDebugEnabled()) {
                    log.debug("Skipping subdirectory [{}] because the application is not allowed to read the directory",
                              dir.getAbsolutePath());
                }
            }
            if (pathMatcher.match(fullPattern, currPath)) {
                result.add(content);
            }
        }
    }

    /**
     * Determine a sorted list of files in the given directory.
     * 
     * @param dir
     *            the directory to introspect
     * @return the sorted list of files (by default in alphabetical order)
     * @see File#listFiles()
     */
    protected File[] listDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            if (log.isInfoEnabled()) {
                log.info("Could not retrieve contents of directory [{}]", dir.getAbsolutePath());
            }
            return new File[0];
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }

}
