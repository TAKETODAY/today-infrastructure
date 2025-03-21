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

package infra.web.client;

import java.io.IOException;
import java.lang.reflect.Type;

import infra.http.client.ClientHttpResponse;
import infra.lang.Nullable;

/**
 * Generic callback interface used by {@link RestTemplate}'s retrieval methods.
 * Implementations of this interface perform the actual work of extracting data
 * from a {@link ClientHttpResponse}, but don't need to worry about exception
 * handling or closing resources.
 *
 * <p>Used internally by the {@link RestTemplate}, but also useful for
 * application code. There is one available factory method, see
 * {@link RestTemplate#responseEntityExtractor(Type)}.
 *
 * @param <T> the data type
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RestTemplate#execute
 * @since 4.0
 */
@FunctionalInterface
public interface ResponseExtractor<T> {

  /**
   * Extract data from the given {@code ClientHttpResponse} and return it.
   *
   * @param response the HTTP response
   * @return the extracted data
   * @throws IOException in case of I/O errors
   */
  @Nullable
  T extractData(ClientHttpResponse response) throws IOException;

}
