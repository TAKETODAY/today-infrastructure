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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Objects;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ResourceUtils;

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
      return checkReadable(getURL());
    }
    catch (IOException ex) {
      return false;
    }
  }

  boolean checkReadable(URL url) {
    try {
      if (ResourceUtils.isFileURL(url)) {
        // Proceed with file system resolution
        File file = getFile();
        return (file.canRead() && !file.isDirectory());
      }
      else {
        // Try InputStream resolution for jar resources
        URLConnection con = url.openConnection();
        customizeConnection(con);
        if (con instanceof HttpURLConnection httpCon) {
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
   * @param con the URLConnection to customize
   * @throws IOException if thrown from URLConnection methods
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
   * @param con the HttpURLConnection to customize
   * @throws IOException if thrown from HttpURLConnection methods
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

  /**
   * This implementation always returns {@code false}.
   */
  @Override
  public boolean isFile() {
    return false;
  }

  @Override
  public URL getURL() throws IOException {
    throw new FileNotFoundException(this + " cannot be resolved to URL");
  }

  @Override
  public URI getURI() throws IOException {
    URL location = getURL();
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
    String[] names = list();

    if (ObjectUtils.isEmpty(names)) {
      return EMPTY_ARRAY;
    }

    ArrayList<Resource> resources = new ArrayList<>();
    for (String name : names) { // this resource is a directory
      Resource resource = createRelative(name);
      if ((filter == null) || filter.accept(resource)) {
        resources.add(resource);
      }
    }
    if (resources.isEmpty()) {
      return EMPTY_ARRAY;
    }
    return resources.toArray(Resource.EMPTY_ARRAY);
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
   * @throws FileNotFoundException if the resource cannot be resolved as
   * an absolute file path, i.e. is not available in a file system
   * @throws IOException in case of general resolution/reading failures
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
      return ToStringBuilder.from(this).append("name", getName())
              .append("location", getURL())
              .toString();
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
    if (obj == this) {
      return true;
    }
    if (obj instanceof Resource resource) {
      try {
        URL location = getURL();
        URL otherLocation = resource.getURL();
        return Objects.equals(location, otherLocation);
      }
      catch (Exception ignored) { }
      return Objects.equals(toString(), obj.toString());
    }
    return false;
  }

}
