/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.bind.support;

import org.jspecify.annotations.Nullable;

import infra.core.MethodParameter;
import infra.util.StringUtils;
import infra.validation.DataBinder;
import infra.web.bind.annotation.BindParam;

/**
 * {@link DataBinder.NameResolver} that determines
 * the bind value name from a {@link BindParam @BindParam} method parameter
 * annotation.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class BindParamNameResolver implements DataBinder.NameResolver {

  @Nullable
  @Override
  public String resolveName(MethodParameter parameter) {
    BindParam bindParam = parameter.getParameterAnnotation(BindParam.class);
    if (bindParam != null) {
      if (StringUtils.hasText(bindParam.value())) {
        return bindParam.value();
      }
    }
    return null;
  }

}
