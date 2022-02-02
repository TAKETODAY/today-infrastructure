/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.view.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Component;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.Version;

/**
 * @author TODAY 2021/3/24 21:50
 * @since 3.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Import(FreeMarkerConfig.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface EnableFreeMarker {
  String CONFIGURATION_BEAN_NAME = "freemarker.template.Configuration";

}

@cn.taketoday.context.annotation.Configuration(proxyBeanMethods = false)
class FreeMarkerConfig {

  @Props(prefix = "web.mvc.view.")
  @Order(Ordered.LOWEST_PRECEDENCE - 100)
  @MissingBean(AbstractFreeMarkerTemplateRenderer.class)
  FreeMarkerTemplateRenderer freeMarkerTemplateRenderer(
          Configuration configuration,
          ResourceLoader resourceLoader,
          ObjectWrapper freeMarkerObjectWrapper,
          Map<String, TemplateModel> templateModels,
          @Props(prefix = "freemarker.", replace = true) Properties settings
  ) {
    Logger log = LoggerFactory.getLogger(getClass());
    log.info("Initialize freemarker");

    FreeMarkerTemplateRenderer renderer = new FreeMarkerTemplateRenderer();

    renderer.setConfiguration(configuration);
    renderer.setResourceLoader(resourceLoader);
    renderer.setObjectWrapper(freeMarkerObjectWrapper);

    log.info("Configure freemarker-template-model");
    templateModels.forEach(configuration::setSharedVariable);

    try {
      if (CollectionUtils.isNotEmpty(settings)) {
        configuration.setSettings(settings);
      }
    }
    catch (TemplateException e) {
      throw new ConfigurationException("Set FreeMarker's Properties Error: [" + e + "]", e);
    }

    log.info("FreeMarker template renderer init successfully.");
    return renderer;
  }

  protected Version freemakerVersion() {
    return Configuration.VERSION_2_3_31;
  }

  @Component
  @ConditionalOnMissingBean
  DefaultObjectWrapper freeMarkerObjectWrapper() {
    return new DefaultObjectWrapper(freemakerVersion());
  }

  @ConditionalOnMissingBean
  @Component(EnableFreeMarker.CONFIGURATION_BEAN_NAME)
  Configuration freemakerConfiguration() {
    return new Configuration(freemakerVersion());
  }

}
