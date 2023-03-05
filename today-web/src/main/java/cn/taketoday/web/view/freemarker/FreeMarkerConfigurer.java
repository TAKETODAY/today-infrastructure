/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.view.freemarker;

import java.io.IOException;
import java.util.List;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ResourceLoaderAware;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * JavaBean to configure FreeMarker for web usage, via the "configLocation"
 * and/or "freemarkerSettings" and/or "templateLoaderPath" properties.
 * The simplest way to use this class is to specify just a "templateLoaderPath";
 * you do not need any further configuration then.
 *
 * <pre class="code">
 * &lt;bean id="freemarkerConfig" class="cn.taketoday.web.view.freemarker.FreeMarkerConfigurer"&gt;
 *   &lt;property name="templateLoaderPath"&gt;&lt;value&gt;/WEB-INF/freemarker/&lt;/value&gt;&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * This bean must be included in the application context of any application
 * using Framework's FreeMarkerView for web MVC. It exists purely to configure FreeMarker.
 * It is not meant to be referenced by application components but just internally
 * by FreeMarkerView. Implements FreeMarkerConfig to be found by FreeMarkerView without
 * depending on the bean name of the configurer. Each DispatcherServlet can define its
 * own FreeMarkerConfigurer if desired.
 *
 * <p>Note that you can also refer to a preconfigured FreeMarker Configuration
 * instance, for example one set up by FreeMarkerConfigurationFactoryBean, via
 * the "configuration" property. This allows to share a FreeMarker Configuration
 * for web and email usage, for example.
 *
 *
 * Note: Framework's FreeMarker support requires FreeMarker 2.3 or higher.
 *
 * @author Darren Davison
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setConfigLocation
 * @see #setFreemarkerSettings
 * @see #setTemplateLoaderPath
 * @see #setConfiguration
 * @see cn.taketoday.web.view.freemarker.FreeMarkerConfigurationFactoryBean
 * @see FreeMarkerView
 * @since 4.0
 */
public class FreeMarkerConfigurer extends FreeMarkerConfigurationFactory
        implements FreeMarkerConfig, InitializingBean, ResourceLoaderAware {

  @Nullable
  private Configuration configuration;

  /**
   * Set a preconfigured Configuration to use for the FreeMarker web config, e.g. a
   * shared one for web and email usage, set up via FreeMarkerConfigurationFactoryBean.
   * If this is not set, FreeMarkerConfigurationFactory's properties (inherited by
   * this class) have to be specified.
   *
   * @see FreeMarkerConfigurationFactoryBean
   */
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Initialize FreeMarkerConfigurationFactory's Configuration
   * if not overridden by a preconfigured FreeMarker Configuration.
   * <p>Sets up a ClassTemplateLoader to use for loading Framework macros.
   *
   * @see #createConfiguration
   * @see #setConfiguration
   */
  @Override
  public void afterPropertiesSet() throws IOException, TemplateException {
    if (this.configuration == null) {
      this.configuration = createConfiguration();
    }
  }

  /**
   * This implementation registers an additional ClassTemplateLoader
   * for the Framework-provided macros, added to the end of the list.
   */
  @Override
  protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
    templateLoaders.add(new ClassTemplateLoader(FreeMarkerConfigurer.class, ""));
  }

  /**
   * Return the Configuration object wrapped by this bean.
   */
  @Override
  public Configuration getConfiguration() {
    Assert.state(this.configuration != null, "No Configuration available");
    return this.configuration;
  }

}
