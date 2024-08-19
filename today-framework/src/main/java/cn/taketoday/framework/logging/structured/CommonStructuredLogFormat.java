/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.framework.logging.structured;

import cn.taketoday.lang.Nullable;

/**
 * Common structured log formats supported by infra.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public enum CommonStructuredLogFormat {

  /**
   * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">Elasic Common
   * Schema</a> (ECS) log format.
   */
  ELASTIC_COMMON_SCHEMA("ecs"),

  /**
   * The <a href=
   * "https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#standard-fields">Logstash</a>
   * log format.
   */
  LOGSTASH("logstash");

  private final String id;

  CommonStructuredLogFormat(String id) {
    this.id = id;
  }

  /**
   * Return the ID for this format.
   *
   * @return the format identifier
   */
  String getId() {
    return this.id;
  }

  /**
   * Find the {@link CommonStructuredLogFormat} for the given ID.
   *
   * @param id the format identifier
   * @return the associated {@link CommonStructuredLogFormat} or {@code null}
   */
  @Nullable
  static CommonStructuredLogFormat forId(String id) {
    for (CommonStructuredLogFormat candidate : values()) {
      if (candidate.getId().equalsIgnoreCase(id)) {
        return candidate;
      }
    }
    return null;
  }

}
