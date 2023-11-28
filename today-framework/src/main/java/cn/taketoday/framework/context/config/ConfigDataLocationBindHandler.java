/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.context.config;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.taketoday.context.properties.bind.AbstractBindHandler;
import cn.taketoday.context.properties.bind.BindContext;
import cn.taketoday.context.properties.bind.BindHandler;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.origin.Origin;

/**
 * {@link BindHandler} to set the {@link Origin} of bound {@link ConfigDataLocation}
 * objects.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ConfigDataLocationBindHandler extends AbstractBindHandler {

  @Override
  @SuppressWarnings("unchecked")
  public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
    if (result instanceof ConfigDataLocation) {
      return withOrigin(context, (ConfigDataLocation) result);
    }
    if (result instanceof List) {
      List<Object> list = ((List<Object>) result).stream().filter(Objects::nonNull).collect(Collectors.toList());
      for (int i = 0; i < list.size(); i++) {
        Object element = list.get(i);
        if (element instanceof ConfigDataLocation) {
          list.set(i, withOrigin(context, (ConfigDataLocation) element));
        }
      }
      return list;
    }
    if (result instanceof ConfigDataLocation[]) {
      ConfigDataLocation[] locations = Arrays.stream((ConfigDataLocation[]) result).filter(Objects::nonNull)
              .toArray(ConfigDataLocation[]::new);
      for (int i = 0; i < locations.length; i++) {
        locations[i] = withOrigin(context, locations[i]);
      }
      return locations;
    }
    return result;
  }

  private ConfigDataLocation withOrigin(BindContext context, ConfigDataLocation result) {
    if (result.getOrigin() != null) {
      return result;
    }
    Origin origin = Origin.from(context.getConfigurationProperty());
    return result.withOrigin(origin);
  }

}
