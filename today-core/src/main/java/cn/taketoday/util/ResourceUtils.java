/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.io.UrlResource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.lang.Constant.BLANK;
import static cn.taketoday.lang.Constant.PATH_SEPARATOR;

/**
 * @author TODAY 2019-05-15 13:37
 * @since 2.1.6
 */
public abstract class ResourceUtils {
  /** Pseudo URL prefix for loading from the class path: "classpath:". */
  public static final String CLASSPATH_URL_PREFIX = ResourceLoader.CLASSPATH_URL_PREFIX;

  public static final String JAR_ENTRY_URL_PREFIX = "jar:file:";
  public static final String JAR_SEPARATOR = "!/";

  /** URL prefix for loading from the file system: "file:". */
  public static final String FILE_URL_PREFIX = "file:";
  /** URL prefix for loading from a jar file: "jar:". */
  public static final String JAR_URL_PREFIX = "jar:";
  /** URL prefix for loading from a war file on Tomcat: "war:". */
  public static final String WAR_URL_PREFIX = "war:";
  /** URL protocol for a file in the file system: "file". */
  public static final String URL_PROTOCOL_FILE = "file";
  /** URL protocol for an entry from a jar file: "jar". */
  public static final String URL_PROTOCOL_JAR = "jar";
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
   * @param pathPattern The location pattern to resolve
   * @return the corresponding Resource objects
   * @throws IOException in case of I/O errors
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
   * @param pathPattern The location pattern to resolve
   * @param classLoader The {@link ClassLoader} to search (including its ancestors)
   * @return the corresponding Resource objects
   * @throws IOException in case of I/O errors
   */
  public static Resource[] getResources(String pathPattern, @Nullable ClassLoader classLoader) throws IOException {
    return new PathMatchingPatternResourceLoader(classLoader).getResourcesArray(pathPattern);
  }

  /**
   * Get {@link Resource} with given location
   *
   * @param location resource location
   */
  public static Resource getResource(String location) {

    // fix location is empty
    if (StringUtils.isEmpty(location)) {
      return new ClassPathResource(BLANK);
    }
    if (location.startsWith(CLASSPATH_URL_PREFIX)) {
      String path = URLDecoder.decode(
              location.substring(CLASSPATH_URL_PREFIX.length()), StandardCharsets.UTF_8);
      return new ClassPathResource(path.charAt(0) == PATH_SEPARATOR ? path.substring(1) : path);
    }
    try {
      return getResource(new URL(location));
    }
    catch (MalformedURLException e) {
      return new ClassPathResource(location);
    }
  }

  public static Resource getResource(URL url) {
    if (ResourceUtils.URL_PROTOCOL_FILE.equals(url.getProtocol())) {
      try {
        // URI decoding for special characters such as spaces.
        return new FileSystemResource(ResourceUtils.toURI(url).getSchemeSpecificPart());
      }
      catch (URISyntaxException ex) {
        // Fallback for URLs that are not valid URIs (should hardly ever happen).
        return new FileSystemResource(url.getFile());
      }
    }
    else {
      return new UrlResource(url);
    }
  }

  /**
   * Get {@link Resource} from a file
   *
   * @param file source
   * @return a {@link FileSystemResource}
   */
  public static Resource getResource(File file) {
    return new FileSystemResource(file);
  }

  // @since 2.1.7

  /**
   * Determine whether the given URL points to a resource in the file system, i.e.
   * has protocol "file".
   *
   * @param url the URL to check
   * @return whether the URL has been identified as a file system URL
   */
  public static boolean isFileURL(URL url) {
    return URL_PROTOCOL_FILE.equals(url.getProtocol());
  }

  /**
   * Determine whether the given URL points to a resource in a jar file. i.e. has
   * protocol "jar", "war, ""zip", "vfszip" or "wsjar".
   *
   * @param url the URL to check
   * @return whether the URL has been identified as a JAR URL
   */
  public static boolean isJarURL(URL url) {
    String protocol = url.getProtocol();
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
   * @param url the URL to check
   * @return whether the URL has been identified as a JAR file URL
   */
  public static boolean isJarFileURL(URL url) {
    return URL_PROTOCOL_FILE.equals(url.getProtocol())
            && url.getPath().toLowerCase().endsWith(JAR_FILE_EXTENSION);
  }

  /**
   * Extract the URL for the actual jar file from the given URL (which may point
   * to a resource in a jar file or to a jar file itself).
   *
   * @param jarUrl the original URL
   * @return the URL for the actual jar file
   * @throws MalformedURLException if no valid jar file URL could be extracted
   */
  public static URL extractJarFileURL(URL jarUrl) throws MalformedURLException {
    String urlFile = jarUrl.getFile();
    int separatorIndex = urlFile.indexOf(JAR_URL_SEPARATOR);
    if (separatorIndex != -1) {
      String jarFile = urlFile.substring(0, separatorIndex);
      try {
        return toURL(jarFile);
      }
      catch (MalformedURLException ex) {
        // Probably no protocol in original jar URL, like "jar:C:/mypath/myjar.jar".
        // This usually indicates that the jar file resides in the file system.
        if (!jarFile.startsWith("/")) {
          jarFile = '/' + jarFile;
        }
        return toURL(FILE_URL_PREFIX.concat(jarFile));
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
   * @param con the URLConnection to set the flag on
   */
  public static void useCachesIfNecessary(URLConnection con) {
    con.setUseCaches(con.getClass().getSimpleName().startsWith("JNLP"));
  }

  /**
   * Create a URI instance for the given URL, replacing spaces with "%20" URI
   * encoding first.
   *
   * @param url the URL to convert into a URI instance
   * @return the URI instance
   * @throws URISyntaxException if the URL wasn't a valid URI
   * @see java.net.URL#toURI()
   */
  public static URI toURI(URL url) throws URISyntaxException {
    return toURI(url.toString());
  }

  /**
   * Create a URI instance for the given location String, replacing spaces with
   * "%20" URI encoding first.
   *
   * @param location the location String to convert into a URI instance
   * @return the URI instance
   * @throws URISyntaxException if the location wasn't a valid URI
   */
  public static URI toURI(String location) throws URISyntaxException {
    return new URI(StringUtils.replace(location, " ", "%20"));
  }

  /**
   * Return whether the given resource location is a URL: either a special
   * "classpath" pseudo URL or a standard URL.
   *
   * @param resourceLocation the location String to check
   * @return whether the location qualifies as a URL
   * @see ResourceLoader#CLASSPATH_URL_PREFIX
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
   * @param jarUrl the original URL
   * @return the URL for the actual jar file
   * @throws MalformedURLException if no valid jar file URL could be extracted
   * @see #extractJarFileURL(URL)
   */
  public static URL extractArchiveURL(URL jarUrl) throws MalformedURLException {
    String urlFile = jarUrl.getFile();

    int endIndex = urlFile.indexOf(WAR_URL_SEPARATOR);
    if (endIndex != -1) {
      // Tomcat's "war:file:...mywar.war*/WEB-INF/lib/myjar.jar!/myentry.txt"
      String warFile = urlFile.substring(0, endIndex);
      if (URL_PROTOCOL_WAR.equals(jarUrl.getProtocol())) {
        return toURL(warFile);
      }
      int startIndex = warFile.indexOf(WAR_URL_PREFIX);
      if (startIndex != -1) {
        return toURL(warFile.substring(startIndex + WAR_URL_PREFIX.length()));
      }
    }

    // Regular "jar:file:...myjar.jar!/myentry.txt"
    return extractJarFileURL(jarUrl);
  }

  /**
   * Get a {@link InputStream} from given resource string
   *
   * @param resourceLocation Target resource string
   * @return A {@link InputStream}
   * @throws IOException If any IO {@link Exception} occurred
   * @since 4.0
   */
  public static InputStream getResourceAsStream(String resourceLocation) throws IOException {
    Resource resource = getResource(resourceLocation);
    if (resource.exists()) {
      InputStream in = resource.getInputStream();
      if (in != null) {
        return in;
      }
    }
    throw new IOException("Could not find resource " + resourceLocation);
  }

  /**
   * Resolve the given resource location to a {@code java.net.URL}.
   * <p>Does not check whether the URL actually exists; simply returns
   * the URL that the given location would correspond to.
   *
   * @param resourceLocation the resource location to resolve: either a
   * "classpath:" pseudo URL, a "file:" URL, or a plain file path
   * @return a corresponding URL object
   * @throws FileNotFoundException if the resource cannot be resolved to a URL
   * @since 4.0
   */
  public static URL getURL(String resourceLocation) throws FileNotFoundException {
    Assert.notNull(resourceLocation, "Resource location must not be null");
    if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
      String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
      ClassLoader cl = ClassUtils.getDefaultClassLoader();
      URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
      if (url == null) {
        String description = "class path resource [" + path + "]";
        throw new FileNotFoundException(description +
                " cannot be resolved to URL because it does not exist");
      }
      return url;
    }
    try {
      // try URL
      return new URL(resourceLocation);
    }
    catch (MalformedURLException ex) {
      // no URL -> treat as file path
      try {
        return new File(resourceLocation).toURI().toURL();
      }
      catch (MalformedURLException ex2) {
        throw new FileNotFoundException("Resource location [" + resourceLocation +
                "] is neither a URL not a well-formed file path");
      }
    }
  }

  /**
   * Resolve the given resource location to a {@code java.io.File},
   * i.e. to a file in the file system.
   * <p>Does not check whether the file actually exists; simply returns
   * the File that the given location would correspond to.
   *
   * @param resourceLocation the resource location to resolve: either a
   * "classpath:" pseudo URL, a "file:" URL, or a plain file path
   * @return a corresponding File object
   * @throws FileNotFoundException if the resource cannot be resolved to
   * a file in the file system
   * @since 4.0
   */
  public static File getFile(String resourceLocation) throws FileNotFoundException {
    Assert.notNull(resourceLocation, "Resource location must not be null");
    if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
      String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
      String description = "class path resource [" + path + "]";
      ClassLoader cl = ClassUtils.getDefaultClassLoader();
      URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
      if (url == null) {
        throw new FileNotFoundException(description +
                " cannot be resolved to absolute file path because it does not exist");
      }
      return getFile(url, description);
    }
    try {
      // try URL
      return getFile(new URL(resourceLocation));
    }
    catch (MalformedURLException ex) {
      // no URL -> treat as file path
      return new File(resourceLocation);
    }
  }

  /**
   * Resolve the given resource URL to a {@code java.io.File},
   * i.e. to a file in the file system.
   *
   * @param resourceUrl the resource URL to resolve
   * @return a corresponding File object
   * @throws FileNotFoundException if the URL cannot be resolved to
   * a file in the file system
   * @since 4.0
   */
  public static File getFile(URL resourceUrl) throws FileNotFoundException {
    return getFile(resourceUrl, "URL");
  }

  /**
   * Resolve the given resource URL to a {@code java.io.File},
   * i.e. to a file in the file system.
   *
   * @param resourceUrl the resource URL to resolve
   * @param description a description of the original resource that
   * the URL was created for (for example, a class path location)
   * @return a corresponding File object
   * @throws FileNotFoundException if the URL cannot be resolved to
   * a file in the file system
   * @since 4.0
   */
  public static File getFile(URL resourceUrl, String description) throws FileNotFoundException {
    Assert.notNull(resourceUrl, "Resource URL must not be null");
    if (!URL_PROTOCOL_FILE.equals(resourceUrl.getProtocol())) {
      throw new FileNotFoundException(
              description + " cannot be resolved to absolute file path " +
                      "because it does not reside in the file system: " + resourceUrl);
    }
    try {
      return new File(toURI(resourceUrl).getSchemeSpecificPart());
    }
    catch (URISyntaxException ex) {
      // Fallback for URLs that are not valid URIs (should hardly ever happen).
      return new File(resourceUrl.getFile());
    }
  }

  /**
   * Resolve the given resource URI to a {@code java.io.File},
   * i.e. to a file in the file system.
   *
   * @param resourceUri the resource URI to resolve
   * @return a corresponding File object
   * @throws FileNotFoundException if the URL cannot be resolved to
   * a file in the file system
   * @since 4.0
   */
  public static File getFile(URI resourceUri) throws FileNotFoundException {
    return getFile(resourceUri, "URI");
  }

  /**
   * Resolve the given resource URI to a {@code java.io.File},
   * i.e. to a file in the file system.
   *
   * @param resourceUri the resource URI to resolve
   * @param description a description of the original resource that
   * the URI was created for (for example, a class path location)
   * @return a corresponding File object
   * @throws FileNotFoundException if the URL cannot be resolved to
   * a file in the file system
   * @since 4.0
   */
  public static File getFile(URI resourceUri, String description) throws FileNotFoundException {
    Assert.notNull(resourceUri, "Resource URI must not be null");
    if (!URL_PROTOCOL_FILE.equals(resourceUri.getScheme())) {
      throw new FileNotFoundException(
              description + " cannot be resolved to absolute file path " +
                      "because it does not reside in the file system: " + resourceUri);
    }
    return new File(resourceUri.getSchemeSpecificPart());
  }

  /**
   * Create a URL instance for the given location String,
   * going through URI construction and then URL conversion.
   *
   * @param location the location String to convert into a URL instance
   * @return the URL instance
   * @throws MalformedURLException if the location wasn't a valid URL
   * @since 4.0
   */
  public static URL toURL(String location) throws MalformedURLException {
    try {
      // Prefer URI construction with toURL conversion (as of 6.1)
      return toURI(StringUtils.cleanPath(location)).toURL();
    }
    catch (URISyntaxException | IllegalArgumentException ex) {
      // Lenient fallback to deprecated (on JDK 20) URL constructor,
      // e.g. for decoded location Strings with percent characters.
      return new URL(location);
    }
  }

  /**
   * Create a URL instance for the given root URL and relative path,
   * going through URI construction and then URL conversion.
   *
   * @param root the root URL to start from
   * @param relativePath the relative path to apply
   * @return the relative URL instance
   * @throws MalformedURLException if the end result is not a valid URL
   * @since 4.0
   */
  public static URL toRelativeURL(URL root, String relativePath) throws MalformedURLException {
    // # can appear in filenames, java.net.URL should not treat it as a fragment
    relativePath = StringUtils.replace(relativePath, "#", "%23");
    return toURL(StringUtils.applyRelativePath(root.toString(), relativePath));
  }

}
