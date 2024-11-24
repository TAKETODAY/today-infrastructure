/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.app.context.config;

import java.io.IOException;

import infra.core.io.FileSystemResource;
import infra.core.io.FileUrlResource;
import infra.core.io.Resource;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * {@link ConfigDataResource} backed by a {@link Resource}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
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
        return "file [" + this.resource.getFile().toString() + "]";
      }
      catch (IOException ignored) { }
    }
    return this.resource.toString();
  }

}
