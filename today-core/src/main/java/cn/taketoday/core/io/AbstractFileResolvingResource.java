/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;

import cn.taketoday.util.ResourceUtils;

/**
 * Abstract base class for resources which resolve URLs into File references,
 * such as {@link UrlResource} or {@link ClassPathResource}.
 *
 * <p>Detects the "file" protocol as well as the JBoss "vfs" protocol in URLs,
 * resolving file system references accordingly.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 16:28
 */
public abstract class AbstractFileResolvingResource extends AbstractResource {

  @Override
  public boolean exists() {
    try {
      URL url = getURL();
      if (ResourceUtils.isFileURL(url)) {
        // Proceed with file system resolution
        return getFile().exists();
      }
      else {
        // Try a URL connection content-length header
        URLConnection con = url.openConnection();
        customizeConnection(con);
        HttpURLConnection httpCon =
                (con instanceof HttpURLConnection ? (HttpURLConnection) con : null);
        if (httpCon != null) {
          httpCon.setRequestMethod("HEAD");
          int code = httpCon.getResponseCode();
          if (code == HttpURLConnection.HTTP_OK) {
            return true;
          }
          else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
            return false;
          }
        }
        if (con.getContentLengthLong() > 0) {
          return true;
        }
        if (httpCon != null) {
          // No HTTP OK status, and no content-length header: give up
          httpCon.disconnect();
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
          httpCon.setRequestMethod("HEAD");
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

  @Override
  public boolean isFile() {
    try {
      URL url = getURL();
      return ResourceUtils.URL_PROTOCOL_FILE.equals(url.getProtocol());
    }
    catch (IOException ex) {
      return false;
    }
  }

  /**
   * This implementation returns a File reference for the underlying class path
   * resource, provided that it refers to a file in the file system.
   *
   * @see ResourceUtils#getFile(java.net.URL, String)
   */
  @Override
  public File getFile() throws IOException {
    URL url = getURL();
    return ResourceUtils.getFile(url, toString());
  }

  /**
   * This implementation determines the underlying File
   * (or jar file, in case of a resource in a jar/zip).
   */
  @Override
  protected File getFileForLastModifiedCheck() throws IOException {
    URL url = getURL();
    if (ResourceUtils.isJarURL(url)) {
      URL actualUrl = ResourceUtils.extractArchiveURL(url);
      return ResourceUtils.getFile(actualUrl, "Jar URL");
    }
    else {
      return getFile();
    }
  }

  /**
   * Determine whether the given {@link URI} represents a file in a file system.
   *
   * @see #getFile(URI)
   */
  protected boolean isFile(URI uri) {
    return ResourceUtils.URL_PROTOCOL_FILE.equals(uri.getScheme());
  }

  /**
   * This implementation returns a File reference for the given URI-identified
   * resource, provided that it refers to a file in the file system.
   *
   * @see ResourceUtils#getFile(java.net.URI, String)
   */
  protected File getFile(URI uri) throws IOException {
    return ResourceUtils.getFile(uri, toString());
  }

  /**
   * This implementation returns a FileChannel for the given URI-identified
   * resource, provided that it refers to a file in the file system.
   *
   * @see #getFile()
   */
  @Override
  public ReadableByteChannel readableChannel() throws IOException {
    try {
      // Try file system channel
      return FileChannel.open(getFile().toPath(), StandardOpenOption.READ);
    }
    catch (FileNotFoundException | NoSuchFileException ex) {
      // Fall back to InputStream adaptation in superclass
      return super.readableChannel();
    }
  }

  @Override
  public long contentLength() throws IOException {
    URL url = getURL();
    if (ResourceUtils.isFileURL(url)) {
      // Proceed with file system resolution
      File file = getFile();
      long length = file.length();
      if (length == 0L && !file.exists()) {
        throw new FileNotFoundException(this +
                " cannot be resolved in the file system for checking its content length");
      }
      return length;
    }
    else {
      // Try a URL connection content-length header
      URLConnection con = url.openConnection();
      customizeConnection(con);
      if (con instanceof HttpURLConnection httpCon) {
        httpCon.setRequestMethod("HEAD");
      }
      return con.getContentLengthLong();
    }
  }

  @Override
  public long lastModified() throws IOException {
    URL url = getURL();
    boolean fileCheck = false;
    if (ResourceUtils.isFileURL(url) || ResourceUtils.isJarURL(url)) {
      // Proceed with file system resolution
      fileCheck = true;
      try {
        File fileToCheck = getFileForLastModifiedCheck();
        long lastModified = fileToCheck.lastModified();
        if (lastModified > 0L || fileToCheck.exists()) {
          return lastModified;
        }
      }
      catch (FileNotFoundException ex) {
        // Defensively fall back to URL connection check instead
      }
    }
    // Try a URL connection last-modified header
    URLConnection con = url.openConnection();
    customizeConnection(con);
    if (con instanceof HttpURLConnection httpCon) {
      httpCon.setRequestMethod("HEAD");
    }
    long lastModified = con.getLastModified();
    if (fileCheck && lastModified == 0 && con.getContentLengthLong() <= 0) {
      throw new FileNotFoundException(this +
              " cannot be resolved in the file system for checking its last-modified timestamp");
    }
    return lastModified;
  }

  /**
   * Customize the given {@link URLConnection} before fetching the resource.
   * <p>Calls {@link ResourceUtils#useCachesIfNecessary(URLConnection)} and
   * delegates to {@link #customizeConnection(HttpURLConnection)} if possible.
   * Can be overridden in subclasses.
   *
   * @param con the URLConnection to customize
   * @throws IOException if thrown from URLConnection methods
   */
  protected void customizeConnection(URLConnection con) throws IOException {
    ResourceUtils.useCachesIfNecessary(con);
    if (con instanceof HttpURLConnection httpConn) {
      customizeConnection(httpConn);
    }
  }

  /**
   * Customize the given {@link HttpURLConnection} before fetching the resource.
   * <p>Can be overridden in subclasses for configuring request headers and timeouts.
   *
   * @param con the HttpURLConnection to customize
   * @throws IOException if thrown from HttpURLConnection methods
   */
  protected void customizeConnection(HttpURLConnection con) throws IOException {

  }

}
