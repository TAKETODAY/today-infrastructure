/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.view.Model;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.RedirectModel;
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
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    Class<?> parameterType = resolvable.getParameterType();
    if (Model.class.isAssignableFrom(parameterType)) {
      return true;
    }
    if (parameterType == HttpHeaders.class) {
      // http request headers @since 3.0
      return true;
    }

    if (parameterType == Map.class) {
      // Map<String, Object> model;
      ResolvableType mapType = resolvable.getResolvableType().asMap();
      ResolvableType keyType = mapType.getGeneric(0);
      ResolvableType valueType = mapType.getGeneric(1);
      return keyType.resolve() == String.class
              && valueType.resolve() == Object.class;
    }
    return false;
  }

  /**
   * Resolve {@link Model} parameter.
   */
  @Override
  public Object resolveParameter(
          RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {

    if (resolvable.isAssignableTo(RedirectModel.class)) { // RedirectModel
      RedirectModel redirectModel = new RedirectModel();
      RedirectModelManager modelManager = getModelManager();
      // @since 3.0.3 checking model manager
      if (modelManager != null) {
        context.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, redirectModel);
        modelManager.saveRedirectModel(context, redirectModel);
      }
      return redirectModel;
    }
    if (resolvable.isAssignableTo(ModelAndView.class)) {
      return context.modelAndView();
    }

    // @since 3.0
    if (resolvable.is(HttpHeaders.class)) {
      return context.requestHeaders();
    }

    if (resolvable.is(Map.class)) {
      return context.asMap(); // Model Map
    }
    return context;
  }

  @Nullable
  public RedirectModelManager getModelManager() {
    return modelManager;
  }

}
