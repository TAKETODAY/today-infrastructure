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

package cn.taketoday.annotation.config.freemarker;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.framework.template.TemplateLocation;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.ui.freemarker.FreeMarkerConfigurationFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for FreeMarker.
 *
 * @author Andy Wilkinson
 * @author Dave Syer
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@AutoConfiguration
@EnableConfigurationProperties(FreeMarkerProperties.class)
@ConditionalOnClass({ freemarker.template.Configuration.class, FreeMarkerConfigurationFactory.class })
@Import({ FreeMarkerWebConfiguration.class, FreeMarkerNonWebConfiguration.class })
public class FreeMarkerAutoConfiguration {

  public FreeMarkerAutoConfiguration(ApplicationContext context, FreeMarkerProperties properties) {
    checkTemplateLocationExists(properties, context);
  }

  public void checkTemplateLocationExists(FreeMarkerProperties properties, ApplicationContext context) {
    Logger logger = LoggerFactory.getLogger(FreeMarkerAutoConfiguration.class);
    if (logger.isWarnEnabled() && properties.isCheckTemplateLocation()) {
      List<TemplateLocation> locations = getLocations(properties);
      for (TemplateLocation location : locations) {
        if (location.exists(context)) {
          return;
        }
      }
      logger.warn("Cannot find template location(s): {} (please add some templates, "
              + "check your FreeMarker configuration, or set "
              + "freemarker.check-template-location=false)", locations);
    }
  }

  private List<TemplateLocation> getLocations(FreeMarkerProperties properties) {
    var locations = new ArrayList<TemplateLocation>();
    for (String templateLoaderPath : properties.getTemplateLoaderPath()) {
      TemplateLocation location = new TemplateLocation(templateLoaderPath);
      locations.add(location);
    }
    return locations;
  }

}
