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

package infra.app.test.mock.web;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import infra.core.ApplicationTemp;
import infra.core.io.FileSystemResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.mock.web.MockContextImpl;

/**
 * {@link MockContextImpl} implementation for Infra. Respects well-known Infra
 * Boot resource locations and uses an empty directory for "/" if no locations can be
 * found.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class InfraMockContext extends MockContextImpl {

  private static final String[] RESOURCE_LOCATIONS = new String[] {
          "classpath:META-INF/resources",
          "classpath:resources", "classpath:static", "classpath:public"
  };

  private final ResourceLoader resourceLoader;

  private File emptyRootDirectory;

  public InfraMockContext(String resourceBasePath) {
    this(resourceBasePath, new FileSystemResourceLoader());
  }

  public InfraMockContext(String resourceBasePath, ResourceLoader resourceLoader) {
    super(resourceBasePath, resourceLoader);
    this.resourceLoader = resourceLoader;
  }

  @Override
  protected String getResourceLocation(String path) {
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    String resourceLocation = getResourceBasePathLocation(path);
    if (exists(resourceLocation)) {
      return resourceLocation;
    }
    for (String prefix : RESOURCE_LOCATIONS) {
      resourceLocation = prefix + path;
      if (exists(resourceLocation)) {
        return resourceLocation;
      }
    }
    return super.getResourceLocation(path);
  }

  protected final String getResourceBasePathLocation(String path) {
    return super.getResourceLocation(path);
  }

  private boolean exists(String resourceLocation) {
    try {
      Resource resource = this.resourceLoader.getResource(resourceLocation);
      return resource.exists();
    }
    catch (Exception ex) {
      return false;
    }
  }

  @Override
  public URL getResource(String path) throws MalformedURLException {
    URL resource = super.getResource(path);
    if (resource == null && "/".equals(path)) {
      // Liquibase assumes that "/" always exists, if we don't have a directory
      // use a temporary location.
      try {
        if (this.emptyRootDirectory == null) {
          synchronized(this) {
            File tempDirectory = ApplicationTemp.createDirectory("spr-mock").toFile();
            tempDirectory.deleteOnExit();
            this.emptyRootDirectory = tempDirectory;
          }
        }
        return this.emptyRootDirectory.toURI().toURL();
      }
      catch (IOException ex) {
        // Ignore
      }
    }
    return resource;
  }

}
