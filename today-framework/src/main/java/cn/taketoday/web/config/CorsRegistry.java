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

package cn.taketoday.web.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.util.CollectionUtils;

/**
 * Assists with the registration of global, URL pattern based
 * {@link CorsConfiguration} mappings.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CorsRegistration
 * @since 4.0 2022/2/15 17:27
 */
public class CorsRegistry {

  private final List<CorsRegistration> registrations = new ArrayList<>();

  /**
   * Enable cross-origin request handling for the specified path pattern.
   * <p>Exact path mapping URIs (such as {@code "/admin"}) are supported as
   * well as Ant-style path patterns (such as {@code "/admin/**"}).
   * <p>By default, the {@code CorsConfiguration} for this mapping is
   * initialized with default values as described in
   * {@link CorsConfiguration#applyPermitDefaultValues()}.
   */
  public CorsRegistration addMapping(String pathPattern) {
    CorsRegistration registration = new CorsRegistration(pathPattern);
    this.registrations.add(registration);
    return registration;
  }

  /**
   * Return the registered {@link CorsConfiguration} objects,
   * keyed by path pattern.
   */
  protected Map<String, CorsConfiguration> getCorsConfigurations() {
    Map<String, CorsConfiguration> configs = CollectionUtils.newLinkedHashMap(this.registrations.size());
    for (CorsRegistration registration : this.registrations) {
      configs.put(registration.getPathPattern(), registration.getCorsConfiguration());
    }
    return configs;
  }

}
