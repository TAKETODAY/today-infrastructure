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

package infra.app.context.config;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.core.io.FileSystemResource;
import infra.core.io.FileUrlResource;
import infra.core.io.Resource;
import infra.lang.Assert;

/**
 * {@link ConfigDataResource} backed by a {@link Resource}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class StandardConfigDataResource extends ConfigDataResource {

  private final StandardConfigDataReference reference;

  private final Resource resource;

  private final boolean emptyDirectory;

  /**
   * Create a new {@link StandardConfigDataResource} instance.
   *
   * @param reference the resource reference
   * @param resource the underlying resource
   */
  StandardConfigDataResource(StandardConfigDataReference reference, Resource resource) {
    this(reference, resource, false);
  }

  /**
   * Create a new {@link StandardConfigDataResource} instance.
   *
   * @param reference the resource reference
   * @param resource the underlying resource
   * @param emptyDirectory if the resource is an empty directory that we know exists
   */
  StandardConfigDataResource(StandardConfigDataReference reference, Resource resource, boolean emptyDirectory) {
    Assert.notNull(reference, "Reference is required");
    Assert.notNull(resource, "Resource is required");
    this.reference = reference;
    this.resource = resource;
    this.emptyDirectory = emptyDirectory;
  }

  StandardConfigDataReference getReference() {
    return this.reference;
  }

  /**
   * Return the underlying Infra {@link Resource} being loaded.
   *
   * @return the underlying resource
   */
  public Resource getResource() {
    return this.resource;
  }

  /**
   * Return the profile or {@code null} if the resource is not profile specific.
   *
   * @return the profile or {@code null}
   */
  @Nullable
  public String getProfile() {
    return this.reference.profile;
  }

  boolean isEmptyDirectory() {
    return this.emptyDirectory;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    StandardConfigDataResource other = (StandardConfigDataResource) obj;
    return this.resource.equals(other.resource) && this.emptyDirectory == other.emptyDirectory;
  }

  @Override
  public int hashCode() {
    return this.resource.hashCode();
  }

  @Override
  public String toString() {
    if (this.resource instanceof FileSystemResource || this.resource instanceof FileUrlResource) {
      try {
        return "file [" + this.resource.getFile() + "]";
      }
      catch (IOException ignored) {
      }
    }
    return this.resource.toString();
  }

}
