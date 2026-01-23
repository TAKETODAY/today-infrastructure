/*
 * Copyright 2002-present the original author or authors.
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

package infra.app.config.jmx;

import org.jspecify.annotations.Nullable;

import infra.context.properties.ConfigurationProperties;
import infra.jmx.support.RegistrationPolicy;

/**
 * Configuration properties for JMX.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/9 18:36
 */
@ConfigurationProperties(prefix = "infra.jmx")
public class JmxProperties {

  /**
   * Expose management beans to the JMX domain.
   */
  private boolean enabled = false;

  /**
   * Whether unique runtime object names should be ensured.
   */
  private boolean uniqueNames = false;

  /**
   * MBeanServer bean name.
   */
  private String server = "mbeanServer";

  /**
   * JMX domain name.
   */
  @Nullable
  private String defaultDomain;

  /**
   * JMX Registration policy.
   */
  private RegistrationPolicy registrationPolicy = RegistrationPolicy.FAIL_ON_EXISTING;

  public boolean getEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isUniqueNames() {
    return this.uniqueNames;
  }

  public void setUniqueNames(boolean uniqueNames) {
    this.uniqueNames = uniqueNames;
  }

  public String getServer() {
    return this.server;
  }

  public void setServer(String server) {
    this.server = server;
  }

  @Nullable
  public String getDefaultDomain() {
    return this.defaultDomain;
  }

  public void setDefaultDomain(String defaultDomain) {
    this.defaultDomain = defaultDomain;
  }

  public RegistrationPolicy getRegistrationPolicy() {
    return this.registrationPolicy;
  }

  public void setRegistrationPolicy(RegistrationPolicy registrationPolicy) {
    this.registrationPolicy = registrationPolicy;
  }

}
