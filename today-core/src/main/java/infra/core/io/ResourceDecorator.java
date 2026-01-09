/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.core.io;

import org.jspecify.annotations.Nullable;

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
  @Nullable
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
    Assert.notNull(delegate, "Resource delegate is required");
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
