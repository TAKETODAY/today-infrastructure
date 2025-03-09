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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import infra.lang.Nullable;

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

  @Nullable
  @Override
  public URL getResource(String name) {
    Resource resource = this.resources.find(name);
    return (resource != null) ? urlOf(resource) : getParent().getResource(name);
  }

  private URL urlOf(Resource resource) {
    try {
      return resource.path().toUri().toURL();
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    Resource resource = this.resources.find(name);
    ArrayList<URL> urls = new ArrayList<>();
    if (resource != null) {
      URL resourceUrl = urlOf(resource);
      urls.add(resourceUrl);
    }
    if (resource == null || resource.additional()) {
      urls.addAll(Collections.list(getParent().getResources(name)));
    }
    return Collections.enumeration(urls);
  }

}
