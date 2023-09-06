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

package cn.taketoday.annotation.config.freemarker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.framework.template.AbstractTemplateViewResolverProperties;
import cn.taketoday.framework.template.TemplateLocation;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.ui.freemarker.FreeMarkerConfigurationFactory;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring FreeMarker.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "freemarker")
public class FreeMarkerProperties extends AbstractTemplateViewResolverProperties implements InitializingBean, ApplicationContextAware {

  public static final String DEFAULT_TEMPLATE_LOADER_PATH = "classpath:/templates/";

  public static final String DEFAULT_PREFIX = "";

  public static final String DEFAULT_SUFFIX = ".ftl";

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

  protected void applyTo(FreeMarkerConfigurationFactory factory) {
    factory.setTemplateLoaderPaths(getTemplateLoaderPath());
    factory.setPreferFileSystemAccess(isPreferFileSystemAccess());
    factory.setDefaultEncoding(getCharsetName());
    Properties settings = new Properties();
    settings.put("recognize_standard_file_extensions", "true");
    settings.putAll(getSettings());
    factory.setFreemarkerSettings(settings);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    checkTemplateLocationExists(this, applicationContext);
  }

  private void checkTemplateLocationExists(FreeMarkerProperties properties, ApplicationContext context) {
    Logger logger = LoggerFactory.getLogger(FreeMarkerProperties.class);
    if (logger.isWarnEnabled() && properties.isCheckTemplateLocation()) {
      for (String templateLoaderPath : properties.getTemplateLoaderPath()) {
        TemplateLocation location = new TemplateLocation(templateLoaderPath);
        if (location.exists(context)) {
          return;
        }
      }
      logger.warn("Cannot find template location(s): {} (please add some templates, "
                      + "check your FreeMarker configuration, or set freemarker.check-template-location=false)",
              Arrays.toString(properties.getTemplateLoaderPath()));
    }
  }

}
