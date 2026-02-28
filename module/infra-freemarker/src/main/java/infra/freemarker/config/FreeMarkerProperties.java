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

import java.util.Arrays;
import java.util.HashMap;
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
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.ui.freemarker.FreeMarkerConfigurationFactory;
import infra.ui.template.TemplateAvailabilityProvider;
import infra.ui.template.TemplateLocation;
import infra.web.view.config.AbstractTemplateViewResolverProperties;

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
public class FreeMarkerProperties extends AbstractTemplateViewResolverProperties implements InitializingBean, ApplicationContextAware {

  public static final String DEFAULT_TEMPLATE_LOADER_PATH = TemplateAvailabilityProvider.DEFAULT_TEMPLATE_LOADER_PATH;

  public static final String DEFAULT_PREFIX = FreeMarkerTemplateAvailabilityProvider.DEFAULT_PREFIX;

  public static final String DEFAULT_SUFFIX = FreeMarkerTemplateAvailabilityProvider.DEFAULT_SUFFIX;

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
    super(DEFAULT_PREFIX, DEFAULT_SUFFIX);
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
