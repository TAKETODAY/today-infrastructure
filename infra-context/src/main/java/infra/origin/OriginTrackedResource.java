/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.origin;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import infra.core.io.Resource;
import infra.core.io.ResourceDecorator;
import infra.core.io.WritableResource;

/**
 * Decorator that can be used to add {@link Origin} information to a {@link Resource} or
 * {@link WritableResource}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #from(Resource, Origin)
 * @see #from(WritableResource, Origin)
 * @see OriginProvider
 * @since 4.0
 */
public class OriginTrackedResource extends ResourceDecorator implements Resource, OriginProvider {

  @Nullable
  private final Origin origin;

  /**
   * Create a new {@link OriginTrackedResource} instance.
   *
   * @param resource the resource to track
   * @param origin the origin of the resource
   */
  OriginTrackedResource(Resource resource, @Nullable Origin origin) {
    super(resource);
    this.origin = origin;
  }

  @Nullable
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
    return this.delegate.equals(other.delegate) && Objects.equals(origin, other.origin);
  }

  @Override
  public int hashCode() {
    return this.delegate.hashCode() * 31 + Objects.hashCode(this.origin);
  }

  @Override
  public String toString() {
    return this.delegate.toString();
  }

  /**
   * Return a new {@link OriginProvider origin tracked} version the given
   * {@link WritableResource}.
   *
   * @param resource the tracked resource
   * @param origin the origin of the resource
   * @return a {@link OriginTrackedWritableResource} instance
   */
  public static OriginTrackedWritableResource from(WritableResource resource, @Nullable Origin origin) {
    return new OriginTrackedWritableResource(resource, origin);
  }

  /**
   * Return a new {@link OriginProvider origin tracked} version the given
   * {@link Resource}.
   *
   * @param resource the tracked resource
   * @param origin the origin of the resource
   * @return a {@link OriginTrackedResource} instance
   */
  public static OriginTrackedResource from(Resource resource, @Nullable Origin origin) {
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
    OriginTrackedWritableResource(WritableResource resource, @Nullable Origin origin) {
      super(resource, origin);
    }

    @Override
    public WritableResource getDelegate() {
      return (WritableResource) super.getDelegate();
    }

    @Override
    public boolean isWritable() {
      return getDelegate().isWritable();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      return getDelegate().getOutputStream();
    }

    @Override
    public WritableByteChannel writableChannel() throws IOException {
      return getDelegate().writableChannel();
    }

    @Override
    public Writer getWriter() throws IOException {
      return getDelegate().getWriter();
    }

  }

}
