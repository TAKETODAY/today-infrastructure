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

package infra.ui.freemarker;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.PropertiesUtils;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;

/**
 * Factory that configures a FreeMarker Configuration. Can be used standalone, but
 * typically you will either use FreeMarkerConfigurationFactoryBean for preparing a
 * Configuration as bean reference, or FreeMarkerConfigurer for web views.
 *
 * <p>The optional "configLocation" property sets the location of a FreeMarker
 * properties file, within the current application. FreeMarker properties can be
 * overridden via "freemarkerSettings". All of these properties will be set by
 * calling FreeMarker's {@code Configuration.setSettings()} method and are
 * subject to constraints set by FreeMarker.
 *
 * <p>The "freemarkerVariables" property can be used to specify a Map of
 * shared variables that will be applied to the Configuration via the
 * {@code setAllSharedVariables()} method. Like {@code setSettings()},
 * these entries are subject to FreeMarker constraints.
 *
 * <p>The simplest way to use this class is to specify a "templateLoaderPath";
 * FreeMarker does not need any further configuration then.
 *
 * <p>Note: Framework's FreeMarker support requires FreeMarker 2.3 or higher.
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setConfigLocation
 * @see #setFreemarkerSettings
 * @see #setFreemarkerVariables
 * @see #setTemplateLoaderPath
 * @see #createConfiguration
 * @see FreeMarkerConfigurationFactoryBean
 * @see infra.web.view.freemarker.FreeMarkerConfigurer
 * @see freemarker.template.Configuration
 * @since 4.0 2022/2/5 13:02
 */
public class FreeMarkerConfigurationFactory {
  private static final Logger log = LoggerFactory.getLogger(FreeMarkerConfigurationFactory.class);

  @Nullable
  private Resource configLocation;

  @Nullable
  private Properties freemarkerSettings;

  @Nullable
  private Map<String, Object> freemarkerVariables;

  @Nullable
  private String defaultEncoding;

  private final ArrayList<TemplateLoader> templateLoaders = new ArrayList<>();

  @Nullable
  private List<TemplateLoader> preTemplateLoaders;

  @Nullable
  private List<TemplateLoader> postTemplateLoaders;

  @Nullable
  private String[] templateLoaderPaths;

  private ResourceLoader resourceLoader = new DefaultResourceLoader();

  private boolean preferFileSystemAccess = true;

  /**
   * Set the location of the FreeMarker config file.
   * Alternatively, you can specify all setting locally.
   *
   * @see #setFreemarkerSettings
   * @see #setTemplateLoaderPath
   */
  public void setConfigLocation(@Nullable Resource resource) {
    this.configLocation = resource;
  }

  /**
   * Set properties that contain well-known FreeMarker keys which will be
   * passed to FreeMarker's {@code Configuration.setSettings} method.
   *
   * @see freemarker.template.Configuration#setSettings
   */
  public void setFreemarkerSettings(@Nullable Properties settings) {
    this.freemarkerSettings = settings;
  }

  /**
   * Set a Map that contains well-known FreeMarker objects which will be passed
   * to FreeMarker's {@code Configuration.setAllSharedVariables()} method.
   *
   * @see freemarker.template.Configuration#setAllSharedVariables
   */
  public void setFreemarkerVariables(@Nullable Map<String, Object> variables) {
    this.freemarkerVariables = variables;
  }

  /**
   * Set the default encoding for the FreeMarker configuration.
   * If not specified, FreeMarker will use the platform file encoding.
   * <p>Used for template rendering unless there is an explicit encoding specified
   * for the rendering process (for example, on Framework's FreeMarkerView).
   *
   * @see freemarker.template.Configuration#setDefaultEncoding
   * @see infra.web.view.freemarker.FreeMarkerView#setEncoding
   */
  public void setDefaultEncoding(@Nullable String defaultEncoding) {
    this.defaultEncoding = defaultEncoding;
  }

  /**
   * Set the {@link Charset} for the default encoding for the FreeMarker
   * {@link Configuration}, which is used to decode byte sequences to character
   * sequences when reading template files.
   * <p>See {@link #setDefaultEncoding(String)} for details.
   *
   * @see java.nio.charset.StandardCharsets
   * @since 5.0
   */
  public void setDefaultCharset(Charset defaultCharset) {
    this.defaultEncoding = defaultCharset.name();
  }

  /**
   * Set a List of {@code TemplateLoader}s that will be used to search
   * for templates. For example, one or more custom loaders such as database
   * loaders could be configured and injected here.
   * <p>The {@link TemplateLoader TemplateLoaders} specified here will be
   * registered <i>before</i> the default template loaders that this factory
   * registers (such as loaders for specified "templateLoaderPaths" or any
   * loaders registered in {@link #postProcessTemplateLoaders}).
   *
   * @see #setTemplateLoaderPaths
   * @see #postProcessTemplateLoaders
   */
  public void setPreTemplateLoaders(TemplateLoader... preTemplateLoaders) {
    this.preTemplateLoaders = Arrays.asList(preTemplateLoaders);
  }

  /**
   * Set a List of {@code TemplateLoader}s that will be used to search
   * for templates. For example, one or more custom loaders such as database
   * loaders can be configured.
   * <p>The {@link TemplateLoader TemplateLoaders} specified here will be
   * registered <i>after</i> the default template loaders that this factory
   * registers (such as loaders for specified "templateLoaderPaths" or any
   * loaders registered in {@link #postProcessTemplateLoaders}).
   *
   * @see #setTemplateLoaderPaths
   * @see #postProcessTemplateLoaders
   */
  public void setPostTemplateLoaders(TemplateLoader... postTemplateLoaders) {
    this.postTemplateLoaders = Arrays.asList(postTemplateLoaders);
  }

  /**
   * Set the Freemarker template loader path via a Framework resource location.
   * See the "templateLoaderPaths" property for details on path handling.
   *
   * @see #setTemplateLoaderPaths
   */
  public void setTemplateLoaderPath(String templateLoaderPath) {
    this.templateLoaderPaths = new String[] { templateLoaderPath };
  }

  /**
   * Set multiple Freemarker template loader paths via Framework resource locations.
   * <p>When populated via a String, standard URLs like "file:" and "classpath:"
   * pseudo URLs are supported, as understood by ResourceEditor. Allows for
   * relative paths when running in an ApplicationContext.
   * <p>Will define a path for the default FreeMarker template loader.
   * If a specified resource cannot be resolved to a {@code java.io.File},
   * a generic InfraTemplateLoader will be used, without modification detection.
   * <p>To enforce the use of InfraTemplateLoader, i.e. to not resolve a path
   * as file system resource in any case, turn off the "preferFileSystemAccess"
   * flag. See the latter's javadoc for details.
   * <p>If you wish to specify your own list of TemplateLoaders, do not set this
   * property and instead use {@code setTemplateLoaders(List templateLoaders)}
   *
   * @see infra.context.ApplicationContext#getResource
   * @see freemarker.template.Configuration#setDirectoryForTemplateLoading
   * @see InfraTemplateLoader
   */
  public void setTemplateLoaderPaths(@Nullable String... templateLoaderPaths) {
    this.templateLoaderPaths = templateLoaderPaths;
  }

  /**
   * Add multiple Freemarker template loader
   *
   * @see InfraTemplateLoader
   */
  public void addTemplateLoader(TemplateLoader... templateLoader) {
    CollectionUtils.addAll(templateLoaders, templateLoader);
  }

  /**
   * Set multiple Freemarker template loader
   *
   * @see InfraTemplateLoader
   */
  public void setTemplateLoaders(List<TemplateLoader> templateLoaders) {
    this.templateLoaders.clear();
    this.templateLoaders.addAll(templateLoaders);
  }

  /**
   * Set the Framework ResourceLoader to use for loading FreeMarker template files.
   * The default is DefaultResourceLoader. Will get overridden by the
   * ApplicationContext if running in a context.
   *
   * @see DefaultResourceLoader
   */
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  /**
   * Return the Framework ResourceLoader to use for loading FreeMarker template files.
   */
  protected ResourceLoader getResourceLoader() {
    return this.resourceLoader;
  }

  /**
   * Set whether to prefer file system access for template loading.
   * File system access enables hot detection of template changes.
   * <p>If this is enabled, FreeMarkerConfigurationFactory will try to resolve
   * the specified "templateLoaderPath" as file system resource (which will work
   * for expanded class path resources and MockContext resources too).
   * <p>Default is "true". Turn this off to always load via InfraTemplateLoader
   * (i.e. as stream, without hot detection of template changes), which might
   * be necessary if some of your templates reside in an expanded classes
   * directory while others reside in jar files.
   *
   * @see #setTemplateLoaderPath
   */
  public void setPreferFileSystemAccess(boolean preferFileSystemAccess) {
    this.preferFileSystemAccess = preferFileSystemAccess;
  }

  /**
   * Return whether to prefer file system access for template loading.
   */
  protected boolean isPreferFileSystemAccess() {
    return this.preferFileSystemAccess;
  }

  /**
   * Prepare the FreeMarker Configuration and return it.
   *
   * @return the FreeMarker Configuration object
   * @throws IOException if the config file wasn't found
   * @throws TemplateException on FreeMarker initialization failure
   */
  public Configuration createConfiguration() throws IOException, TemplateException {
    Configuration config = newConfiguration();
    Properties props = new Properties();

    // Load config file if specified.
    if (this.configLocation != null) {
      if (log.isDebugEnabled()) {
        log.debug("Loading FreeMarker configuration from {}", configLocation);
      }
      PropertiesUtils.fillProperties(props, this.configLocation);
    }

    // Merge local properties if specified.
    if (this.freemarkerSettings != null) {
      props.putAll(this.freemarkerSettings);
    }

    // FreeMarker will only accept known keys in its setSettings and
    // setAllSharedVariables methods.
    if (!props.isEmpty()) {
      config.setSettings(props);
    }

    if (CollectionUtils.isNotEmpty(this.freemarkerVariables)) {
      config.setAllSharedVariables(new SimpleHash(this.freemarkerVariables, config.getObjectWrapper()));
    }

    if (this.defaultEncoding != null) {
      config.setDefaultEncoding(this.defaultEncoding);
    }

    var templateLoaders = new ArrayList<>(this.templateLoaders);

    // Register template loaders that are supposed to kick in early.
    if (this.preTemplateLoaders != null) {
      templateLoaders.addAll(this.preTemplateLoaders);
    }

    // Register default template loaders.
    if (this.templateLoaderPaths != null) {
      for (String path : this.templateLoaderPaths) {
        templateLoaders.add(getTemplateLoaderForPath(path));
      }
    }
    postProcessTemplateLoaders(templateLoaders);

    // Register template loaders that are supposed to kick in late.
    if (this.postTemplateLoaders != null) {
      templateLoaders.addAll(this.postTemplateLoaders);
    }

    TemplateLoader loader = getAggregateTemplateLoader(templateLoaders);
    if (loader != null) {
      config.setTemplateLoader(loader);
    }

    postProcessConfiguration(config);
    return config;
  }

  /**
   * Return a new Configuration object. Subclasses can override this for custom
   * initialization (e.g. specifying a FreeMarker compatibility level which is a
   * new feature in FreeMarker 2.3.21), or for using a mock object for testing.
   * <p>Called by {@code createConfiguration()}.
   *
   * @return the Configuration object
   * @throws IOException if a config file wasn't found
   * @throws TemplateException on FreeMarker initialization failure
   * @see #createConfiguration()
   */
  protected Configuration newConfiguration() throws IOException, TemplateException {
    return new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
  }

  /**
   * Determine a FreeMarker TemplateLoader for the given path.
   * <p>Default implementation creates either a FileTemplateLoader or
   * a InfraTemplateLoader.
   *
   * @param templateLoaderPath the path to load templates from
   * @return an appropriate TemplateLoader
   * @see freemarker.cache.FileTemplateLoader
   * @see InfraTemplateLoader
   */
  protected TemplateLoader getTemplateLoaderForPath(String templateLoaderPath) {
    if (isPreferFileSystemAccess()) {
      // Try to load via the file system, fall back to InfraTemplateLoader
      // (for hot detection of template changes, if possible).
      try {
        Resource path = getResourceLoader().getResource(templateLoaderPath);
        File file = path.getFile();  // will fail if not resolvable in the file system
        if (log.isDebugEnabled()) {
          log.debug("Template loader path [{}] resolved to file path [{}]", path, file.getAbsolutePath());
        }
        return new FileTemplateLoader(file);
      }
      catch (Exception ex) {
        if (log.isDebugEnabled()) {
          log.debug("Cannot resolve template loader path [{}] to [java.io.File]: using InfraTemplateLoader as fallback",
                  templateLoaderPath, ex);
        }
        return new InfraTemplateLoader(getResourceLoader(), templateLoaderPath);
      }
    }
    else {
      // Always load via InfraTemplateLoader (without hot detection of template changes).
      log.debug("File system access not preferred: using InfraTemplateLoader");
      return new InfraTemplateLoader(getResourceLoader(), templateLoaderPath);
    }
  }

  /**
   * To be overridden by subclasses that want to register custom
   * TemplateLoader instances after this factory created its default
   * template loaders.
   * <p>Called by {@code createConfiguration()}. Note that specified
   * "postTemplateLoaders" will be registered <i>after</i> any loaders
   * registered by this callback; as a consequence, they are <i>not</i>
   * included in the given List.
   *
   * @param templateLoaders the current List of TemplateLoader instances,
   * to be modified by a subclass
   * @see #createConfiguration()
   * @see #setPostTemplateLoaders
   */
  protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {

  }

  /**
   * Return a TemplateLoader based on the given TemplateLoader list.
   * If more than one TemplateLoader has been registered, a FreeMarker
   * MultiTemplateLoader needs to be created.
   *
   * @param templateLoaders the final List of TemplateLoader instances
   * @return the aggregate TemplateLoader
   */
  @Nullable
  protected TemplateLoader getAggregateTemplateLoader(List<TemplateLoader> templateLoaders) {
    switch (templateLoaders.size()) {
      case 0 -> {
        log.debug("No FreeMarker TemplateLoaders specified");
        return null;
      }
      case 1 -> {
        return templateLoaders.get(0);
      }
      default -> {
        TemplateLoader[] loaders = templateLoaders.toArray(new TemplateLoader[0]);
        return new MultiTemplateLoader(loaders);
      }
    }
  }

  /**
   * To be overridden by subclasses that want to perform custom
   * post-processing of the Configuration object after this factory
   * performed its default initialization.
   * <p>Called by {@code createConfiguration()}.
   *
   * @param config the current Configuration object
   * @throws IOException if a config file wasn't found
   * @throws TemplateException on FreeMarker initialization failure
   * @see #createConfiguration()
   */
  protected void postProcessConfiguration(Configuration config) throws IOException, TemplateException {

  }

}
