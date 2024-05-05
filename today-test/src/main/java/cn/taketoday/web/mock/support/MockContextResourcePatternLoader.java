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

package cn.taketoday.web.mock.support;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import cn.taketoday.core.PathMatcher;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceConsumer;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.io.UrlResource;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * MockContext-aware subclass of {@link PathMatchingPatternResourceLoader},
 * able to find matching resources below the web application root directory
 * via {@link MockContext#getResourcePaths}. Falls back to the superclass'
 * file system checking for other resources.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/21 16:35
 */
public class MockContextResourcePatternLoader extends PathMatchingPatternResourceLoader {
  private static final Logger logger = LoggerFactory.getLogger(MockContextResourcePatternLoader.class);

  /**
   * Create a new MockContextPatternResourceLoader.
   *
   * @param mockContext the MockContext to load resources with
   * @see MockContextResourceLoader#MockContextResourceLoader(MockContext)
   */
  public MockContextResourcePatternLoader(MockContext mockContext) {
    super(new MockContextResourceLoader(mockContext));
  }

  /**
   * Create a new MockContextPatternResourceLoader.
   *
   * @param resourceLoader the ResourceLoader to load root directories and
   * actual resources with
   */
  public MockContextResourcePatternLoader(ResourceLoader resourceLoader) {
    super(resourceLoader);
  }

  /**
   * Overridden version which checks for MockContextResource
   * and uses {@code MockContext.getResourcePaths} to find
   * matching resources below the web application root directory.
   * In case of other resources, delegates to the superclass version.
   *
   * @see #doRetrieveMatchingMockContextResources
   * @see MockContextResource
   * @see MockContext#getResourcePaths
   */
  @Override
  protected void doFindPathMatchingFileResources(
          Resource rootDirResource, String subPattern, ResourceConsumer consumer) throws IOException {
    if (rootDirResource instanceof MockContextResource scResource) {
      MockContext sc = scResource.getMockContext();
      String fullPattern = scResource.getPath() + subPattern;
      doRetrieveMatchingMockContextResources(sc, fullPattern, scResource.getPath(), consumer);
    }
    else {
      super.doFindPathMatchingFileResources(rootDirResource, subPattern, consumer);
    }
  }

  /**
   * Recursively retrieve MockContextResources that match the given pattern,
   * adding them to the given result set.
   *
   * @param mockContext the MockContext to work on
   * @param fullPattern the pattern to match against,
   * with preprended root directory path
   * @param dir the current directory
   * @param consumer Resource how to use
   * @throws IOException if directory contents could not be retrieved
   * @see MockContextResource
   * @see MockContext#getResourcePaths
   */
  protected void doRetrieveMatchingMockContextResources(
          MockContext mockContext, String fullPattern, String dir, ResourceConsumer consumer)
          throws IOException {

    Set<String> candidates = mockContext.getResourcePaths(dir);
    if (CollectionUtils.isNotEmpty(candidates)) {
      boolean dirDepthNotFixed = fullPattern.contains("**");
      int jarFileSep = fullPattern.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
      String jarFilePath = null;
      String pathInJarFile = null;
      if (jarFileSep > 0 && jarFileSep + ResourceUtils.JAR_URL_SEPARATOR.length() < fullPattern.length()) {
        jarFilePath = fullPattern.substring(0, jarFileSep);
        pathInJarFile = fullPattern.substring(jarFileSep + ResourceUtils.JAR_URL_SEPARATOR.length());
      }

      PathMatcher pathMatcher = getPathMatcher();
      for (String currPath : candidates) {
        if (!currPath.startsWith(dir)) {
          // Returned resource path does not start with relative directory:
          // assuming absolute path returned -> strip absolute path.
          int dirIndex = currPath.indexOf(dir);
          if (dirIndex != -1) {
            currPath = currPath.substring(dirIndex);
          }
        }
        if (currPath.endsWith("/") && (dirDepthNotFixed
                || StringUtils.countOccurrencesOf(currPath, "/") <= StringUtils.countOccurrencesOf(fullPattern, "/"))) {
          // Search subdirectories recursively: MockContext.getResourcePaths
          // only returns entries for one directory level.
          doRetrieveMatchingMockContextResources(mockContext, fullPattern, currPath, consumer);
        }
        if (jarFilePath != null && pathMatcher.match(jarFilePath, currPath)) {
          // Base pattern matches a jar file - search for matching entries within.
          String absoluteJarPath = mockContext.getRealPath(currPath);
          if (absoluteJarPath != null) {
            doRetrieveMatchingJarEntries(absoluteJarPath, pathInJarFile, consumer);
          }
        }
        if (pathMatcher.match(fullPattern, currPath)) {
          consumer.accept(new MockContextResource(mockContext, currPath));
        }
      }
    }
  }

  /**
   * Extract entries from the given jar by pattern.
   *
   * @param jarFilePath the path to the jar file
   * @param entryPattern the pattern for jar entries to match
   * @param consumer Resource how to use
   */
  private void doRetrieveMatchingJarEntries(String jarFilePath, String entryPattern, ResourceConsumer consumer) {
    if (logger.isDebugEnabled()) {
      logger.debug("Searching jar file [{}] for entries matching [{}]", jarFilePath, entryPattern);
    }
    try (JarFile jarFile = new JarFile(jarFilePath)) {
      PathMatcher pathMatcher = getPathMatcher();
      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String entryPath = entry.getName();
        if (pathMatcher.match(entryPattern, entryPath)) {
          consumer.accept(new UrlResource(
                  ResourceUtils.URL_PROTOCOL_JAR,
                  ResourceUtils.FILE_URL_PREFIX + jarFilePath + ResourceUtils.JAR_URL_SEPARATOR + entryPath));
        }
      }
    }
    catch (IOException ex) {
      if (logger.isWarnEnabled()) {
        logger.warn("Cannot search for matching resources in jar file [{}] because the jar cannot be opened through the file system",
                jarFilePath, ex);
      }
    }
  }

}
