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

package infra.freemarker.config;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import infra.beans.BeansException;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.properties.ConfigurationProperties;
import infra.freemarker.FreeMarkerTemplateAvailabilityProvider;
import infra.lang.Constant;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.ui.freemarker.FreeMarkerConfigurationFactory;
import infra.ui.template.TemplateAvailabilityProvider;
import infra.ui.template.TemplateLocation;
import infra.util.MimeType;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring FreeMarker.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableDependencyInjection
@ConfigurationProperties(prefix = "freemarker")
public class FreeMarkerProperties implements InitializingBean, ApplicationContextAware {

  public static final String DEFAULT_TEMPLATE_LOADER_PATH = TemplateAvailabilityProvider.DEFAULT_TEMPLATE_LOADER_PATH;

  public static final String DEFAULT_PREFIX = FreeMarkerTemplateAvailabilityProvider.DEFAULT_PREFIX;

  public static final String DEFAULT_SUFFIX = FreeMarkerTemplateAvailabilityProvider.DEFAULT_SUFFIX;

  public static final String DEFAULT_HTTP_CONTEXT_ATTRIBUTE = "request";

  private static final MimeType DEFAULT_CONTENT_TYPE = MimeType.TEXT_HTML;

  /**
   * Whether to enable view resolution for this technology.
   */
  private boolean enabled = true;

  /**
   * Whether to enable template caching.
   */
  private boolean cache;

  /**
   * Content-Type value.
   */
  private MimeType contentType = DEFAULT_CONTENT_TYPE;

  /**
   * Template encoding.
   */
  private Charset charset = Constant.DEFAULT_CHARSET;

  /**
   * View names that can be resolved.
   */
  private String @Nullable [] viewNames;

  /**
   * Whether to check that the templates location exists.
   */
  private boolean checkTemplateLocation = true;

  /**
   * Prefix that gets prepended to view names when building a URL.
   */
  private String prefix;

  /**
   * Suffix that gets appended to view names when building a URL.
   */
  private String suffix;

  /**
   * Name of the HttpContext attribute for all views.
   */
  private @Nullable String httpContextAttribute = DEFAULT_HTTP_CONTEXT_ATTRIBUTE;

  /**
   * Whether all request attributes should be added to the model prior to merging with
   * the template.
   */
  private boolean exposeRequestAttributes = false;

  /**
   * Whether all HttpSession attributes should be added to the model prior to merging
   * with the template.
   */
  private boolean exposeSessionAttributes = false;

  /**
   * Whether MockRequest attributes are allowed to override (hide) controller
   * generated model attributes of the same name.
   */
  private boolean allowRequestOverride = false;

  /**
   * Whether HttpSession attributes are allowed to override (hide) controller generated
   * model attributes of the same name.
   */
  private boolean allowSessionOverride = false;

  /**
   * Well-known FreeMarker keys which are passed to FreeMarker's Configuration.
   */
  private Map<String, String> settings = new HashMap<>();

  /**
   * Comma-separated list of template paths.
   */
  private String[] templateLoaderPath = new String[] { DEFAULT_TEMPLATE_LOADER_PATH };

  /**
   * Whether to prefer file system access for template loading to enable hot detection
   * of template changes. When a template path is detected as a directory, templates are
   * loaded from the directory only and other matching classpath locations will not be
   * considered.
   */
  private boolean preferFileSystemAccess;

  private ApplicationContext applicationContext;

  public FreeMarkerProperties() {
    this(DEFAULT_PREFIX, DEFAULT_SUFFIX);
  }

  protected FreeMarkerProperties(String defaultPrefix, String defaultSuffix) {
    this.prefix = defaultPrefix;
    this.suffix = defaultSuffix;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void setCheckTemplateLocation(boolean checkTemplateLocation) {
    this.checkTemplateLocation = checkTemplateLocation;
  }

  public boolean isCheckTemplateLocation() {
    return this.checkTemplateLocation;
  }

  public String @Nullable [] getViewNames() {
    return this.viewNames;
  }

  public void setViewNames(String @Nullable ... viewNames) {
    this.viewNames = viewNames;
  }

  public boolean isCache() {
    return this.cache;
  }

  public void setCache(boolean cache) {
    this.cache = cache;
  }

  public MimeType getContentType() {
    if (this.contentType.getCharset() == null) {
      Map<String, String> parameters = new LinkedHashMap<>();
      parameters.put("charset", this.charset.name());
      parameters.putAll(this.contentType.getParameters());
      return new MimeType(this.contentType, parameters);
    }
    return this.contentType;
  }

  public void setContentType(@Nullable MimeType contentType) {
    this.contentType = contentType == null ? DEFAULT_CONTENT_TYPE : contentType;
  }

  public Charset getCharset() {
    return this.charset;
  }

  public String getCharsetName() {
    return this.charset.name();
  }

  public void setCharset(@Nullable Charset charset) {
    this.charset = charset == null ? Constant.DEFAULT_CHARSET : charset;
  }

  public Map<String, String> getSettings() {
    return this.settings;
  }

  public void setSettings(Map<String, String> settings) {
    this.settings = settings;
  }

  public String[] getTemplateLoaderPath() {
    return this.templateLoaderPath;
  }

  public boolean isPreferFileSystemAccess() {
    return this.preferFileSystemAccess;
  }

  public void setPreferFileSystemAccess(boolean preferFileSystemAccess) {
    this.preferFileSystemAccess = preferFileSystemAccess;
  }

  public void setTemplateLoaderPath(String... templateLoaderPaths) {
    this.templateLoaderPath = templateLoaderPaths;
  }

  protected void applyTo(FreeMarkerConfigurationFactory factory, List<FreeMarkerVariablesCustomizer> variablesCustomizers) {
    factory.setTemplateLoaderPaths(getTemplateLoaderPath());
    factory.setPreferFileSystemAccess(isPreferFileSystemAccess());
    factory.setDefaultEncoding(getCharsetName());
    factory.setFreemarkerSettings(createFreeMarkerSettings());
    factory.setFreemarkerVariables(createFreeMarkerVariables(variablesCustomizers));
  }

  private Properties createFreeMarkerSettings() {
    Properties settings = new Properties();
    settings.put("recognize_standard_file_extensions", "true");
    settings.putAll(getSettings());
    return settings;
  }

  private Map<String, Object> createFreeMarkerVariables(List<FreeMarkerVariablesCustomizer> variablesCustomizers) {
    Map<String, Object> variables = new HashMap<>();
    for (FreeMarkerVariablesCustomizer customizer : variablesCustomizers) {
      customizer.customizeFreeMarkerVariables(variables);
    }
    return variables;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    checkTemplateLocationExists(applicationContext);
  }

  public String getPrefix() {
    return this.prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getSuffix() {
    return this.suffix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  public @Nullable String getHttpContextAttribute() {
    return this.httpContextAttribute;
  }

  public void setHttpContextAttribute(@Nullable String httpContextAttribute) {
    this.httpContextAttribute = httpContextAttribute;
  }

  public boolean isExposeRequestAttributes() {
    return this.exposeRequestAttributes;
  }

  public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
    this.exposeRequestAttributes = exposeRequestAttributes;
  }

  public boolean isExposeSessionAttributes() {
    return this.exposeSessionAttributes;
  }

  public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
    this.exposeSessionAttributes = exposeSessionAttributes;
  }

  public boolean isAllowRequestOverride() {
    return this.allowRequestOverride;
  }

  public void setAllowRequestOverride(boolean allowRequestOverride) {
    this.allowRequestOverride = allowRequestOverride;
  }

  public boolean isAllowSessionOverride() {
    return this.allowSessionOverride;
  }

  public void setAllowSessionOverride(boolean allowSessionOverride) {
    this.allowSessionOverride = allowSessionOverride;
  }

  private void checkTemplateLocationExists(ApplicationContext context) {
    Logger logger = LoggerFactory.getLogger(FreeMarkerProperties.class);
    if (logger.isWarnEnabled() && isCheckTemplateLocation()) {
      for (String templateLoaderPath : getTemplateLoaderPath()) {
        TemplateLocation location = new TemplateLocation(templateLoaderPath);
        if (location.exists(context)) {
          return;
        }
      }
      logger.warn("Cannot find template location(s): {} (please add some templates, "
                      + "check your FreeMarker configuration, or set freemarker.check-template-location=false)",
              Arrays.toString(getTemplateLoaderPath()));
    }
  }

}
