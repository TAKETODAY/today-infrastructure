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

import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.framework.cloud.CloudPlatform;
import cn.taketoday.lang.Nullable;

/**
 * Context information used when determining when to activate
 * {@link ConfigDataEnvironmentContributor contributed} {@link ConfigData}.
 *
 * @author Phillip Webb
 */
class ConfigDataActivationContext {

  private final CloudPlatform cloudPlatform;

  @Nullable
  private final Profiles profiles;

  /**
   * Create a new {@link ConfigDataActivationContext} instance before any profiles have
   * been activated.
   *
   * @param environment the source environment
   * @param binder a binder providing access to relevant config data contributions
   */
  ConfigDataActivationContext(Environment environment, Binder binder) {
    this.cloudPlatform = deduceCloudPlatform(environment, binder);
    this.profiles = null;
  }

  /**
   * Create a new {@link ConfigDataActivationContext} instance with the given
   * {@link CloudPlatform} and {@link Profiles}.
   *
   * @param cloudPlatform the cloud platform
   * @param profiles the profiles
   */
  ConfigDataActivationContext(CloudPlatform cloudPlatform, Profiles profiles) {
    this.cloudPlatform = cloudPlatform;
    this.profiles = profiles;
  }

  private CloudPlatform deduceCloudPlatform(Environment environment, Binder binder) {
    for (CloudPlatform candidate : CloudPlatform.values()) {
      if (candidate.isEnforced(binder)) {
        return candidate;
      }
    }
    return CloudPlatform.getActive(environment);
  }

  /**
   * Return a new {@link ConfigDataActivationContext} with specific profiles.
   *
   * @param profiles the profiles
   * @return a new {@link ConfigDataActivationContext} with specific profiles
   */
  ConfigDataActivationContext withProfiles(Profiles profiles) {
    return new ConfigDataActivationContext(this.cloudPlatform, profiles);
  }

  /**
   * Return the active {@link CloudPlatform} or {@code null}.
   *
   * @return the active cloud platform
   */
  CloudPlatform getCloudPlatform() {
    return this.cloudPlatform;
  }

  /**
   * Return profile information if it is available.
   *
   * @return profile information or {@code null}
   */
  @Nullable
  Profiles getProfiles() {
    return this.profiles;
  }

  @Override
  public String toString() {
    ToStringBuilder creator = new ToStringBuilder(this);
    creator.append("cloudPlatform", this.cloudPlatform);
    creator.append("profiles", this.profiles);
    return creator.toString();
  }

}
