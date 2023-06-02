/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.ui.freemarker;

import java.io.IOException;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ResourceLoaderAware;
import cn.taketoday.lang.Nullable;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

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
 * <pre class="code"> &lt;bean id="freemarkerConfiguration" class="cn.taketoday.ui.freemarker.FreeMarkerConfigurationFactoryBean"&gt;
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
 * @see cn.taketoday.web.view.freemarker.FreeMarkerConfigurer
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

