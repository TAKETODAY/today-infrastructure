/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.resolver;

import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.view.Model;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.RedirectModel;
import cn.taketoday.web.view.RedirectModelAttributes;
import cn.taketoday.web.view.RedirectModelManager;

/**
 * Supports {@link Model}, {@link RedirectModel}, HTTP request headers,
 * Map<String, Object> model
 *
 * @author TODAY 2019-07-09 22:49
 * @see Model
 * @see RedirectModel
 */
public class ModelParameterResolver implements ParameterResolvingStrategy {

  @Nullable
  private final RedirectModelManager modelManager;

  public ModelParameterResolver(@Nullable RedirectModelManager modelManager) {
    this.modelManager = modelManager;
  }

  @Override
  public boolean supportsParameter(final MethodParameter parameter) {
    return parameter.isAssignableTo(Model.class) // Model
            || parameter.is(HttpHeaders.class) // HTTP request headers @since 3.0
            || (
            parameter.is(Map.class) // Map<String, Object> model;
                    && parameter.isGenericPresent(String.class, 0)
                    && parameter.isGenericPresent(Object.class, 1)
    );
  }

  /**
   * Resolve {@link Model} parameter.
   */
  @Override
  public Object resolveParameter(final RequestContext context,
                                 final MethodParameter parameter) throws Throwable {
    if (parameter.isAssignableTo(RedirectModel.class)) { // RedirectModel
      final RedirectModelAttributes redirectModel = new RedirectModelAttributes();
      final RedirectModelManager modelManager = getModelManager();
      // @since 3.0.3 checking model manager
      if (modelManager != null) {
        modelManager.applyModel(context, redirectModel);
      }
      return redirectModel;
    }
    if (parameter.isAssignableTo(ModelAndView.class)) {
      return context.modelAndView();
    }

    // @since 3.0
    if (parameter.is(HttpHeaders.class)) {
      return context.requestHeaders();
    }

    if (parameter.is(Map.class)) {
      return context.asMap(); // Model Map
    }
    return context;
  }

  @Nullable
  public RedirectModelManager getModelManager() {
    return modelManager;
  }

}
