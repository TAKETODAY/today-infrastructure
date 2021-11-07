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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.view.template;

import cn.taketoday.lang.Nullable;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;

/**
 * FreeMarker
 *
 * @author TODAY 2018-06-26 19:16:46
 */
public class FreeMarkerTemplateRenderer extends AbstractFreeMarkerTemplateRenderer {

  public FreeMarkerTemplateRenderer() { }

  public FreeMarkerTemplateRenderer(Configuration configuration) {
    this(new DefaultObjectWrapper(configuration.getIncompatibleImprovements()), configuration);
  }

  public FreeMarkerTemplateRenderer(
          @Nullable ObjectWrapper wrapper, @Nullable Configuration configuration) {
    setObjectWrapper(wrapper);
    setConfiguration(configuration);
  }

}
