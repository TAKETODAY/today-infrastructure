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

package infra.http.converter;

/**
 * Extension of {@link infra.http.converter.FormHttpMessageConverter},
 * adding support for XML, JSON, Smile, CBOR, Protobuf and Yaml based parts when
 * related libraries are present in the classpath.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class AllEncompassingFormHttpMessageConverter extends FormHttpMessageConverter {

  /**
   * Create a new {@link AllEncompassingFormHttpMessageConverter} instance
   * that will auto-detect part converters.
   */
  public AllEncompassingFormHttpMessageConverter() {
    HttpMessageConverters.forClient().registerDefaults().build().forEach(this::addPartConverter);
  }

  /**
   * Create a new {@link AllEncompassingFormHttpMessageConverter} instance
   * using the given message converters.
   *
   * @param converters the message converters to use for part conversion
   * @since 5.0
   */
  public AllEncompassingFormHttpMessageConverter(Iterable<HttpMessageConverter<?>> converters) {
    for (HttpMessageConverter<?> converter : converters) {
      addPartConverter(converter);
    }
  }

}
