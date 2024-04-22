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

package cn.taketoday.web;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.web.handler.method.NamedValueInfo;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/27 17:03
 */
@SuppressWarnings("serial")
public class MockResolvableMethodParameter extends ResolvableMethodParameter {

  final String name;

  public MockResolvableMethodParameter(ResolvableMethodParameter other, String name) {
    super(other);
    this.name = name;
  }

  public MockResolvableMethodParameter(MethodParameter parameter, String name) {
    super(parameter);
    this.name = name;
  }

  @Override
  protected NamedValueInfo createNamedValueInfo() {
    if (name != null) {
      return new NamedValueInfo(name, true, null);
    }
    return super.createNamedValueInfo();
  }

}
