/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.sample.lombok;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.properties.sample.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties using lombok @Getter/@Setter at field level.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "explicit")
public class LombokExplicitProperties {

  @Getter
  private final String id = "super-id";

  /**
   * Name description.
   */
  @Getter
  @Setter
  private String name;

  @Getter
  @Setter
  private String description;

  @Getter
  @Setter
  private Integer counter;

  @Deprecated
  @Getter
  @Setter
  private Integer number = 0;

  @Getter
  private final List<String> items = new ArrayList<>();

  // Should be ignored if no annotation is set
  @SuppressWarnings("unused")
  private String ignored;

  @Getter
  private String ignoredOnlyGetter;

  @Setter
  private String ignoredOnlySetter;

}
