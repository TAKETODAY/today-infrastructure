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

package cn.taketoday.http;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.core.AttributeAccessorSupport;

/**
 * for AttributeAccessor support
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/4 21:46
 */
public abstract class AbstractHttpRequest extends AttributeAccessorSupport implements HttpRequest {

  @Override
  protected Map<String, Object> createAttributes() {
    return new LinkedHashMap<>();
  }

}
