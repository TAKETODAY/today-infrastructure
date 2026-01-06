/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.annotation.config.jackson;

import tools.jackson.databind.json.JsonMapper;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link JsonMapper} through {@link tools.jackson.databind.json.JsonMapper.Builder} to
 * fine-tune its auto-configuration.
 *
 * @author Grzegorz Poznachowski
 * @since 5.0
 */
@FunctionalInterface
public interface JsonMapperBuilderCustomizer {

  /**
   * Customize the JsonMapper.Builder.
   *
   * @param jsonMapperBuilder the builder to customize
   */
  void customize(JsonMapper.Builder jsonMapperBuilder);

}
