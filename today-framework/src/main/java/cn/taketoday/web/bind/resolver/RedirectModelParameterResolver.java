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

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.view.RedirectModel;

/**
 * Supports {@link RedirectModel}
 *
 * @author TODAY 2019-07-09 22:49
 * @see RedirectModel
 * @see RedirectModel#OUTPUT_ATTRIBUTE
 */
public class RedirectModelParameterResolver implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.is(RedirectModel.class);
  }

  /**
   * Resolve {@link RedirectModel} parameter.
   * <p>
   * and set {@code RedirectModel#OUTPUT_ATTRIBUTE_NAME} to RequestContext
   *
   * @see RedirectModel#OUTPUT_ATTRIBUTE
   */
  @Override
  public Object resolveParameter(
          RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    RedirectModel redirectModel = new RedirectModel();

    context.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, redirectModel);

    // set redirect model to current BindingContext
    context.getBindingContext().setRedirectModel(redirectModel);
    return redirectModel;
  }

}
