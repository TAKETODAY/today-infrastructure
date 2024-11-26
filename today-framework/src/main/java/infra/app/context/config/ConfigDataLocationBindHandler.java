/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.app.context.config;

import java.util.ArrayList;
import java.util.List;

import infra.context.properties.bind.AbstractBindHandler;
import infra.context.properties.bind.BindContext;
import infra.context.properties.bind.BindHandler;
import infra.context.properties.bind.Bindable;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.origin.Origin;
import infra.util.CollectionUtils;

/**
 * {@link BindHandler} to set the {@link Origin} of bound {@link ConfigDataLocation}
 * objects.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class ConfigDataLocationBindHandler extends AbstractBindHandler {

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
    if (result instanceof ConfigDataLocation) {
      return withOrigin(context, (ConfigDataLocation) result);
    }
    if (result instanceof List list) {
      CollectionUtils.removeNullElements(list);
      for (int i = 0; i < list.size(); i++) {
        Object element = list.get(i);
        if (element instanceof ConfigDataLocation) {
          list.set(i, withOrigin(context, (ConfigDataLocation) element));
        }
      }
      return list;
    }
    if (result instanceof ConfigDataLocation[] dataLocations) {
      ArrayList<ConfigDataLocation> configDataLocations = new ArrayList<>(dataLocations.length);
      for (ConfigDataLocation dataLocation : dataLocations) {
        if (dataLocation != null) {
          configDataLocations.add(dataLocation);
        }
      }
      ConfigDataLocation[] locations = configDataLocations.toArray(new ConfigDataLocation[configDataLocations.size()]);
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
