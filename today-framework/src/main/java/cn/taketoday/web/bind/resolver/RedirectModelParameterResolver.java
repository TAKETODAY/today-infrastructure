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

import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.view.Model;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.RedirectModel;
import cn.taketoday.web.view.RedirectModelManager;

/**
 * Supports {@link RedirectModel}
 *
 * @author TODAY 2019-07-09 22:49
 * @see RedirectModel
 */
public class RedirectModelParameterResolver implements ParameterResolvingStrategy {

  @Nullable
  private final RedirectModelManager modelManager;

  public RedirectModelParameterResolver(@Nullable RedirectModelManager modelManager) {
    this.modelManager = modelManager;
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.is(RedirectModel.class);
  }

  /**
   * Resolve {@link Model} parameter.
   */
  @Override
  public Object resolveParameter(
          RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {

    BindingContext bindingContext = context.getBindingContext();
    RedirectModel redirectModel = new RedirectModel();
    RedirectModelManager modelManager = getModelManager();
    // @since 3.0.3 checking model manager
    if (modelManager != null) {
      context.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, redirectModel);
      modelManager.saveRedirectModel(context, redirectModel);
    }
    return redirectModel;
  }

  @Nullable
  public RedirectModelManager getModelManager() {
    return modelManager;
  }

}
