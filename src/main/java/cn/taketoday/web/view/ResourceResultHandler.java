/**
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
package cn.taketoday.web.view;

import java.io.File;

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.WebUtils;

/**
 * @author TODAY <br>
 * 2019-07-14 11:18
 */
public class ResourceResultHandler extends OrderedSupport implements RuntimeResultHandler {
  private final int downloadFileBuf;

  public ResourceResultHandler(int downloadFileBuf) {
    this.downloadFileBuf = downloadFileBuf;
  }

  @Override
  public boolean supportsHandler(Object handler) {
    return HandlerMethodResultHandler.supportHandlerMethod(handler)
            && supports((HandlerMethod) handler);
  }

  private boolean supports(HandlerMethod handlerMethod) {
    return handlerMethod.isAssignableTo(Resource.class)
            || handlerMethod.isAssignableTo(File.class);
  }

  @Override
  public boolean supportsResult(Object result) {
    return result instanceof Resource
            || result instanceof File;
  }

  @Override
  public void handleResult(final RequestContext context,
                           final Object handler, final Object result) throws Throwable {
    if (result != null) {
      if (result instanceof Resource) {
        WebUtils.downloadFile(context, (Resource) result, downloadFileBuf);
      }
      WebUtils.downloadFile(context, ResourceUtils.getResource((File) result), downloadFileBuf);
    }
  }

}
