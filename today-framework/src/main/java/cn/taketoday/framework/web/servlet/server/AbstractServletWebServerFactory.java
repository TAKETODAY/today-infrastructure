/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.servlet.server;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.framework.web.server.AbstractConfigurableWebServerFactory;
import cn.taketoday.framework.web.server.MimeMappings;
import cn.taketoday.framework.web.servlet.ServletContextInitializer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.session.config.CookieProperties;
import cn.taketoday.session.config.SessionProperties;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.PropertyMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;

/**
 * Abstract base class for {@link ConfigurableServletWebServerFactory} implementations.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Ivan Sopov
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractServletWebServerFactory extends AbstractConfigurableWebServerFactory
        implements ConfigurableServletWebServerFactory {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private String contextPath = "";

  private String displayName;

  private SessionProperties session = new SessionProperties();

  private boolean registerDefaultServlet = false;

  private MimeMappings mimeMappings = MimeMappings.lazyCopy(MimeMappings.DEFAULT);

  private List<ServletContextInitializer> initializers = new ArrayList<>();

  @Nullable
  private JspProperties jspConfig = new JspProperties();

  private Map<Locale, Charset> localeCharsetMappings = new HashMap<>();

  private Map<String, String> initParameters = Collections.emptyMap();

  private List<CookieSameSiteSupplier> cookieSameSiteSuppliers = new ArrayList<>();

  private final DocumentRoot documentRoot = new DocumentRoot(logger);

  private final StaticResourceJars staticResourceJars = new StaticResourceJars();

  private final Set<String> webListenerClassNames = new HashSet<>();

  /**
   * Create a new {@link AbstractServletWebServerFactory} instance.
   */
  public AbstractServletWebServerFactory() { }

  /**
   * Create a new {@link AbstractServletWebServerFactory} instance with the specified
   * port.
   *
   * @param port the port number for the web server
   */
  public AbstractServletWebServerFactory(int port) {
    super(port);
  }

  /**
   * Create a new {@link AbstractServletWebServerFactory} instance with the specified
   * context path and port.
   *
   * @param contextPath the context path for the web server
   * @param port the port number for the web server
   */
  public AbstractServletWebServerFactory(String contextPath, int port) {
    super(port);
    checkContextPath(contextPath);
    this.contextPath = contextPath;
  }

  /**
   * Returns the context path for the web server. The path will start with "/" and not
   * end with "/". The root context is represented by an empty string.
   *
   * @return the context path
   */
  public String getContextPath() {
    return this.contextPath;
  }

  @Override
  public void setContextPath(String contextPath) {
    checkContextPath(contextPath);
    this.contextPath = contextPath;
  }

  private void checkContextPath(String contextPath) {
    Assert.notNull(contextPath, "ContextPath must not be null");
    if (!contextPath.isEmpty()) {
      if ("/".equals(contextPath)) {
        throw new IllegalArgumentException("Root ContextPath must be specified using an empty string");
      }
      if (!contextPath.startsWith("/") || contextPath.endsWith("/")) {
        throw new IllegalArgumentException("ContextPath must start with '/' and not end with '/'");
      }
    }
  }

  public String getDisplayName() {
    return this.displayName;
  }

  @Override
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Flag to indicate that the default servlet should be registered.
   *
   * @return true if the default servlet is to be registered
   */
  public boolean isRegisterDefaultServlet() {
    return this.registerDefaultServlet;
  }

  @Override
  public void setRegisterDefaultServlet(boolean registerDefaultServlet) {
    this.registerDefaultServlet = registerDefaultServlet;
  }

  /**
   * Returns the mime-type mappings.
   *
   * @return the mimeMappings the mime-type mappings.
   */
  public MimeMappings getMimeMappings() {
    return this.mimeMappings;
  }

  @Override
  public void setMimeMappings(MimeMappings mimeMappings) {
    this.mimeMappings = new MimeMappings(mimeMappings);
  }

  /**
   * Returns the document root which will be used by the web context to serve static
   * files.
   *
   * @return the document root
   */
  @Nullable
  public File getDocumentRoot() {
    return this.documentRoot.getDirectory();
  }

  @Override
  public void setDocumentRoot(@Nullable File documentRoot) {
    this.documentRoot.setDirectory(documentRoot);
  }

  @Override
  public void setInitializers(List<? extends ServletContextInitializer> initializers) {
    Assert.notNull(initializers, "Initializers must not be null");
    this.initializers = new ArrayList<>(initializers);
  }

  @Override
  public void addInitializers(ServletContextInitializer... initializers) {
    Assert.notNull(initializers, "Initializers must not be null");
    CollectionUtils.addAll(this.initializers, initializers);
  }

  @Nullable
  public JspProperties getJsp() {
    return this.jspConfig;
  }

  @Override
  public void setJsp(@Nullable JspProperties jsp) {
    this.jspConfig = jsp;
  }

  public SessionProperties getSession() {
    return this.session;
  }

  @Override
  public void setSession(SessionProperties session) {
    Assert.notNull(session, "SessionProperties is required");
    this.session = session;
  }

  /**
   * Return the Locale to Charset mappings.
   *
   * @return the charset mappings
   */
  public Map<Locale, Charset> getLocaleCharsetMappings() {
    return this.localeCharsetMappings;
  }

  @Override
  public void setLocaleCharsetMappings(Map<Locale, Charset> localeCharsetMappings) {
    Assert.notNull(localeCharsetMappings, "localeCharsetMappings must not be null");
    this.localeCharsetMappings = localeCharsetMappings;
  }

  @Override
  public void setInitParameters(Map<String, String> initParameters) {
    this.initParameters = initParameters;
  }

  public Map<String, String> getInitParameters() {
    return this.initParameters;
  }

  @Override
  public void setCookieSameSiteSuppliers(List<? extends CookieSameSiteSupplier> cookieSameSiteSuppliers) {
    Assert.notNull(cookieSameSiteSuppliers, "CookieSameSiteSuppliers must not be null");
    this.cookieSameSiteSuppliers = new ArrayList<>(cookieSameSiteSuppliers);
  }

  @Override
  public void addCookieSameSiteSuppliers(CookieSameSiteSupplier... cookieSameSiteSuppliers) {
    Assert.notNull(cookieSameSiteSuppliers, "CookieSameSiteSuppliers must not be null");
    CollectionUtils.addAll(this.cookieSameSiteSuppliers, cookieSameSiteSuppliers);
  }

  public List<CookieSameSiteSupplier> getCookieSameSiteSuppliers() {
    return this.cookieSameSiteSuppliers;
  }

  /**
   * Utility method that can be used by subclasses wishing to combine the specified
   * {@link ServletContextInitializer} parameters with those defined in this instance.
   *
   * @param initializers the initializers to merge
   * @return a complete set of merged initializers (with the specified parameters
   * appearing first)
   */
  protected final ServletContextInitializer[] mergeInitializers(ServletContextInitializer... initializers) {
    ArrayList<ServletContextInitializer> mergedInitializers = new ArrayList<>();
    mergedInitializers.add(servletContext -> initParameters.forEach(servletContext::setInitParameter));
    mergedInitializers.add(new SessionConfiguringInitializer(session));
    CollectionUtils.addAll(mergedInitializers, initializers);
    mergedInitializers.addAll(this.initializers);
    return mergedInitializers.toArray(new ServletContextInitializer[0]);
  }

  /**
   * Returns whether or not the JSP servlet should be registered with the web server.
   *
   * @return {@code true} if the servlet should be registered, otherwise {@code false}
   */
  protected boolean shouldRegisterJspServlet() {
    return jspConfig != null && jspConfig.getRegistered()
            && ClassUtils.isPresent(jspConfig.getClassName(), getClass().getClassLoader());
  }

  /**
   * Returns the absolute document root when it points to a valid directory, logging a
   * warning and returning {@code null} otherwise.
   *
   * @return the valid document root
   */
  @Nullable
  protected final File getValidDocumentRoot() {
    return this.documentRoot.getValidDirectory();
  }

  protected final List<URL> getUrlsOfJarsWithMetaInfResources() {
    return this.staticResourceJars.getUrls();
  }

  protected final File getValidSessionStoreDir() {
    return getValidSessionStoreDir(true);
  }

  protected final File getValidSessionStoreDir(boolean mkdirs) {
    return session.getValidStoreDir(getApplicationTemp(), mkdirs);
  }

  @Override
  public void addWebListeners(String... webListenerClassNames) {
    CollectionUtils.addAll(this.webListenerClassNames, webListenerClassNames);
  }

  protected final Set<String> getWebListenerClassNames() {
    return this.webListenerClassNames;
  }

  /**
   * {@link ServletContextInitializer} to apply appropriate parts of the {@link SessionProperties}
   * configuration.
   */
  private record SessionConfiguringInitializer(SessionProperties session) implements ServletContextInitializer {

    @Override
    public void onStartup(ServletContext servletContext) {
      if (CollectionUtils.isNotEmpty(session.getTrackingModes())) {
        servletContext.setSessionTrackingModes(unwrap(session.getTrackingModes()));
      }
      configureSessionCookie(servletContext.getSessionCookieConfig());
    }

    private void configureSessionCookie(SessionCookieConfig config) {
      CookieProperties cookie = session.getCookie();
      PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
      map.from(cookie::getName).to(config::setName);
      map.from(cookie::getDomain).to(config::setDomain);
      map.from(cookie::getPath).to(config::setPath);
      map.from(cookie::getHttpOnly).to(config::setHttpOnly);
      map.from(cookie::getSecure).to(config::setSecure);
      map.from(cookie::getMaxAge).asInt(Duration::getSeconds).to(config::setMaxAge);
    }

    @Nullable
    private Set<SessionTrackingMode> unwrap(@Nullable Set<cn.taketoday.session.config.SessionTrackingMode> modes) {
      if (CollectionUtils.isEmpty(modes)) {
        return null;
      }

      return modes.stream()
              .map(Enum::name)
              .map(SessionTrackingMode::valueOf)
              .collect(Collectors.toCollection(LinkedHashSet::new));
    }

  }

}
