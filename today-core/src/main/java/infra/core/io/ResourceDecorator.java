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
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Objects;

import infra.lang.Assert;

/**
 * This class implements the Wrapper or Decorator pattern.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 9:06
 */
public class ResourceDecorator implements Resource {

  protected Resource delegate;

  protected ResourceDecorator() {
  }

  public ResourceDecorator(Resource delegate) {
    Assert.notNull(delegate, "Resource delegate is required");
    this.delegate = delegate;
  }

  // InputStreamSource

  @Override
  public InputStream getInputStream() throws IOException {
    return getDelegate().getInputStream();
  }

  @Override
  public Reader getReader() throws IOException {
    return getDelegate().getReader();
  }

  @Override
  public Reader getReader(Charset encoding) throws IOException {
    return getDelegate().getReader(encoding);
  }

  @Override
  public ReadableByteChannel readableChannel() throws IOException {
    return getDelegate().readableChannel();
  }

  // Resource

  @Override
  public String getName() {
    return getDelegate().getName();
  }

  @Override
  public long contentLength() throws IOException {
    return getDelegate().contentLength();
  }

  @Override
  public long lastModified() throws IOException {
    return getDelegate().lastModified();
  }

  @Override
  public URL getURL() throws IOException {
    return getDelegate().getURL();
  }

  @Override
  public URI getURI() throws IOException {
    return getDelegate().getURI();
  }

  @Override
  public File getFile() throws IOException {
    return getDelegate().getFile();
  }

  @Override
  public boolean exists() {
    try {
      return getDelegate().exists();
    }
    catch (Exception e) {
      if (e instanceof IOException) {
        return false;
      }
      throw e;
    }
  }

  @Override
  public boolean isReadable() {
    return getDelegate().isReadable();
  }

  @Override
  public boolean isOpen() {
    return getDelegate().isOpen();
  }

  @Override
  public boolean isFile() {
    return getDelegate().isFile();
  }

  @Override
  public Resource createRelative(String relativePath) throws IOException {
    return getDelegate().createRelative(relativePath);
  }

  // delegate

  public Resource getDelegate() {
    return delegate;
  }

  public void setDelegate(Resource delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof ResourceDecorator decorator) {
      return Objects.equals(decorator.delegate, delegate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

}
