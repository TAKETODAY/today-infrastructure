/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.loader;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.core.io.Resource;

/**
 * A test {@link ClassLoader} that can be used in a testing context to control the
 * {@code spring.components} resource that should be loaded. Can also simulate a failure
 * by throwing a configurable {@link IOException}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/9 22:00
 */
public class CandidateComponentsTestClassLoader extends ClassLoader {

  /**
   * Create a test {@link ClassLoader} that disable the use of the index, even
   * if resources are present at the standard location.
   *
   * @param classLoader the classloader to use for all other operations
   * @return a test {@link ClassLoader} that has no index
   * @see CandidateComponentsIndexLoader#COMPONENTS_RESOURCE_LOCATION
   */
  public static ClassLoader disableIndex(ClassLoader classLoader) {
    return new CandidateComponentsTestClassLoader(classLoader,
            Collections.enumeration(Collections.emptyList()));
  }

  /**
   * Create a test {@link ClassLoader} that creates an index with the
   * specified {@link Resource} instances
   *
   * @param classLoader the classloader to use for all other operations
   * @return a test {@link ClassLoader} with an index built based on the
   * specified resources.
   */
  public static ClassLoader index(ClassLoader classLoader, Resource... resources) {
    return new CandidateComponentsTestClassLoader(classLoader,
            Collections.enumeration(Stream.of(resources).map(r -> {
              try {
                return r.getLocation();
              }
              catch (Exception ex) {
                throw new IllegalArgumentException("Invalid resource " + r, ex);
              }
            }).collect(Collectors.toList())));
  }

  private final Enumeration<URL> resourceUrls;

  private final IOException cause;

  public CandidateComponentsTestClassLoader(ClassLoader classLoader, Enumeration<URL> resourceUrls) {
    super(classLoader);
    this.resourceUrls = resourceUrls;
    this.cause = null;
  }

  public CandidateComponentsTestClassLoader(ClassLoader parent, IOException cause) {
    super(parent);
    this.resourceUrls = null;
    this.cause = cause;
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    if (CandidateComponentsIndexLoader.COMPONENTS_RESOURCE_LOCATION.equals(name)) {
      if (this.resourceUrls != null) {
        return this.resourceUrls;
      }
      throw this.cause;
    }
    return super.getResources(name);
  }

}
