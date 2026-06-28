/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.mock.web;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import infra.core.io.DefaultResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.mock.api.MockContext;
import infra.util.ObjectUtils;
import infra.web.mock.MockUtils;

/**
 * Mock implementation of the {@link MockContext} interface.
 *
 * <p>For setting up a full {@code WebApplicationContext} in a test environment, you can
 * use {@code AnnotationConfigWebApplicationContext}, {@code XmlWebApplicationContext},
 * or {@code GenericWebApplicationContext}, passing in a corresponding
 * {@code MockContext} instance. Consider configuring your
 * {@code MockContext} with a {@code FileSystemResourceLoader} in order to
 * interpret resource paths as relative filesystem locations.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #MockContextImpl(ResourceLoader)
 * @since 4.0
 */
public class MockContextImpl implements MockContext {

  private static final String TEMP_DIR_SYSTEM_PROPERTY = "java.io.tmpdir";

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final ResourceLoader resourceLoader;

  private final String resourceBasePath;

  private final Map<String, Object> attributes = new LinkedHashMap<>();

  /**
   * Create a new {@code MockContext}, using no base path and a
   * {@link DefaultResourceLoader} (i.e. the classpath root as WAR root).
   */
  public MockContextImpl() {
    this("", null);
  }

  /**
   * Create a new {@code MockContext}, using a {@link DefaultResourceLoader}.
   *
   * @param resourceBasePath the root directory of the WAR (should not end with a slash)
   */
  public MockContextImpl(String resourceBasePath) {
    this(resourceBasePath, null);
  }

  /**
   * Create a new {@code MockContext}, using the specified {@link ResourceLoader}
   * and no base path.
   *
   * @param resourceLoader the ResourceLoader to use (or null for the default)
   */
  public MockContextImpl(@Nullable ResourceLoader resourceLoader) {
    this("", resourceLoader);
  }

  /**
   * Create a new {@code MockContext} using the supplied resource base
   * path and resource loader.
   *
   * @param resourceBasePath the root directory of the WAR (should not end with a slash)
   * @param resourceLoader the ResourceLoader to use (or null for the default)
   */
  public MockContextImpl(String resourceBasePath, @Nullable ResourceLoader resourceLoader) {
    this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
    this.resourceBasePath = resourceBasePath;
    // Use JVM temp dir as MockContext temp dir.
    String tempDir = System.getProperty(TEMP_DIR_SYSTEM_PROPERTY);
    if (tempDir != null) {
      this.attributes.put(MockUtils.TEMP_DIR_CONTEXT_ATTRIBUTE, new File(tempDir));
    }
  }

  /**
   * Build a full resource location for the given path, prepending the resource
   * base path of this {@code MockContext}.
   *
   * @param path the path as specified
   * @return the full resource path
   */
  protected String getResourceLocation(String path) {
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    return this.resourceBasePath + path;
  }

  @Override
  @Nullable
  public Set<String> getResourcePaths(String path) {
    String actualPath = (path.endsWith("/") ? path : path + "/");
    String resourceLocation = getResourceLocation(actualPath);
    Resource resource = null;
    try {
      resource = this.resourceLoader.getResource(resourceLocation);
      File file = resource.getFile();
      String[] fileList = file.list();
      if (ObjectUtils.isEmpty(fileList)) {
        return null;
      }
      Set<String> resourcePaths = new LinkedHashSet<>(fileList.length);
      for (String fileEntry : fileList) {
        String resultPath = actualPath + fileEntry;
        if (resource.createRelative(fileEntry).getFile().isDirectory()) {
          resultPath += "/";
        }
        resourcePaths.add(resultPath);
      }
      return resourcePaths;
    }
    catch (InvalidPathException | IOException ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Could not get resource paths for " +
                (resource != null ? resource : resourceLocation), ex);
      }
      return null;
    }
  }

  @Override
  @Nullable
  public URL getResource(String path) throws MalformedURLException {
    String resourceLocation = getResourceLocation(path);
    Resource resource = null;
    try {
      resource = this.resourceLoader.getResource(resourceLocation);
      if (!resource.exists()) {
        return null;
      }
      return resource.getURL();
    }
    catch (MalformedURLException ex) {
      throw ex;
    }
    catch (InvalidPathException | IOException ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Could not get URL for resource " +
                (resource != null ? resource : resourceLocation), ex);
      }
      return null;
    }
  }

  @Override
  @Nullable
  public InputStream getResourceAsStream(String path) {
    String resourceLocation = getResourceLocation(path);
    Resource resource = null;
    try {
      resource = this.resourceLoader.getResource(resourceLocation);
      if (!resource.exists()) {
        return null;
      }
      return resource.getInputStream();
    }
    catch (InvalidPathException | IOException ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Could not open InputStream for resource " +
                (resource != null ? resource : resourceLocation), ex);
      }
      return null;
    }
  }

  @Override
  @Nullable
  public String getRealPath(String path) {
    String resourceLocation = getResourceLocation(path);
    Resource resource = null;
    try {
      resource = this.resourceLoader.getResource(resourceLocation);
      return resource.getFile().getAbsolutePath();
    }
    catch (InvalidPathException | IOException ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Could not determine real path of resource " +
                (resource != null ? resource : resourceLocation), ex);
      }
      return null;
    }
  }

  @Override
  @Nullable
  public Object getAttribute(String name) {
    Assert.notNull(name, "Attribute name is required");
    return this.attributes.get(name);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(new LinkedHashSet<>(this.attributes.keySet()));
  }

  @Override
  public void setAttribute(String name, @Nullable Object value) {
    Assert.notNull(name, "Attribute name is required");
    if (value != null) {
      this.attributes.put(name, value);
    }
    else {
      this.attributes.remove(name);
    }
  }

  @Override
  public void removeAttribute(String name) {
    Assert.notNull(name, "Attribute name is required");
    this.attributes.remove(name);
  }

}
