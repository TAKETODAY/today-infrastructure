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

import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.json.JsonWriter;
import cn.taketoday.util.StringUtils;

/**
 * Service details for Elastic Common Schema structured logging.
 *
 * @param name the application name
 * @param version the version of the application
 * @param environment the name of the environment the application is running in
 * @param nodeName the name of the node the application is running on
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public record ElasticCommonSchemaService(String name, String version, String environment, String nodeName) {

  static final ElasticCommonSchemaService NONE = new ElasticCommonSchemaService(null, null, null, null);

  private ElasticCommonSchemaService withDefaults(Environment environment) {
    String name = withFallbackProperty(environment, this.name, "app.name");
    String version = withFallbackProperty(environment, this.version, "app.version");
    return new ElasticCommonSchemaService(name, version, this.environment, this.nodeName);
  }

  private String withFallbackProperty(Environment environment, String value, String property) {
    return StringUtils.isEmpty(value) ? environment.getProperty(property) : value;
  }

  /**
   * Add {@link JsonWriter} members for the service.
   *
   * @param members the members to add to
   */
  public void jsonMembers(JsonWriter.Members<?> members) {
    members.add("service.name", this::name).whenHasLength();
    members.add("service.version", this::version).whenHasLength();
    members.add("service.environment", this::environment).whenHasLength();
    members.add("service.node.name", this::nodeName).whenHasLength();
  }

  /**
   * Return a new {@link ElasticCommonSchemaService} from bound from properties in the
   * given {@link Environment}.
   *
   * @param environment the source environment
   * @return a new {@link ElasticCommonSchemaService} instance
   */
  public static ElasticCommonSchemaService get(ConfigurableEnvironment environment) {
    return Binder.get(environment)
            .bind("logging.structured.ecs.service", ElasticCommonSchemaService.class)
            .orElse(NONE)
            .withDefaults(environment);
  }
}
