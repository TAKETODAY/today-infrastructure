/**
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
package cn.taketoday.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2019-05-14 22:32
 * @since 2.1.6
 */
public abstract class AbstractResource implements Resource {

  @Override
  public String getName() {
    try {
      return getFile().getName();
    }
    catch (IOException e) {
      return null;
    }
  }

  @Override
  public boolean exists() {

    try (InputStream inputStream = getInputStream()) {
      return inputStream != null;
    }
    catch (IOException e) {
      return false;
    }
  }

  @Override
  public boolean isReadable() {
    try {
      URL url = getLocation();
      if (ResourceUtils.isFileURL(url)) {
        // Proceed with file system resolution
        File file = getFile();
        return (file.canRead() && !file.isDirectory());
      }
      else {
        // Try InputStream resolution for jar resources
        URLConnection con = url.openConnection();
        customizeConnection(con);
        if (con instanceof HttpURLConnection) {
          HttpURLConnection httpCon = (HttpURLConnection) con;
          int code = httpCon.getResponseCode();
          if (code != HttpURLConnection.HTTP_OK) {
            httpCon.disconnect();
            return false;
          }
        }
        long contentLength = con.getContentLengthLong();
        if (contentLength > 0) {
          return true;
        }
        else if (contentLength == 0) {
          // Empty file or directory -> not considered readable...
          return false;
        }
        else {
          // Fall back to stream existence: can we open the stream?
          getInputStream().close();
          return true;
        }
      }
    }
    catch (IOException ex) {
      return false;
    }
  }

  /**
   * Customize the given {@link URLConnection}, obtained in the course of an
   * {@link #exists()}, {@link #contentLength()} or {@link #lastModified()} call.
   * <p>Calls {@link ResourceUtils#useCachesIfNecessary(URLConnection)} and
   * delegates to {@link #customizeConnection(HttpURLConnection)} if possible.
   * Can be overridden in subclasses.
   *
   * @param con
   *         the URLConnection to customize
   *
   * @throws IOException
   *         if thrown from URLConnection methods
   */
  protected void customizeConnection(URLConnection con) throws IOException {
    ResourceUtils.useCachesIfNecessary(con);
    if (con instanceof HttpURLConnection) {
      customizeConnection((HttpURLConnection) con);
    }
  }

  /**
   * Customize the given {@link HttpURLConnection}, obtained in the course of an
   * {@link #exists()}, {@link #contentLength()} or {@link #lastModified()} call.
   * <p>Sets request method "HEAD" by default. Can be overridden in subclasses.
   *
   * @param con
   *         the HttpURLConnection to customize
   *
   * @throws IOException
   *         if thrown from HttpURLConnection methods
   */
  protected void customizeConnection(HttpURLConnection con) throws IOException {
    con.setRequestMethod("HEAD");
  }

  /**
   * This implementation always returns {@code false}.
   */
  @Override
  public boolean isOpen() {
    return false;
  }

  @Override
  public URL getLocation() throws IOException {
    throw new FileNotFoundException(this + " cannot be resolved to URL");
  }

  @Override
  public URI getURI() throws IOException {
    final URL location = getLocation();
    try {
      return location.toURI();
    }
    catch (URISyntaxException ex) {
      throw new IOException("Invalid URI [" + location + "]", ex);
    }
  }

  @Override
  public File getFile() throws IOException {
    throw new FileNotFoundException(this + " cannot be resolved to absolute file path");
  }

  @Override
  public boolean isDirectory() throws IOException {
    return getFile().isDirectory();
  }

  @Override
  public String[] list() throws IOException {
    return getFile().list();
  }

  @Override
  public Resource[] list(ResourceFilter filter) throws IOException {
    final String[] names = list();

    if (StringUtils.isArrayEmpty(names)) {
      return EMPTY_ARRAY;
    }

    List<Resource> resources = new ArrayList<>();
    for (String name : names) { // this resource is a directory
      Resource resource = createRelative(name);
      if ((filter == null) || filter.accept(resource)) {
        resources.add(resource);
      }
    }
    if (resources.isEmpty()) {
      return EMPTY_ARRAY;
    }
    return resources.toArray(new Resource[resources.size()]);
  }

  /**
   * This method reads the entire InputStream to determine the content length.
   * <p>For a custom sub-class of {@code InputStreamResource}, we strongly
   * recommend overriding this method with a more optimal implementation, e.g.
   * checking File length, or possibly simply returning -1 if the stream can
   * only be read once.
   *
   * @see #getInputStream()
   */
  @Override
  public long contentLength() throws IOException {
    InputStream is = getInputStream();
    try {
      long size = 0;
      byte[] buf = new byte[256];
      int read;
      while ((read = is.read(buf)) != -1) {
        size += read;
      }
      return size;
    }
    finally {
      try {
        is.close();
      }
      catch (IOException ex) {
        Logger logger = LoggerFactory.getLogger(getClass());
        if (logger.isDebugEnabled()) {
          logger.debug("Could not close content-length InputStream for " + this, ex);
        }
      }
    }
  }

  /**
   * This implementation checks the timestamp of the underlying File,
   * if available.
   *
   * @see #getFileForLastModifiedCheck()
   */
  @Override
  public long lastModified() throws IOException {
    File fileToCheck = getFileForLastModifiedCheck();
    long lastModified = fileToCheck.lastModified();
    if (lastModified == 0L && !fileToCheck.exists()) {
      throw new FileNotFoundException(this + " cannot be resolved in the file system for checking its last-modified timestamp");
    }
    return lastModified;
  }

  /**
   * Determine the File to use for timestamp checking.
   * <p>The default implementation delegates to {@link #getFile()}.
   *
   * @return the File to use for timestamp checking (never {@code null})
   *
   * @throws FileNotFoundException
   *         if the resource cannot be resolved as
   *         an absolute file path, i.e. is not available in a file system
   * @throws IOException
   *         in case of general resolution/reading failures
   */
  protected File getFileForLastModifiedCheck() throws IOException {
    return getFile();
  }

  @Override
  public Resource createRelative(String relativePath) throws IOException {
    throw new FileNotFoundException(this + " cannot be resolved relative file path for " + relativePath);
  }

  @Override
  public String toString() {
    try {
      StringBuilder builder = new StringBuilder();
      builder.append("{\n\t\"name\":\"");
      builder.append(getName());
      builder.append("\",\n\t\"exists\":\"");
      builder.append(exists());
      builder.append("\",\n\t\"location\":\"");
      builder.append(getLocation());
      builder.append("\",\n\t\"file\":\"");
      builder.append(getFile());
      builder.append("\"\n}");
      return builder.toString();
    }
    catch (IOException e) {
      return super.toString();
    }
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj instanceof AbstractResource && Objects.equals(toString(), obj.toString()));
  }

}
