/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import infra.app.cloud.CloudPlatform;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.context.properties.bind.Name;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.util.ObjectUtils;

/**
 * Bound properties used when working with {@link ConfigData}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigDataProperties {

  /**
   * any additional imports requested.
   */
  public final List<ConfigDataLocation> imports;

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
    return binder.bind(ConfigurationPropertyName.of("app.config"),
            Bindable.of(ConfigDataProperties.class), new ConfigDataLocationBindHandler()).orElse(null);
  }

  /**
   * Activate properties used to determine when a config data property source is active.
   */
  static class Activate {

    @Nullable
    private final CloudPlatform onCloudPlatform;

    private final String @Nullable [] onProfile;

    /**
     * Create a new {@link Activate} instance.
     *
     * @param onCloudPlatform the cloud platform required for activation
     * @param onProfile the profile expression required for activation
     */
    Activate(@Nullable CloudPlatform onCloudPlatform, String @Nullable [] onProfile) {
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
      CloudPlatform cloudPlatform = activationContext.cloudPlatform;
      return isActive(cloudPlatform != null ? cloudPlatform : CloudPlatform.NONE)
              && isActive(activationContext.profiles);
    }

    private boolean isActive(@Nullable CloudPlatform cloudPlatform) {
      return this.onCloudPlatform == null || this.onCloudPlatform == cloudPlatform;
    }

    private boolean isActive(@Nullable Profiles profiles) {
      return ObjectUtils.isEmpty(this.onProfile)
              || (profiles != null && matchesActiveProfiles(profiles::isAccepted));
    }

    @SuppressWarnings("NullAway")
    private boolean matchesActiveProfiles(Predicate<String> activeProfiles) {
      return infra.core.env.Profiles.parse(this.onProfile).matches(activeProfiles);
    }

  }

}
