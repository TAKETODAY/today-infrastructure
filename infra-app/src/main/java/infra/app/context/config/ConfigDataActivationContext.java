/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.context.config;

import org.jspecify.annotations.Nullable;

import infra.app.cloud.CloudPlatform;
import infra.context.properties.bind.Binder;
import infra.core.env.Environment;
import infra.core.style.ToStringBuilder;

/**
 * Context information used when determining when to activate
 * {@link ConfigDataEnvironmentContributor contributed} {@link ConfigData}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigDataActivationContext {

  /**
   * the active {@link CloudPlatform} or {@code null}.
   */
  @Nullable
  public final CloudPlatform cloudPlatform;

  /**
   * profile information if it is available.
   */
  @Nullable
  public final Profiles profiles;

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
  ConfigDataActivationContext(@Nullable CloudPlatform cloudPlatform, Profiles profiles) {
    this.cloudPlatform = cloudPlatform;
    this.profiles = profiles;
  }

  @Nullable
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

  @Override
  public String toString() {
    ToStringBuilder creator = new ToStringBuilder(this);
    creator.append("cloudPlatform", this.cloudPlatform);
    creator.append("profiles", this.profiles);
    return creator.toString();
  }

}
