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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.origin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;

import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceFilter;
import cn.taketoday.core.io.WritableResource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Decorator that can be used to add {@link Origin} information to a {@link Resource} or
 * {@link WritableResource}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #of(Resource, Origin)
 * @see #of(WritableResource, Origin)
 * @see OriginProvider
 * @since 4.0
 */
public class OriginTrackedResource implements Resource, OriginProvider {

  private final Resource resource;

  private final Origin origin;

  /**
   * Create a new {@link OriginTrackedResource} instance.
   *
   * @param resource the resource to track
   * @param origin the origin of the resource
   */
  OriginTrackedResource(Resource resource, Origin origin) {
    Assert.notNull(resource, "Resource must not be null");
    this.resource = resource;
    this.origin = origin;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return getResource().getInputStream();
  }

  @Override
  public boolean exists() {
    return getResource().exists();
  }

  @Override
  public boolean isReadable() {
    return getResource().isReadable();
  }

  @Override
  public boolean isOpen() {
    return getResource().isOpen();
  }

  @Override
  public URL getLocation() throws IOException {
    return getResource().getLocation();
  }

  @Override
  public URI getURI() throws IOException {
    return getResource().getURI();
  }

  @Override
  public File getFile() throws IOException {
    return getResource().getFile();
  }

  @Override
  public ReadableByteChannel readableChannel() throws IOException {
    return getResource().readableChannel();
  }

  @Override
  public long contentLength() throws IOException {
    return getResource().contentLength();
  }

  @Override
  public long lastModified() throws IOException {
    return getResource().lastModified();
  }

  @Override
  public Resource createRelative(String relativePath) throws IOException {
    return getResource().createRelative(relativePath);
  }

  @Override
  public String getName() {
    return getResource().getName();
  }

  @Override
  public boolean isDirectory() throws IOException {
    return getResource().isDirectory();
  }

  @Override
  public String[] list() throws IOException {
    return getResource().list();
  }

  @Override
  public Resource[] list(@Nullable ResourceFilter filter) throws IOException {
    return getResource().list(filter);
  }

  public Resource getResource() {
    return this.resource;
  }

  @Override
  public Origin getOrigin() {
    return this.origin;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    OriginTrackedResource other = (OriginTrackedResource) obj;
    return this.resource.equals(other) && ObjectUtils.nullSafeEquals(this.origin, other.origin);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = this.resource.hashCode();
    result = prime * result + ObjectUtils.nullSafeHashCode(this.origin);
    return result;
  }

  @Override
  public String toString() {
    return this.resource.toString();
  }

  /**
   * Return a new {@link OriginProvider origin tracked} version the given
   * {@link WritableResource}.
   *
   * @param resource the tracked resource
   * @param origin the origin of the resource
   * @return a {@link OriginTrackedWritableResource} instance
   */
  public static OriginTrackedWritableResource of(WritableResource resource, Origin origin) {
    return (OriginTrackedWritableResource) of((Resource) resource, origin);
  }

  /**
   * Return a new {@link OriginProvider origin tracked} version the given
   * {@link Resource}.
   *
   * @param resource the tracked resource
   * @param origin the origin of the resource
   * @return a {@link OriginTrackedResource} instance
   */
  public static OriginTrackedResource of(Resource resource, Origin origin) {
    if (resource instanceof WritableResource) {
      return new OriginTrackedWritableResource((WritableResource) resource, origin);
    }
    return new OriginTrackedResource(resource, origin);
  }

  /**
   * Variant of {@link OriginTrackedResource} for {@link WritableResource} instances.
   */
  public static class OriginTrackedWritableResource extends OriginTrackedResource implements WritableResource {

    /**
     * Create a new {@link OriginTrackedWritableResource} instance.
     *
     * @param resource the resource to track
     * @param origin the origin of the resource
     */
    OriginTrackedWritableResource(WritableResource resource, Origin origin) {
      super(resource, origin);
    }

    @Override
    public WritableResource getResource() {
      return (WritableResource) super.getResource();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      return getResource().getOutputStream();
    }

  }

}
