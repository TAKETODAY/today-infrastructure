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

package infra.annotation.config.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link Gson} via {@link GsonBuilder} retaining its default auto-configuration.
 *
 * @author Ivan Golovko
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface GsonBuilderCustomizer {

  /**
   * Customize the GsonBuilder.
   *
   * @param gsonBuilder the GsonBuilder to customize
   */
  void customize(GsonBuilder gsonBuilder);

}
