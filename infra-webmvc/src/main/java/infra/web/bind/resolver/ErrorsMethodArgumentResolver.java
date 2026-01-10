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

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import infra.ui.ModelMap;
import infra.util.CollectionUtils;
import infra.validation.BindingResult;
import infra.validation.Errors;
import infra.web.RequestContext;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * Resolves {@link Errors} method arguments.
 *
 * <p>An {@code Errors} method argument is expected to appear immediately after
 * the model attribute in the method signature. It is resolved by expecting the
 * last two attributes added to the model to be the model attribute and its
 * {@link BindingResult}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/27 15:36
 */
public class ErrorsMethodArgumentResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.isAssignableTo(Errors.class);
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    ModelMap model = context.binding().getModel();
    String lastKey = CollectionUtils.lastElement(model.keySet());
    if (lastKey != null && lastKey.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
      return model.getAttribute(lastKey);
    }

    throw new IllegalStateException(
            "An Errors/BindingResult argument is expected to be declared immediately after " +
                    "the model attribute, the @RequestBody or the @RequestPart arguments " +
                    "to which they apply: " + resolvable.getMethod());
  }

}
