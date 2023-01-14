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

package cn.taketoday.annotation.config.web.servlet;

import java.io.File;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.template.TemplateAvailabilityProvider;
import cn.taketoday.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for JSP
 * view templates.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JspTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

  @Override
  public boolean isTemplateAvailable(String view,
          Environment environment, ClassLoader classLoader, ResourceLoader resourceLoader) {
    if (ClassUtils.isPresent("org.apache.jasper.compiler.JspConfig", classLoader)) {
      String resourceName = getResourceName(view, environment);
      if (resourceLoader.getResource(resourceName).exists()) {
        return true;
      }
      return new File("src/main/webapp", resourceName).exists();
    }
    return false;
  }

  private String getResourceName(String view, Environment environment) {
    String prefix = environment.getProperty("web.mvc.view.prefix", "");
    String suffix = environment.getProperty("web.mvc.view.suffix", "");
    return prefix + view + suffix;
  }

}
