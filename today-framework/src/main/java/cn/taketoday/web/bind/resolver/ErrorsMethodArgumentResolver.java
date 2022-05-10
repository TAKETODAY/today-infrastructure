/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.web.bind.resolver;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.Errors;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.view.ModelMap;

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
    BindingContext bindingContext = context.getBindingContext();

    ModelMap model = bindingContext.getModel();
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
