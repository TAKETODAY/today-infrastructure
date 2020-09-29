/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.context.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ResourceUtils;

/**
 * @author TODAY <br>
 * 2019-05-14 21:47
 */
public class ClassPathResource implements Resource, WritableResource {

  private final Resource resource;

  public ClassPathResource(URL location) {
    this.resource = ResourceUtils.getResource(location);
  }

  public ClassPathResource(String location) {
    this(location, ClassUtils.getClassLoader());
  }

  public ClassPathResource(String location, Class<?> resourceClass) {
    this(location, resourceClass.getClassLoader());
  }

  public ClassPathResource(String location, ClassLoader classLoader) {
    Assert.notNull(location, "Location must not be null");
    final URL resource = (classLoader != null ? classLoader : ClassUtils.getClassLoader()).getResource(location);
    // linux path start with '/'
    this.resource = resource == null ? new FileBasedResource(location) : ResourceUtils.getResource(resource);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return resource.getInputStream();
  }

  @Override
  public long contentLength() throws IOException {
    return resource.contentLength();
  }

  @Override
  public String getName() {
    return resource.getName();
  }

  @Override
  public long lastModified() throws IOException {
    return resource.lastModified();
  }

  @Override
  public URL getLocation() throws IOException {
    return resource.getLocation();
  }

  @Override
  public File getFile() throws IOException {
    return resource.getFile();
  }

  @Override
  public boolean exists() {
    return resource.exists();
  }

  @Override
  public boolean isDirectory() throws IOException {
    return resource.isDirectory();
  }

  @Override
  public String[] list() throws IOException {
    return resource.list();
  }

  @Override
  public Resource createRelative(String relativePath) throws IOException {
    return resource.createRelative(relativePath);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    final Resource resource = this.resource;
    if (resource instanceof Writable) {
      return ((Writable) resource).getOutputStream();
    }
    throw new IOException("Writable operation is not supported");
  }

  @Override
  public ReadableByteChannel readableChannel() throws IOException {
    return resource.readableChannel();
  }

  @Override
  public WritableByteChannel writableChannel() throws IOException {
    final Resource resource = this.resource;
    if (resource instanceof Writable) {
      return ((Writable) resource).writableChannel();
    }
    throw new ConfigurationException("Writable operation is not supported");
  }

  @Override
  public Resource[] list(ResourceFilter filter) throws IOException {
    return resource.list(filter);
  }

  @Override
  public String toString() {
    return resource.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj instanceof ClassPathResource && resource.equals(((ClassPathResource) obj).getOriginalResource()));
  }

  /**
   * Get Original {@link Resource}
   *
   * @return Original {@link Resource}
   */
  public final Resource getOriginalResource() {
    return resource;
  }

  public URI getURI() throws IOException {
    return resource.getURI();
  }
}
