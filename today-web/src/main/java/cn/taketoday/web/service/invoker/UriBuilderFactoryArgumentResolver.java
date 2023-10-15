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

package cn.taketoday.web.service.invoker;

import java.net.URL;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.util.UriBuilderFactory;
import cn.taketoday.web.util.UriTemplate;

/**
 * An {@link HttpServiceArgumentResolver} that uses the provided
 * {@link UriBuilderFactory} to expand the {@link UriTemplate}.
 * <p>Unlike with the {@link UrlArgumentResolver},
 * if the {@link UriBuilderFactoryArgumentResolver} is provided,
 * it will not override the entire {@link URL}, but just the {@code baseUri}.
 * <p>This allows for dynamically setting the {@code baseUri},
 * while keeping the {@code path} specified through class
 * and method annotations.
 *
 * @author Olga Maciaszek-Sharma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UriBuilderFactoryArgumentResolver implements HttpServiceArgumentResolver {

  @Override
  public boolean resolve(@Nullable Object argument,
          MethodParameter parameter, HttpRequestValues.Builder requestValues) {
    if (!parameter.getParameterType().equals(UriBuilderFactory.class)) {
      return false;
    }

    if (argument != null) {
      requestValues.setUriBuilderFactory((UriBuilderFactory) argument);
    }

    return true;
  }
}
