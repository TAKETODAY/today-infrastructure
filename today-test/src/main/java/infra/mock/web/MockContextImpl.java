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

package infra.mock.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import infra.core.io.DefaultResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.http.MediaType;
import infra.http.MediaTypeFactory;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.mock.api.MockContext;
import infra.mock.api.RequestDispatcher;
import infra.mock.api.SessionCookieConfig;
import infra.mock.api.SessionTrackingMode;
import infra.util.ClassUtils;
import infra.util.MimeType;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.mock.MockUtils;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;
import infra.web.mock.support.GenericWebApplicationContext;
import infra.web.mock.support.XmlWebApplicationContext;

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
 * @see AnnotationConfigWebApplicationContext
 * @see XmlWebApplicationContext
 * @see GenericWebApplicationContext
 * @since 4.0
 */
public class MockContextImpl implements MockContext {

  /** Default Mock name used by Tomcat, Jetty, JBoss, and GlassFish: {@value}. */
  private static final String COMMON_DEFAULT_MOCK_NAME = "default";

  private static final String TEMP_DIR_SYSTEM_PROPERTY = "java.io.tmpdir";

  private static final Set<SessionTrackingMode> DEFAULT_SESSION_TRACKING_MODES = new LinkedHashSet<>(4);

  static {
    DEFAULT_SESSION_TRACKING_MODES.add(SessionTrackingMode.COOKIE);
    DEFAULT_SESSION_TRACKING_MODES.add(SessionTrackingMode.URL);
    DEFAULT_SESSION_TRACKING_MODES.add(SessionTrackingMode.SSL);
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final ResourceLoader resourceLoader;

  private final String resourceBasePath;

  private int majorVersion = 3;

  private int minorVersion = 1;

  private int effectiveMajorVersion = 3;

  private int effectiveMinorVersion = 1;

  private final Map<String, RequestDispatcher> namedRequestDispatchers = new HashMap<>();

  private String defaultMockName = COMMON_DEFAULT_MOCK_NAME;

  private final Map<String, String> initParameters = new LinkedHashMap<>();

  private final Map<String, Object> attributes = new LinkedHashMap<>();

  private String mockContextName = "MockContext";

  private final Set<String> declaredRoles = new LinkedHashSet<>();

  @Nullable
  private Set<SessionTrackingMode> sessionTrackingModes;

  private final SessionCookieConfig sessionCookieConfig = new MockSessionCookieConfig();

  private int sessionTimeout;

  @Nullable
  private String requestCharacterEncoding;

  @Nullable
  private String responseCharacterEncoding;

  private final Map<String, MediaType> mimeTypes = new LinkedHashMap<>();

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
   * <p>Registers a {@link MockRequestDispatcher} for the Mock named
   * {@literal 'default'}.
   *
   * @param resourceBasePath the root directory of the WAR (should not end with a slash)
   * @param resourceLoader the ResourceLoader to use (or null for the default)
   * @see #registerNamedDispatcher
   */
  public MockContextImpl(String resourceBasePath, @Nullable ResourceLoader resourceLoader) {
    this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
    this.resourceBasePath = resourceBasePath;
    // Use JVM temp dir as MockContext temp dir.
    String tempDir = System.getProperty(TEMP_DIR_SYSTEM_PROPERTY);
    if (tempDir != null) {
      this.attributes.put(MockUtils.TEMP_DIR_CONTEXT_ATTRIBUTE, new File(tempDir));
    }
    registerNamedDispatcher(this.defaultMockName, new MockRequestDispatcher(this.defaultMockName));
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

  public void setMajorVersion(int majorVersion) {
    this.majorVersion = majorVersion;
  }

  @Override
  public int getMajorVersion() {
    return this.majorVersion;
  }

  public void setMinorVersion(int minorVersion) {
    this.minorVersion = minorVersion;
  }

  @Override
  public int getMinorVersion() {
    return this.minorVersion;
  }

  public void setEffectiveMajorVersion(int effectiveMajorVersion) {
    this.effectiveMajorVersion = effectiveMajorVersion;
  }

  @Override
  public int getEffectiveMajorVersion() {
    return this.effectiveMajorVersion;
  }

  public void setEffectiveMinorVersion(int effectiveMinorVersion) {
    this.effectiveMinorVersion = effectiveMinorVersion;
  }

  @Override
  public int getEffectiveMinorVersion() {
    return this.effectiveMinorVersion;
  }

  @Override
  @Nullable
  public String getMimeType(String filePath) {
    String extension = StringUtils.getFilenameExtension(filePath);
    if (this.mimeTypes.containsKey(extension)) {
      return this.mimeTypes.get(extension).toString();
    }
    else {
      return MediaTypeFactory.getMediaType(filePath).
              map(MimeType::toString)
              .orElse(null);
    }
  }

  /**
   * Adds a mime type mapping for use by {@link #getMimeType(String)}.
   *
   * @param fileExtension a file extension, such as {@code txt}, {@code gif}
   * @param mimeType the mime type
   */
  public void addMimeType(String fileExtension, MediaType mimeType) {
    Assert.notNull(fileExtension, "'fileExtension' is required");
    this.mimeTypes.put(fileExtension, mimeType);
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
  public RequestDispatcher getRequestDispatcher(String path) {
    Assert.isTrue(path.startsWith("/"),
            () -> "RequestDispatcher path [" + path + "] at MockContext level must start with '/'");
    return new MockRequestDispatcher(path);
  }

  @Override
  public RequestDispatcher getNamedDispatcher(String path) {
    return this.namedRequestDispatchers.get(path);
  }

  /**
   * Register a {@link RequestDispatcher} (typically a {@link MockRequestDispatcher})
   * that acts as a wrapper for the named Mock.
   *
   * @param name the name of the wrapped Mock
   * @param requestDispatcher the dispatcher that wraps the named Mock
   * @see #getNamedDispatcher
   * @see #unregisterNamedDispatcher
   */
  public void registerNamedDispatcher(String name, RequestDispatcher requestDispatcher) {
    Assert.notNull(name, "RequestDispatcher name is required");
    Assert.notNull(requestDispatcher, "RequestDispatcher is required");
    this.namedRequestDispatchers.put(name, requestDispatcher);
  }

  /**
   * Unregister the {@link RequestDispatcher} with the given name.
   *
   * @param name the name of the dispatcher to unregister
   * @see #getNamedDispatcher
   * @see #registerNamedDispatcher
   */
  public void unregisterNamedDispatcher(String name) {
    Assert.notNull(name, "RequestDispatcher name is required");
    this.namedRequestDispatchers.remove(name);
  }

  /**
   * Get the name of the <em>default</em> {@code Mock}.
   * <p>Defaults to {@literal 'default'}.
   *
   * @see #setDefaultMockName
   */
  public String getDefaultMockName() {
    return this.defaultMockName;
  }

  /**
   * Set the name of the <em>default</em> {@code Mock}.
   * <p>Also {@link #unregisterNamedDispatcher unregisters} the current default
   * {@link RequestDispatcher} and {@link #registerNamedDispatcher replaces}
   * it with a {@link MockRequestDispatcher} for the provided
   * {@code defaultMockName}.
   *
   * @param defaultMockName the name of the <em>default</em> {@code Mock};
   * never {@code null} or empty
   * @see #getDefaultMockName
   */
  public void setDefaultMockName(String defaultMockName) {
    Assert.hasText(defaultMockName, "defaultMockName must not be null or empty");
    unregisterNamedDispatcher(this.defaultMockName);
    this.defaultMockName = defaultMockName;
    registerNamedDispatcher(this.defaultMockName, new MockRequestDispatcher(this.defaultMockName));
  }

  @Override
  public void log(String message) {
    logger.info(message);
  }

  @Override
  public void log(String message, Throwable ex) {
    logger.info(message, ex);
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
  public String getServerInfo() {
    return "MockContext";
  }

  @Override
  public String getInitParameter(String name) {
    Assert.notNull(name, "Parameter name is required");
    return this.initParameters.get(name);
  }

  @Override
  public Enumeration<String> getInitParameterNames() {
    return Collections.enumeration(this.initParameters.keySet());
  }

  @Override
  public boolean setInitParameter(String name, String value) {
    Assert.notNull(name, "Parameter name is required");
    if (this.initParameters.containsKey(name)) {
      return false;
    }
    this.initParameters.put(name, value);
    return true;
  }

  public void addInitParameter(String name, String value) {
    Assert.notNull(name, "Parameter name is required");
    this.initParameters.put(name, value);
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

  public void setMockContextName(String mockContextName) {
    this.mockContextName = mockContextName;
  }

  @Override
  public String getMockContextName() {
    return this.mockContextName;
  }

  @Override
  @Nullable
  public ClassLoader getClassLoader() {
    return ClassUtils.getDefaultClassLoader();
  }

  public void declareRoles(String... roleNames) {
    Assert.notNull(roleNames, "Role names array is required");
    for (String roleName : roleNames) {
      Assert.hasLength(roleName, "Role name must not be empty");
      this.declaredRoles.add(roleName);
    }
  }

  public Set<String> getDeclaredRoles() {
    return Collections.unmodifiableSet(this.declaredRoles);
  }

  @Override
  public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes)
          throws IllegalStateException, IllegalArgumentException {
    this.sessionTrackingModes = sessionTrackingModes;
  }

  @Override
  public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
    return DEFAULT_SESSION_TRACKING_MODES;
  }

  @Override
  public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
    return (this.sessionTrackingModes != null ?
            Collections.unmodifiableSet(this.sessionTrackingModes) : DEFAULT_SESSION_TRACKING_MODES);
  }

  @Override
  public SessionCookieConfig getSessionCookieConfig() {
    return this.sessionCookieConfig;
  }

  @Override  // on Mock 4.0
  public void setSessionTimeout(int sessionTimeout) {
    this.sessionTimeout = sessionTimeout;
  }

  @Override  // on Mock 4.0
  public int getSessionTimeout() {
    return this.sessionTimeout;
  }

  @Override  // on Mock 4.0
  public void setRequestCharacterEncoding(@Nullable String requestCharacterEncoding) {
    this.requestCharacterEncoding = requestCharacterEncoding;
  }

  @Override  // on Mock 4.0
  @Nullable
  public String getRequestCharacterEncoding() {
    return this.requestCharacterEncoding;
  }

  @Override  // on Mock 4.0
  public void setResponseCharacterEncoding(@Nullable String responseCharacterEncoding) {
    this.responseCharacterEncoding = responseCharacterEncoding;
  }

  @Override  // on Mock 4.0
  @Nullable
  public String getResponseCharacterEncoding() {
    return this.responseCharacterEncoding;
  }

}
