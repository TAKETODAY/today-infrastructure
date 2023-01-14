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

import cn.taketoday.framework.template.PathBasedTemplateAvailabilityProvider;
import cn.taketoday.framework.template.TemplateAvailabilityProvider;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for
 * FreeMarker view templates.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class FreeMarkerTemplateAvailabilityProvider extends PathBasedTemplateAvailabilityProvider {

  public FreeMarkerTemplateAvailabilityProvider() {
    super("freemarker.template.Configuration", FreeMarkerTemplateAvailabilityProperties.class, "spring.freemarker");
  }

  protected static final class FreeMarkerTemplateAvailabilityProperties extends TemplateAvailabilityProperties {

    private List<String> templateLoaderPath = new ArrayList<>(
            List.of(FreeMarkerProperties.DEFAULT_TEMPLATE_LOADER_PATH));

    FreeMarkerTemplateAvailabilityProperties() {
      super(FreeMarkerProperties.DEFAULT_PREFIX, FreeMarkerProperties.DEFAULT_SUFFIX);
    }

    @Override
    protected List<String> getLoaderPath() {
      return this.templateLoaderPath;
    }

    public List<String> getTemplateLoaderPath() {
      return this.templateLoaderPath;
    }

    public void setTemplateLoaderPath(List<String> templateLoaderPath) {
      this.templateLoaderPath = templateLoaderPath;
    }

  }

}
