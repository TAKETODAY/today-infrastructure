/*
 * Copyright 2017 - 2026 the TODAY authors.
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
@SuppressWarnings("NullAway")
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
