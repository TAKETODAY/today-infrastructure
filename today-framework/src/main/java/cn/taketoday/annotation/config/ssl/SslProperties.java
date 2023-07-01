/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.ssl;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.context.properties.ConfigurationProperties;

/**
 * Properties for centralized SSL trust material configuration.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConfigurationProperties(prefix = "ssl")
public class SslProperties {

  /**
   * SSL bundles.
   */
  private final Bundles bundle = new Bundles();

  public Bundles getBundle() {
    return this.bundle;
  }

  /**
   * Properties to define SSL Bundles.
   */
  public static class Bundles {

    /**
     * PEM-encoded SSL trust material.
     */
    private final Map<String, PemSslBundleProperties> pem = new LinkedHashMap<>();

    /**
     * Java keystore SSL trust material.
     */
    private final Map<String, JksSslBundleProperties> jks = new LinkedHashMap<>();

    public Map<String, PemSslBundleProperties> getPem() {
      return this.pem;
    }

    public Map<String, JksSslBundleProperties> getJks() {
      return this.jks;
    }

  }

}
