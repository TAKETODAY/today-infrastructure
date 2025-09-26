/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.logging.structured;

import org.jspecify.annotations.Nullable;

import infra.app.json.JsonWriter;
import infra.context.properties.bind.Binder;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.util.StringUtils;

/**
 * Service details for Graylog Extended Log Format structured logging.
 *
 * @param name the application name
 * @param version the version of the application
 * @author Samuel Lissner
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public record GraylogExtendedLogFormatService(@Nullable String name, @Nullable String version) {

  static final GraylogExtendedLogFormatService NONE = new GraylogExtendedLogFormatService(null, null);

  private GraylogExtendedLogFormatService withDefaults(Environment environment) {
    String name = withFallbackProperty(environment, this.name, "app.name");
    String version = withFallbackProperty(environment, this.version, "app.version");
    return new GraylogExtendedLogFormatService(name, version);
  }

  @Nullable
  private String withFallbackProperty(Environment environment, @Nullable String value, String property) {
    return StringUtils.isEmpty(value) ? environment.getProperty(property) : value;
  }

  /**
   * Add {@link JsonWriter} members for the service.
   *
   * @param members the members to add to
   */
  public void jsonMembers(JsonWriter.Members<?> members) {
    members.add("host", this::name).whenHasLength();
    members.add("_service_version", this::version).whenHasLength();
  }

  /**
   * Return a new {@link GraylogExtendedLogFormatService} from bound from properties in
   * the given {@link Environment}.
   *
   * @param environment the source environment
   * @return a new {@link GraylogExtendedLogFormatService} instance
   */
  public static GraylogExtendedLogFormatService get(ConfigurableEnvironment environment) {
    return Binder.get(environment)
            .bind("logging.structured.gelf.service", GraylogExtendedLogFormatService.class)
            .orElse(NONE)
            .withDefaults(environment);
  }

}
