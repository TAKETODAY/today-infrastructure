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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link Resource} implementation for {@code java.net.URL} locators.
 * Supports resolution as a {@code URL} and also as a {@code File} in
 * case of the {@code "file:"} protocol.
 *
 * @author TODAY
 * @since 2.1.6 2019-05-14 22:26
 */
public class UrlBasedResource extends AbstractFileResolvingResource {

  /**
   * Original URI, if available; used for URI and File access.
   */
  @Nullable
  private final URI uri;

  /**
   * Original URL, used for actual access.
   */
  private final URL url;

  /**
   * Cleaned URL (with normalized path), used for comparisons.
   */
  @Nullable
  private volatile URL cleanedUrl;

  /**
   * Create a new {@code UrlBasedResource} based on the given URI object.
   *
   * @param uri a URI
   * @throws MalformedURLException if the given URL path is not valid
   * @since 4.0
   */
  public UrlBasedResource(URI uri) throws MalformedURLException {
    Assert.notNull(uri, "URI must not be null");
    this.uri = uri;
    this.url = uri.toURL();
  }

  /**
   * Create a new {@code UrlBasedResource} based on the given URL object.
   *
   * @param url a URL
   */
  public UrlBasedResource(URL url) {
    Assert.notNull(url, "URL must not be null");
    this.uri = null;
    this.url = url;
  }

  /**
   * Create a new {@code UrlBasedResource} based on a URL path.
   * <p>Note: The given path needs to be pre-encoded if necessary.
   *
   * @param path a URL path
   * @throws MalformedURLException if the given URL path is not valid
   * @see java.net.URL#URL(String)
   * @since 4.0
   */
  public UrlBasedResource(String path) throws MalformedURLException {
    Assert.notNull(path, "Path must not be null");
    this.uri = null;
    this.url = new URL(path);
    this.cleanedUrl = getCleanedUrl(this.url, path);
  }

  /**
   * Create a new {@code UrlBasedResource} based on a URI specification.
   * <p>The given parts will automatically get encoded if necessary.
   *
   * @param protocol the URL protocol to use (e.g. "jar" or "file" - without colon);
   * also known as "scheme"
   * @param location the location (e.g. the file path within that protocol);
   * also known as "scheme-specific part"
   * @throws MalformedURLException if the given URL specification is not valid
   * @see java.net.URI#URI(String, String, String)
   * @since 4.0
   */
  public UrlBasedResource(String protocol, String location) throws MalformedURLException {
    this(protocol, location, null);
  }

  /**
   * Create a new {@code UrlBasedResource} based on a URI specification.
   * <p>The given parts will automatically get encoded if necessary.
   *
   * @param protocol the URL protocol to use (e.g. "jar" or "file" - without colon);
   * also known as "scheme"
   * @param location the location (e.g. the file path within that protocol);
   * also known as "scheme-specific part"
   * @param fragment the fragment within that location (e.g. anchor on an HTML page,
   * as following after a "#" separator)
   * @throws MalformedURLException if the given URL specification is not valid
   * @see java.net.URI#URI(String, String, String)
   * @since 4.0
   */
  public UrlBasedResource(String protocol, String location, @Nullable String fragment) throws MalformedURLException {
    try {
      this.uri = new URI(protocol, location, fragment);
      this.url = this.uri.toURL();
    }
    catch (URISyntaxException ex) {
      MalformedURLException exToThrow = new MalformedURLException(ex.getMessage());
      exToThrow.initCause(ex);
      throw exToThrow;
    }
  }

  /**
   * This implementation opens an InputStream for the given URL.
   * <p>
   * It sets the {@code useCaches} flag to {@code false}, mainly to avoid jar file
   * locking on Windows.
   *
   * @see java.net.URL#openConnection()
   * @see java.net.URLConnection#setUseCaches(boolean)
   * @see java.net.URLConnection#getInputStream()
   */
  @Override
  public InputStream getInputStream() throws IOException {
    URLConnection con = this.url.openConnection();
    ResourceUtils.useCachesIfNecessary(con);
    try {
      return con.getInputStream();
    }
    catch (IOException ex) {
      // Close the HTTP connection (if applicable).
      if (con instanceof HttpURLConnection) {
        ((HttpURLConnection) con).disconnect();
      }
      throw ex;
    }
  }

  /**
   * This implementation returns the underlying URL reference.
   */
  @Override
  public URL getLocation() {
    return url;
  }

  /**
   * This implementation returns the underlying URI directly,
   * if possible.
   */
  @Override
  public URI getURI() throws IOException {
    if (this.uri != null) {
      return this.uri;
    }
    else {
      return super.getURI();
    }
  }

  /**
   * This implementation returns a File reference for the underlying URL/URI,
   * provided that it refers to a file in the file system.
   *
   * @see ResourceUtils#getFile(java.net.URL, String)
   */
  @Override
  public File getFile() throws IOException {
    if (this.uri != null) {
      return ResourceUtils.getFile(uri, toString());
    }
    else {
      return ResourceUtils.getFile(url, toString());
    }
  }

  @Override
  public boolean isFile() {
    if (this.uri != null) {
      return super.isFile(this.uri);
    }
    else {
      return super.isFile();
    }
  }

  /**
   * Determine a cleaned URL for the given original URL.
   *
   * @param originalUrl the original URL
   * @param originalPath the original URL path
   * @return the cleaned URL (possibly the original URL as-is)
   * @see StringUtils#cleanPath
   * @since 4.0
   */
  private static URL getCleanedUrl(URL originalUrl, String originalPath) {
    String cleanedPath = StringUtils.cleanPath(originalPath);
    if (!cleanedPath.equals(originalPath)) {
      try {
        return new URL(cleanedPath);
      }
      catch (MalformedURLException ex) {
        // Cleaned URL path cannot be converted to URL -> take original URL.
      }
    }
    return originalUrl;
  }

  /**
   * Lazily determine a cleaned URL for the given original URL.
   *
   * @see #getCleanedUrl(URL, String)
   * @since 4.0
   */
  private URL getCleanedUrl() {
    URL cleanedUrl = this.cleanedUrl;
    if (cleanedUrl != null) {
      return cleanedUrl;
    }
    cleanedUrl = getCleanedUrl(this.url, (this.uri != null ? this.uri : this.url).toString());
    this.cleanedUrl = cleanedUrl;
    return cleanedUrl;
  }

  @Override
  public String getName() {
    return StringUtils.getFilename(getCleanedUrl().getPath());
  }

  /**
   * This implementation compares the underlying URL references.
   */
  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof UrlBasedResource
            && getCleanedUrl().equals(((UrlBasedResource) other).getCleanedUrl())));
  }

  /**
   * This implementation returns the hash code of the underlying URL reference.
   */
  @Override
  public int hashCode() {
    return getCleanedUrl().hashCode();
  }

  /**
   * This implementation creates a {@code UrlBasedResource}, delegating to
   * {@link #createRelativeURL(String)} for adapting the relative path.
   *
   * @see #createRelativeURL(String)
   */
  @Override
  public UrlBasedResource createRelative(String relativePath) throws IOException {
    return new UrlBasedResource(createRelativeURL(relativePath));
  }

  /**
   * This delegate creates a {@code java.net.URL}, applying the given path
   * relative to the path of the underlying URL of this resource descriptor. A
   * leading slash will get dropped; a "#" symbol will get encoded.
   *
   * @see #createRelative(String)
   * @see java.net.URL#URL(java.net.URL, String)
   */
  protected URL createRelativeURL(String relativePath) throws MalformedURLException {
    String relativePathToUse = relativePath;
    if (StringUtils.matchesFirst(relativePathToUse, '/')) {
      relativePathToUse = relativePathToUse.substring(1);
    }
    // # can appear in filenames, java.net.URL should not treat it as a fragment
    relativePathToUse = StringUtils.replace(relativePathToUse, "#", "%23");
    // Use the URL constructor for applying the relative path as a URL spec
    return new URL(this.url, relativePathToUse);
  }

  @Override
  public String toString() {
    return "UrlBasedResource: ".concat(url.toString());
  }

}
