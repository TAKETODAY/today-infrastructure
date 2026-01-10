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

package infra.http;

import java.util.LinkedHashMap;
import java.util.Map;

import infra.core.AttributeAccessorSupport;

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
