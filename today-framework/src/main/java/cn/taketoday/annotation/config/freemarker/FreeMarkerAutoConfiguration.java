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
import cn.taketoday.web.view.freemarker.FreeMarkerConfigurationFactory;

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

  private static final Logger logger = LoggerFactory.getLogger(FreeMarkerAutoConfiguration.class);

  private final ApplicationContext applicationContext;

  private final FreeMarkerProperties properties;

  public FreeMarkerAutoConfiguration(ApplicationContext applicationContext, FreeMarkerProperties properties) {
    this.applicationContext = applicationContext;
    this.properties = properties;
    checkTemplateLocationExists();
  }

  public void checkTemplateLocationExists() {
    if (logger.isWarnEnabled() && this.properties.isCheckTemplateLocation()) {
      List<TemplateLocation> locations = getLocations();
      if (locations.stream().noneMatch(this::locationExists)) {
        logger.warn("Cannot find template location(s): {} (please add some templates, "
                + "check your FreeMarker configuration, or set "
                + "infra.freemarker.check-template-location=false)", locations);
      }
    }
  }

  private List<TemplateLocation> getLocations() {
    List<TemplateLocation> locations = new ArrayList<>();
    for (String templateLoaderPath : this.properties.getTemplateLoaderPath()) {
      TemplateLocation location = new TemplateLocation(templateLoaderPath);
      locations.add(location);
    }
    return locations;
  }

  private boolean locationExists(TemplateLocation location) {
    return location.exists(this.applicationContext);
  }

}
