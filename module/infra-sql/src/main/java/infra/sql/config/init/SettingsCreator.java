/*
 * Copyright 2012-present the original author or authors.
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

package infra.sql.config.init;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.sql.init.DatabaseInitializationSettings;

/**
 * Helpers class for creating {@link DatabaseInitializationSettings} from
 * {@link SqlInitializationProperties}.
 *
 * @author Andy Wilkinson
 */
final class SettingsCreator {

  private SettingsCreator() {
  }

  static DatabaseInitializationSettings createFrom(SqlInitializationProperties properties) {
    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings
            .setSchemaLocations(scriptLocations(properties.getSchemaLocations(), "schema", properties.getPlatform()));
    settings.setDataLocations(scriptLocations(properties.getDataLocations(), "data", properties.getPlatform()));
    settings.setContinueOnError(properties.isContinueOnError());
    settings.setSeparator(properties.getSeparator());
    settings.setEncoding(properties.getEncoding());
    settings.setMode(properties.getMode());
    return settings;
  }

  private static List<String> scriptLocations(@Nullable List<String> locations, String fallback, String platform) {
    if (locations != null) {
      return locations;
    }
    List<String> fallbackLocations = new ArrayList<>();
    fallbackLocations.add("optional:classpath*:" + fallback + "-" + platform + ".sql");
    fallbackLocations.add("optional:classpath*:" + fallback + ".sql");
    return fallbackLocations;
  }

}
