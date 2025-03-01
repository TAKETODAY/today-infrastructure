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

package infra.test.classpath.resources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * A {@link ClassLoader} that provides access to {@link Resources resources}.
 *
 * @author Andy Wilkinson
 */
class ResourcesClassLoader extends ClassLoader {

  private final Resources resources;

  ResourcesClassLoader(ClassLoader parent, Resources resources) {
    super(parent);
    this.resources = resources;
  }

  @Override
  protected URL findResource(String name) {
    Path resource = this.resources.getRoot().resolve(name);
    if (Files.exists(resource)) {
      try {
        return resource.toUri().toURL();
      }
      catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
    return null;
  }

  @Override
  protected Enumeration<URL> findResources(String name) throws IOException {
    URL resourceUrl = findResource(name);
    return (resourceUrl != null) ? Collections.enumeration(List.of(resourceUrl)) : Collections.emptyEnumeration();
  }

}
