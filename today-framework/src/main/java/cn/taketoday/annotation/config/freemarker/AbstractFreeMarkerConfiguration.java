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

import java.util.Properties;

import cn.taketoday.ui.freemarker.FreeMarkerConfigurationFactory;

/**
 * Base class for shared FreeMarker configuration.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class AbstractFreeMarkerConfiguration {

  protected final FreeMarkerProperties properties;

  protected AbstractFreeMarkerConfiguration(FreeMarkerProperties properties) {
    this.properties = properties;
  }

  protected void applyProperties(FreeMarkerConfigurationFactory factory) {
    factory.setTemplateLoaderPaths(properties.getTemplateLoaderPath());
    factory.setPreferFileSystemAccess(properties.isPreferFileSystemAccess());
    factory.setDefaultEncoding(properties.getCharsetName());
    Properties settings = new Properties();
    settings.put("recognize_standard_file_extensions", "true");
    settings.putAll(properties.getSettings());
    factory.setFreemarkerSettings(settings);
  }

}
