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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.env.PropertySource;

class TestConfigDataEnvironmentUpdateListener implements ConfigDataEnvironmentUpdateListener {

  private final List<AddedPropertySource> addedPropertySources = new ArrayList<>();

  private Profiles profiles;

  @Override
  public void onPropertySourceAdded(PropertySource<?> propertySource, ConfigDataLocation location,
          ConfigDataResource resource) {
    this.addedPropertySources.add(new AddedPropertySource(propertySource, location, resource));
  }

  @Override
  public void onSetProfiles(Profiles profiles) {
    this.profiles = profiles;
  }

  List<AddedPropertySource> getAddedPropertySources() {
    return Collections.unmodifiableList(this.addedPropertySources);
  }

  Profiles getProfiles() {
    return this.profiles;
  }

  static class AddedPropertySource {

    private final PropertySource<?> propertySource;

    private final ConfigDataLocation location;

    private final ConfigDataResource resource;

    AddedPropertySource(PropertySource<?> propertySource, ConfigDataLocation location,
            ConfigDataResource resource) {
      this.propertySource = propertySource;
      this.location = location;
      this.resource = resource;
    }

    PropertySource<?> getPropertySource() {
      return this.propertySource;
    }

    ConfigDataLocation getLocation() {
      return this.location;
    }

    ConfigDataResource getResource() {
      return this.resource;
    }

  }

}
