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
package cn.taketoday.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileBasedResource;
import cn.taketoday.core.io.JarEntryResource;
import cn.taketoday.core.io.PathMatchingResourcePatternResolver;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.UrlBasedResource;

import static cn.taketoday.core.Constant.BLANK;
import static cn.taketoday.core.Constant.PATH_SEPARATOR;
import static cn.taketoday.core.io.ResourceResolver.CLASSPATH_URL_PREFIX;

/**
 * @author TODAY 2019-05-15 13:37
 * @since 2.1.6
 */
public abstract class ResourceUtils {

  public static final String JAR_ENTRY_URL_PREFIX = "jar:file:";
  public static final String JAR_SEPARATOR = "!/";
  public static final String PROTOCOL_JAR = "jar";
  public static final String PROTOCOL_FILE = "file";

  /** URL prefix for loading from the file system: "file:". */
  public static final String FILE_URL_PREFIX = "file:";
  /** URL prefix for loading from a jar file: "jar:". */
  public static final String JAR_URL_PREFIX = "jar:";
  /** URL prefix for loading from a war file on Tomcat: "war:". */
  public static final String WAR_URL_PREFIX = "war:";
  /** URL protocol for a file in the file system: "file". */
  public static final String URL_PROTOCOL_FILE = PROTOCOL_FILE;
  /** URL protocol for an entry from a jar file: "jar". */
  public static final String URL_PROTOCOL_JAR = PROTOCOL_JAR;
  /** URL protocol for an entry from a war file: "war". */
  public static final String URL_PROTOCOL_WAR = "war";
  /** URL protocol for an entry from a zip file: "zip". */
  public static final String URL_PROTOCOL_ZIP = "zip";
  /** URL protocol for an entry from a WebSphere jar file: "wsjar". */
  public static final String URL_PROTOCOL_WSJAR = "wsjar";
  /** URL protocol for an entry from a JBoss jar file: "vfszip". */
  public static final String URL_PROTOCOL_VFSZIP = "vfszip";
  /** URL protocol for a JBoss file system resource: "vfsfile". */
  public static final String URL_PROTOCOL_VFSFILE = "vfsfile";
  /** File extension for a regular jar file: ".jar". */
  public static final String JAR_FILE_EXTENSION = ".jar";
  /** Separator between JAR URL and file path within the JAR: "!/". */
  public static final String JAR_URL_SEPARATOR = JAR_SEPARATOR;
  /** Special separator between WAR URL and jar part on Tomcat. */
  public static final String WAR_URL_SEPARATOR = "*/";

  /**
   * Resolve the given location pattern into Resource objects.
   * <p>
   * Overlapping resource entries that point to the same physical resource should
   * be avoided, as far as possible. The result should have set semantics.
   *
   * @param pathPattern
   *         The location pattern to resolve
   *
   * @return the corresponding Resource objects
   *
   * @throws IOException
   *         in case of I/O errors
   */
  public static Resource[] getResources(String pathPattern) throws IOException {
    return getResources(pathPattern, null);
  }

  /**
   * Resolve the given location pattern into Resource objects.
   * <p>
   * Overlapping resource entries that point to the same physical resource should
   * be avoided, as far as possible. The result should have set semantics.
   *
   * @param pathPattern
   *         The location pattern to resolve
   * @param classLoader
   *         The {@link ClassLoader} to search (including its ancestors)
   *
   * @return the corresponding Resource objects
   *
   * @throws IOException
   *         in case of I/O errors
   */
  public static Resource[] getResources(String pathPattern, ClassLoader classLoader) throws IOException {
    return new PathMatchingResourcePatternResolver(classLoader).getResources(pathPattern);
  }

  /**
   * Get {@link Resource} with given location
   *
   * @param location
   *         resource location
   */
  public static Resource getResource(final String location) {

    // fix location is empty
    if (StringUtils.isEmpty(location)) {
      return new ClassPathResource(BLANK);
    }
    if (location.startsWith(CLASSPATH_URL_PREFIX)) {
      final String path = StringUtils.decodeUrl(location.substring(CLASSPATH_URL_PREFIX.length()));
      return new ClassPathResource(path.charAt(0) == PATH_SEPARATOR ? path.substring(1) : path);
    }
    try {
      return getResource(toURL(location));
    }
    catch (MalformedURLException e) {
      return new ClassPathResource(location);
    }
  }

  /**
   * Resolve the given resource location to a java.net.URL.
   * <p>
   * Does not check whether the URL actually exists; simply returns the URL that
   * the given location would correspond to.
   *
   * @param location
   *         Url location
   *
   * @throws MalformedURLException
   *         if no protocol is specified, or an unknown protocol is found, or
   *         {@code spec} is {@code null}.
   */
  public static URL toURL(String location) throws MalformedURLException {
    return new URL(location);
  }

  public static Resource getResource(final URL url) {
    final String protocol = url.getProtocol();
    if (PROTOCOL_FILE.equals(protocol)) {
      return new FileBasedResource(StringUtils.decodeUrl(url.getPath()));
    }
    if (PROTOCOL_JAR.equals(protocol)) {
      return new JarEntryResource(url);
    }
    return new UrlBasedResource(url);
  }

  /**
   * Get {@link Resource} from a file
   *
   * @param file
   *         source
   *
   * @return a {@link FileBasedResource}
   */
  public static Resource getResource(final File file) {
    return new FileBasedResource(file);
  }

  /**
   * Create a new relative path from a file path.
   * <p>
   * Note: When building relative path, it makes a difference whether the
   * specified resource base path here ends with a slash or not. In the case of
   * "C:/dir1/", relative paths will be built underneath that root: e.g. relative
   * path "dir2" -> "C:/dir1/dir2". In the case of "C:/dir1", relative paths will
   * apply at the same directory level: relative path "dir2" -> "C:/dir2".
   */
  public static String getRelativePath(final String path, final String relativePath) {
    final int separatorIndex = path.lastIndexOf(PATH_SEPARATOR);
    if (separatorIndex > 0) {
      final StringBuilder newPath = new StringBuilder(path.substring(0, separatorIndex));
      if (relativePath.charAt(0) != PATH_SEPARATOR) {
        newPath.append(PATH_SEPARATOR);
      }
      return newPath.append(relativePath).toString();
    }
    return relativePath;
  }

  // @since 2.1.7

  /**
   * Determine whether the given URL points to a resource in the file system, i.e.
   * has protocol "file", "vfsfile" or "vfs".
   *
   * @param url
   *         the URL to check
   *
   * @return whether the URL has been identified as a file system URL
   */
  public static boolean isFileURL(final URL url) {
    String protocol = url.getProtocol();
    return (URL_PROTOCOL_FILE.equals(protocol) || URL_PROTOCOL_VFSFILE.equals(protocol));
  }

  /**
   * Determine whether the given URL points to a resource in a jar file. i.e. has
   * protocol "jar", "war, ""zip", "vfszip" or "wsjar".
   *
   * @param url
   *         the URL to check
   *
   * @return whether the URL has been identified as a JAR URL
   */
  public static boolean isJarURL(final URL url) {
    final String protocol = url.getProtocol();
    return (URL_PROTOCOL_JAR.equals(protocol)
            || URL_PROTOCOL_WAR.equals(protocol)
            || URL_PROTOCOL_ZIP.equals(protocol)
            || URL_PROTOCOL_VFSZIP.equals(protocol)
            || URL_PROTOCOL_WSJAR.equals(protocol));
  }

  /**
   * Determine whether the given URL points to a jar file itself, that is, has
   * protocol "file" and ends with the ".jar" extension.
   *
   * @param url
   *         the URL to check
   *
   * @return whether the URL has been identified as a JAR file URL
   */
  public static boolean isJarFileURL(final URL url) {
    return (URL_PROTOCOL_FILE.equals(url.getProtocol()) &&
            url.getPath().toLowerCase().endsWith(JAR_FILE_EXTENSION));
  }

  /**
   * Extract the URL for the actual jar file from the given URL (which may point
   * to a resource in a jar file or to a jar file itself).
   *
   * @param jarUrl
   *         the original URL
   *
   * @return the URL for the actual jar file
   *
   * @throws MalformedURLException
   *         if no valid jar file URL could be extracted
   */
  public static URL extractJarFileURL(final URL jarUrl) throws MalformedURLException {
    String urlFile = jarUrl.getFile();
    int separatorIndex = urlFile.indexOf(JAR_URL_SEPARATOR);
    if (separatorIndex != -1) {
      String jarFile = urlFile.substring(0, separatorIndex);
      try {
        return new URL(jarFile);
      }
      catch (MalformedURLException ex) {
        // Probably no protocol in original jar URL, like "jar:C:/mypath/myjar.jar".
        // This usually indicates that the jar file resides in the file system.
        if (!jarFile.startsWith("/")) {
          jarFile = '/' + jarFile;
        }
        return new URL(FILE_URL_PREFIX.concat(jarFile));
      }
    }
    else {
      return jarUrl;
    }
  }

  /**
   * Set the {@link URLConnection#setUseCaches "useCaches"} flag on the given
   * connection, preferring {@code false} but leaving the flag at {@code true} for
   * JNLP based resources.
   *
   * @param con
   *         the URLConnection to set the flag on
   */
  public static void useCachesIfNecessary(final URLConnection con) {
    con.setUseCaches(con.getClass().getSimpleName().startsWith("JNLP"));
  }

  /**
   * Create a URI instance for the given URL, replacing spaces with "%20" URI
   * encoding first.
   *
   * @param url
   *         the URL to convert into a URI instance
   *
   * @return the URI instance
   *
   * @throws URISyntaxException
   *         if the URL wasn't a valid URI
   * @see java.net.URL#toURI()
   */
  public static URI toURI(final URL url) throws URISyntaxException {
    return toURI(url.toString());
  }

  /**
   * Create a URI instance for the given location String, replacing spaces with
   * "%20" URI encoding first.
   *
   * @param location
   *         the location String to convert into a URI instance
   *
   * @return the URI instance
   *
   * @throws URISyntaxException
   *         if the location wasn't a valid URI
   */
  public static URI toURI(final String location) throws URISyntaxException {
    return new URI(StringUtils.replace(location, " ", "%20"));
  }

  /**
   * Return whether the given resource location is a URL: either a special
   * "classpath" pseudo URL or a standard URL.
   *
   * @param resourceLocation
   *         the location String to check
   *
   * @return whether the location qualifies as a URL
   *
   * @see cn.taketoday.core.io.ResourceResolver#CLASSPATH_URL_PREFIX
   * @see java.net.URL
   */
  public static boolean isUrl(String resourceLocation) {
    if (resourceLocation == null) {
      return false;
    }
    if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
      return true;
    }
    try {
      new URL(resourceLocation);
      return true;
    }
    catch (MalformedURLException ex) {
      return false;
    }
  }

  /**
   * Extract the URL for the outermost archive from the given jar/war URL (which
   * may point to a resource in a jar file or to a jar file itself).
   * <p>
   * In the case of a jar file nested within a war file, this will return a URL to
   * the war file since that is the one resolvable in the file system.
   *
   * @param jarUrl
   *         the original URL
   *
   * @return the URL for the actual jar file
   *
   * @throws MalformedURLException
   *         if no valid jar file URL could be extracted
   * @see #extractJarFileURL(URL)
   */
  public static URL extractArchiveURL(URL jarUrl) throws MalformedURLException {
    String urlFile = jarUrl.getFile();

    int endIndex = urlFile.indexOf(WAR_URL_SEPARATOR);
    if (endIndex != -1) {
      // Tomcat's "war:file:...mywar.war*/WEB-INF/lib/myjar.jar!/myentry.txt"
      String warFile = urlFile.substring(0, endIndex);
      if (URL_PROTOCOL_WAR.equals(jarUrl.getProtocol())) {
        return new URL(warFile);
      }
      int startIndex = warFile.indexOf(WAR_URL_PREFIX);
      if (startIndex != -1) {
        return new URL(warFile.substring(startIndex + WAR_URL_PREFIX.length()));
      }
    }

    // Regular "jar:file:...myjar.jar!/myentry.txt"
    return extractJarFileURL(jarUrl);
  }
}
