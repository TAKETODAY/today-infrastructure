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

import java.io.IOException;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.InitializingBean;
import infra.context.ResourceLoaderAware;
import infra.lang.Nullable;

/**
 * Factory bean that creates a FreeMarker Configuration and provides it as
 * bean reference. This bean is intended for any kind of usage of FreeMarker
 * in application code, e.g. for generating email content. For web views,
 * FreeMarkerConfigurer is used to set up a FreeMarkerConfigurationFactory.
 *
 * The simplest way to use this class is to specify just a "templateLoaderPath";
 * you do not need any further configuration then. For example, in a web
 * application context:
 *
 * <pre class="code"> &lt;bean id="freemarkerConfiguration" class="infra.ui.freemarker.FreeMarkerConfigurationFactoryBean"&gt;
 *   &lt;property name="templateLoaderPath" value="/WEB-INF/freemarker/"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * See the base class FreeMarkerConfigurationFactory for configuration details.
 *
 * <p>Note: requires FreeMarker 2.3 or higher.
 *
 * @author Darren Davison
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setConfigLocation
 * @see #setFreemarkerSettings
 * @see #setTemplateLoaderPath
 * @see infra.web.view.freemarker.FreeMarkerConfigurer
 * @since 4.0 2022/2/5 13:03
 */
public class FreeMarkerConfigurationFactoryBean extends FreeMarkerConfigurationFactory
        implements FactoryBean<Configuration>, InitializingBean, ResourceLoaderAware {

  @Nullable
  private Configuration configuration;

  @Override
  public void afterPropertiesSet() throws IOException, TemplateException {
    this.configuration = createConfiguration();
  }

  @Override
  @Nullable
  public Configuration getObject() {
    return this.configuration;
  }

  @Override
  public Class<? extends Configuration> getObjectType() {
    return Configuration.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}

