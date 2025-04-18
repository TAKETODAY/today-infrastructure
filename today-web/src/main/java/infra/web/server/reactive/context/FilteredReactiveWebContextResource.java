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

package infra.web.server.reactive.context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import infra.core.io.AbstractResource;
import infra.core.io.Resource;
import infra.util.StringUtils;

/**
 * Resource implementation that replaces the resource in a reactive
 * web application.
 * <p>
 * {@link #exists()} always returns {@code false} in order to avoid exposing the whole
 * classpath in a non-mock environment.
 *
 * @author Brian Clozel
 * @since 4.0
 */
class FilteredReactiveWebContextResource extends AbstractResource {

  private final String path;

  FilteredReactiveWebContextResource(String path) {
    this.path = path;
  }

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public Resource createRelative(String relativePath) throws IOException {
    String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
    return new FilteredReactiveWebContextResource(pathToUse);
  }

  @Override
  public String toString() {
    return "ReactiveWebContext resource [" + this.path + "]";
  }

  @Override
  public InputStream getInputStream() throws IOException {
    throw new FileNotFoundException(this + " cannot be opened because it does not exist");
  }

}
