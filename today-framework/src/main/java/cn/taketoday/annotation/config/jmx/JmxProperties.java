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

package cn.taketoday.annotation.config.jmx;

import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.jmx.support.RegistrationPolicy;

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
