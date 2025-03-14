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

package infra.http.codec;

/**
 * Callback interface that can be used to customize codecs configuration for an HTTP
 * client and/or server with a {@link CodecConfigurer}.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 16:33
 */
@FunctionalInterface
public interface CodecCustomizer {

  /**
   * Callback to customize a {@link CodecConfigurer} instance.
   *
   * @param configurer codec configurer to customize
   */
  void customize(CodecConfigurer configurer);

}
