/*
 * Copyright 2017 - 2025 the original author or authors.
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
package infra.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ResourceUtils;
import infra.util.StringUtils;

/**
 * {@link Resource} implementation for {@code java.net.URL} locators.
 * Supports resolution as a {@code URL} and also as a {@code File} in
 * case of the {@code "file:"} protocol.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see java.net.URL
 * @since 2.1.6 2019-05-14 22:26
 */
public class UrlResource extends AbstractFileResolvingResource {

  private static final String AUTHORIZATION = "Authorization";

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
   * Cleaned URL String (with normalized path), used for comparisons.
   */
  @Nullable
  private volatile String cleanedUrl;

  /**
   * Whether to use URLConnection caches ({@code null} means default).
   *
   * @since 5.0
   */
  @Nullable
  volatile Boolean useCaches;

  /**
   * Create a new {@code UrlResource} based on the given URI object.
   *
   * @param uri a URI
   * @throws MalformedURLException if the given URL path is not valid
   * @since 4.0
   */
  public UrlResource(URI uri) throws MalformedURLException {
    Assert.notNull(uri, "URI is required");
    this.uri = uri;
    this.url = uri.toURL();
  }

  /**
   * Create a new {@code UrlResource} based on the given URL object.
   *
   * @param url a URL
   */
  public UrlResource(URL url) {
    Assert.notNull(url, "URL is required");
    this.uri = null;
    this.url = url;
  }

  /**
   * Create a new {@code UrlResource} based on a URI path.
   * <p>Note: The given path needs to be pre-encoded if necessary.
   *
   * @param path a URI path
   * @throws MalformedURLException if the given URI path is not valid
   * @see ResourceUtils#toURI(String)
   * @since 4.0
   */
  public UrlResource(String path) throws MalformedURLException {
    Assert.notNull(path, "Path is required");
    String cleanedPath = StringUtils.cleanPath(path);
    URI uri;
    URL url;

    try {
      // Prefer URI construction with toURL conversion (as of 6.1)
      uri = ResourceUtils.toURI(cleanedPath);
      url = uri.toURL();
    }
    catch (URISyntaxException | IllegalArgumentException ex) {
      uri = null;
      url = ResourceUtils.toURL(path);
    }

    this.uri = uri;
    this.url = url;
    this.cleanedUrl = cleanedPath;
  }

  /**
   * Create a new {@code UrlResource} based on a URI specification.
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
  public UrlResource(String protocol, String location) throws MalformedURLException {
    this(protocol, location, null);
  }

  /**
   * Create a new {@code UrlResource} based on a URI specification.
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
  public UrlResource(String protocol, String location, @Nullable String fragment) throws MalformedURLException {
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
   * Set an explicit flag for {@link URLConnection#setUseCaches},
   * to be applied for any {@link URLConnection} operation in this resource.
   * <p>By default, caching will be applied only to jar resources.
   * An explicit {@code true} flag applies caching to all resources, whereas an
   * explicit {@code false} flag turns off caching for jar resources as well.
   *
   * @see ResourceUtils#useCachesIfNecessary
   * @since 5.0
   */
  public void setUseCaches(boolean useCaches) {
    this.useCaches = useCaches;
  }

  /**
   * This implementation opens an InputStream for the given URL.
   *
   * @see java.net.URL#openConnection()
   * @see java.net.URLConnection#setUseCaches(boolean)
   * @see java.net.URLConnection#getInputStream()
   */
  @Override
  public InputStream getInputStream() throws IOException {
    URLConnection con = this.url.openConnection();
    customizeConnection(con);
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

  @Override
  protected void customizeConnection(URLConnection con) throws IOException {
    super.customizeConnection(con);
    String userInfo = this.url.getUserInfo();
    if (userInfo != null) {
      String encodedCredentials = Base64.getUrlEncoder().encodeToString(userInfo.getBytes());
      con.setRequestProperty(AUTHORIZATION, "Basic " + encodedCredentials);
    }
  }

  @Override
  void useCachesIfNecessary(URLConnection con) {
    Boolean useCaches = this.useCaches;
    if (useCaches != null) {
      con.setUseCaches(useCaches);
    }
    else {
      super.useCachesIfNecessary(con);
    }
  }

  /**
   * This implementation returns the underlying URL reference.
   */
  @Override
  public URL getURL() {
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
   * Lazily determine a cleaned URL for the given original URL.
   */
  private String getCleanedUrl() {
    String cleanedUrl = this.cleanedUrl;
    if (cleanedUrl != null) {
      return cleanedUrl;
    }
    String originalPath = (this.uri != null ? this.uri : this.url).toString();
    cleanedUrl = StringUtils.cleanPath(originalPath);
    this.cleanedUrl = cleanedUrl;
    return cleanedUrl;
  }

  /**
   * This implementation returns the URL-decoded name of the file that this URL
   * refers to.
   *
   * @see java.net.URL#getPath()
   * @see java.net.URLDecoder#decode(String, java.nio.charset.Charset)
   */
  @Override
  @Nullable
  public String getName() {
    if (this.uri != null) {
      String path = this.uri.getPath();
      if (path != null) {
        // Prefer URI path: decoded and has standard separators
        return StringUtils.getFilename(this.uri.getPath());
      }
    }
    // Otherwise, process URL path
    String filename = StringUtils.getFilename(StringUtils.cleanPath(this.url.getPath()));
    return (filename != null ? URLDecoder.decode(filename, StandardCharsets.UTF_8) : null);
  }

  /**
   * This implementation compares the underlying URL references.
   */
  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof UrlResource
            && getCleanedUrl().equals(((UrlResource) other).getCleanedUrl())));
  }

  /**
   * This implementation returns the hash code of the underlying URL reference.
   */
  @Override
  public int hashCode() {
    return getCleanedUrl().hashCode();
  }

  /**
   * This implementation creates a {@code UrlResource}, delegating to
   * {@link #createRelativeURL(String)} for adapting the relative path.
   *
   * @see #createRelativeURL(String)
   */
  @Override
  public UrlResource createRelative(String relativePath) throws IOException {
    UrlResource resource = new UrlResource(createRelativeURL(relativePath));
    resource.useCaches = this.useCaches;
    return resource;
  }

  /**
   * This delegate creates a {@code java.net.URL}, applying the given path
   * relative to the path of the underlying URL of this resource descriptor. A
   * leading slash will get dropped; a "#" symbol will get encoded.
   *
   * @see #createRelative(String)
   * @see ResourceUtils#toRelativeURL(URL, String)
   */
  protected URL createRelativeURL(String relativePath) throws MalformedURLException {
    if (relativePath.startsWith("/")) {
      relativePath = relativePath.substring(1);
    }
    return ResourceUtils.toRelativeURL(this.url, relativePath);
  }

  /**
   * This implementation returns a description that includes the URL.
   */
  @Override
  public String toString() {
    return "URL [%s]".formatted(this.uri != null ? this.uri : this.url);
  }

  /**
   * Create a new {@code UrlResource} from the given {@link URI}.
   * <p>This factory method is a convenience for {@link #UrlResource(URI)} that
   * catches any {@link MalformedURLException} and rethrows it wrapped in an
   * {@link UncheckedIOException}; suitable for use in {@link java.util.stream.Stream}
   * and {@link java.util.Optional} APIs or other scenarios when a checked
   * {@link IOException} is undesirable.
   *
   * @param uri a URI
   * @throws UncheckedIOException if the given URL path is not valid
   * @see #UrlResource(URI)
   * @since 4.0
   */
  public static UrlResource from(URI uri) throws UncheckedIOException {
    try {
      return new UrlResource(uri);
    }
    catch (MalformedURLException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Create a new {@code UrlResource} from the given URL path.
   * <p>This factory method is a convenience for {@link #UrlResource(String)}
   * that catches any {@link MalformedURLException} and rethrows it wrapped in an
   * {@link UncheckedIOException}; suitable for use in {@link java.util.stream.Stream}
   * and {@link java.util.Optional} APIs or other scenarios when a checked
   * {@link IOException} is undesirable.
   *
   * @param path a URL path
   * @throws UncheckedIOException if the given URL path is not valid
   * @see #UrlResource(String)
   * @since 4.0
   */
  public static UrlResource from(String path) throws UncheckedIOException {
    try {
      return new UrlResource(path);
    }
    catch (MalformedURLException ex) {
      throw new UncheckedIOException(ex);
    }
  }

}
