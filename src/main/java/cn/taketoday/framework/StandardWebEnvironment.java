/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.framework;

import java.io.IOException;
import java.util.Set;

import cn.taketoday.context.StandardEnvironment;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Constant;
import cn.taketoday.core.utils.AnnotationUtils;
import cn.taketoday.core.utils.ObjectUtils;
import cn.taketoday.core.utils.StringUtils;
import cn.taketoday.framework.config.PropertiesSource;
import cn.taketoday.framework.utils.WebApplicationUtils;

/**
 * @author TODAY 2019-06-17 22:34
 */
public class StandardWebEnvironment extends StandardEnvironment {

  private final String[] arguments;
  private final Class<?> applicationClass;

  public StandardWebEnvironment() {
    this(null);
  }

  public StandardWebEnvironment(Class<?> applicationClass, String... arguments) {
    this.arguments = arguments;
    this.applicationClass = applicationClass;
  }

  @Override
  protected void postLoadingProperties(Set<String> locations) throws IOException {
    super.postLoadingProperties(locations);

    // load properties from starter class annotated @PropertiesSource
    if (applicationClass != null) {
      AnnotationAttributes[] attributes =
              AnnotationUtils.getAttributesArray(applicationClass, PropertiesSource.class);
      if (ObjectUtils.isNotEmpty(attributes)) {
        for (AnnotationAttributes attribute : attributes) {
          for (String propertiesLocation : StringUtils.split(attribute.getString(Constant.VALUE))) {
            if (!locations.contains(propertiesLocation)) {
              loadProperties(propertiesLocation);
              locations.add(propertiesLocation);
            }
          }
        }
      }
    }

    // arguments
    getProperties().putAll(WebApplicationUtils.parseCommandLineArguments(arguments));
  }

}
