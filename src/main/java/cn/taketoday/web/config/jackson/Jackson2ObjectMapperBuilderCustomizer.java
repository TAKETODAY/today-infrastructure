/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.config.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

import cn.taketoday.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link ObjectMapper} via {@link Jackson2ObjectMapperBuilder} retaining its default
 * auto-configuration.
 *
 * @author Grzegorz Poznachowski
 * @since 4.0
 */
@FunctionalInterface
public interface Jackson2ObjectMapperBuilderCustomizer {

  /**
   * Customize the JacksonObjectMapperBuilder.
   *
   * @param jacksonObjectMapperBuilder the JacksonObjectMapperBuilder to customize
   */
  void customize(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder);

}
