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

package cn.taketoday.web.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * @author TODAY 2021/9/3 23:51
 */
public class CharSequenceReturnValueHandler
        extends HandlerMethodReturnValueHandler implements ReturnValueHandler {

  private final TemplateRendererReturnValueHandler templateRendererHandler;

  public CharSequenceReturnValueHandler(
          TemplateRendererReturnValueHandler templateRendererHandler) {
    Assert.notNull(templateRendererHandler, "templateRendererHandler must not be null");
    this.templateRendererHandler = templateRendererHandler;
  }

  @Override
  protected boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturnTypeAssignableTo(CharSequence.class);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof CharSequence;
  }

  @Override
  public void handleReturnValue(RequestContext context, Object handler, Object returnValue) throws IOException {
    if (returnValue instanceof CharSequence) {
      String toString = returnValue.toString();
      handleCharSequenceValue(toString, context);
    }
  }

  public void handleCharSequenceValue(final String resource, final RequestContext context) throws IOException {
    if (resource.startsWith(REDIRECT_URL_PREFIX)) {
      // redirect
      handleRedirect(resource.substring(9), context);
    }
    else if (resource.startsWith(RESPONSE_BODY_PREFIX)) {
      // write string to response-body
      StreamUtils.copy(resource.substring(5), StandardCharsets.UTF_8, context.getOutputStream());
    }
    else {
      // template view
      templateRendererHandler.renderTemplate(resource, context);
    }
  }

  private void handleRedirect(final String redirect, final RequestContext context) throws IOException {
    if (StringUtils.isEmpty(redirect) || redirect.startsWith(Constant.HTTP)) {
      context.sendRedirect(redirect);
    }
    else {
      context.sendRedirect(context.getContextPath().concat(redirect));
    }
  }
}
