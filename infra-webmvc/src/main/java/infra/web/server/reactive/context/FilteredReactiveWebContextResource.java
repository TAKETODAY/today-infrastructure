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
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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
    return "ReactiveWebContext resource [%s]".formatted(this.path);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    throw new FileNotFoundException(this + " cannot be opened because it does not exist");
  }

}
