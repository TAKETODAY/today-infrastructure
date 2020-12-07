/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.web.HttpHeaders;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.ui.Model;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.ui.RedirectModel;
import cn.taketoday.web.ui.RedirectModelAttributes;

/**
 * @author TODAY <br>
 *         2019-07-09 22:49
 */
public class ModelParameterResolver implements ParameterResolver {

  @Override
  public boolean supports(final MethodParameter parameter) {
    return parameter.isAssignableFrom(Model.class) //
            || parameter.is(HttpHeaders.class)//
            || (parameter.is(Map.class)
            && parameter.isGenericPresent(String.class, 0)
            && parameter.isGenericPresent(Object.class, 1)//
    );
  }

  /**
   * Resolve {@link Model} parameter.
   */
  @Override
  public Object resolveParameter(final RequestContext context, final MethodParameter parameter) throws Throwable {

    if (parameter.isAssignableFrom(RedirectModel.class)) { // RedirectModel
      return context.redirectModel(new RedirectModelAttributes());
    }
    if (parameter.isAssignableFrom(ModelAndView.class)) {
      return context.modelAndView();
    }
    return context;
  }

}
