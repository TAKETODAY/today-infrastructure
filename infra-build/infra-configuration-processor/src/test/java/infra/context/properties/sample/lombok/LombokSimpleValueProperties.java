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

package infra.context.properties.sample.lombok;

import java.util.ArrayList;
import java.util.List;

import infra.context.properties.sample.ConfigurationProperties;
import lombok.Value;

/**
 * Configuration properties using Lombok {@code @Value}.
 *
 * @author Mark Jeffrey
 */
@Value
@ConfigurationProperties(prefix = "value")
@SuppressWarnings("unused")
public class LombokSimpleValueProperties {

  private final String id = "super-id";

  /**
   * Name description.
   */
  private String name;

  private String description;

  private Integer counter;

  @Deprecated
  private Integer number = 0;

  private final List<String> items = new ArrayList<>();

  private final String ignored = "foo";

}
