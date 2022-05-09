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

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import cn.taketoday.context.properties.bind.BindContext;
import cn.taketoday.context.properties.bind.BindHandler;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.bind.Name;
import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.framework.cloud.CloudPlatform;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Bound properties used when working with {@link ConfigData}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
class ConfigDataProperties {

  private static final ConfigurationPropertyName NAME = ConfigurationPropertyName.of("context.config");

  private static final Bindable<ConfigDataProperties> BINDABLE_PROPERTIES = Bindable.of(ConfigDataProperties.class);

  private final List<ConfigDataLocation> imports;

  @Nullable
  private final Activate activate;

  /**
   * Create a new {@link ConfigDataProperties} instance.
   *
   * @param imports the imports requested
   * @param activate the activate properties
   */
  ConfigDataProperties(@Name("import") @Nullable List<ConfigDataLocation> imports, @Nullable Activate activate) {
    this.imports = (imports != null) ? imports : Collections.emptyList();
    this.activate = activate;
  }

  /**
   * Return any additional imports requested.
   *
   * @return the requested imports
   */
  List<ConfigDataLocation> getImports() {
    return this.imports;
  }

  /**
   * Return {@code true} if the properties indicate that the config data property source
   * is active for the given activation context.
   *
   * @param activationContext the activation context
   * @return {@code true} if the config data property source is active
   */
  boolean isActive(@Nullable ConfigDataActivationContext activationContext) {
    return this.activate == null || this.activate.isActive(activationContext);
  }

  /**
   * Return a new variant of these properties without any imports.
   *
   * @return a new {@link ConfigDataProperties} instance
   */
  ConfigDataProperties withoutImports() {
    return new ConfigDataProperties(null, this.activate);
  }

  /**
   * Factory method used to create {@link ConfigDataProperties} from the given
   * {@link Binder}.
   *
   * @param binder the binder used to bind the properties
   * @return a {@link ConfigDataProperties} instance or {@code null}
   */
  @Nullable
  static ConfigDataProperties get(Binder binder) {
    return binder.bind(NAME, BINDABLE_PROPERTIES, new ConfigDataLocationBindHandler()).orElse(null);
  }

  /**
   * Activate properties used to determine when a config data property source is active.
   */
  static class Activate {

    @Nullable
    private final CloudPlatform onCloudPlatform;

    @Nullable
    private final String[] onProfile;

    /**
     * Create a new {@link Activate} instance.
     *
     * @param onCloudPlatform the cloud platform required for activation
     * @param onProfile the profile expression required for activation
     */
    Activate(@Nullable CloudPlatform onCloudPlatform, @Nullable String[] onProfile) {
      this.onProfile = onProfile;
      this.onCloudPlatform = onCloudPlatform;
    }

    /**
     * Return {@code true} if the properties indicate that the config data property
     * source is active for the given activation context.
     *
     * @param activationContext the activation context
     * @return {@code true} if the config data property source is active
     */
    boolean isActive(@Nullable ConfigDataActivationContext activationContext) {
      if (activationContext == null) {
        return false;
      }
      boolean activate = isActive(activationContext.getCloudPlatform());
      activate = activate && isActive(activationContext.getProfiles());
      return activate;
    }

    private boolean isActive(CloudPlatform cloudPlatform) {
      return this.onCloudPlatform == null || this.onCloudPlatform == cloudPlatform;
    }

    private boolean isActive(@Nullable Profiles profiles) {
      return ObjectUtils.isEmpty(this.onProfile)
              || (profiles != null && matchesActiveProfiles(profiles::isAccepted));
    }

    private boolean matchesActiveProfiles(Predicate<String> activeProfiles) {
      return cn.taketoday.core.env.Profiles.of(this.onProfile).matches(activeProfiles);
    }

  }

}
