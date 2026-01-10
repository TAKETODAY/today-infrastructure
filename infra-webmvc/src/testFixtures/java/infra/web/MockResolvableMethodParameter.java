/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web;

import infra.core.MethodParameter;
import infra.web.handler.method.NamedValueInfo;
import infra.web.handler.method.ResolvableMethodParameter;

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
