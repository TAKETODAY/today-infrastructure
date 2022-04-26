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

package cn.taketoday.web.handler.result;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.async.WebAsyncTask;
import cn.taketoday.web.context.async.WebAsyncUtils;
import cn.taketoday.web.handler.method.HandlerMethod;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/9 12:03
 */
public class AsyncTaskMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

  @Nullable
  private final BeanFactory beanFactory;

  public AsyncTaskMethodReturnValueHandler(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof WebAsyncTask;
  }

  @Override
  public void handleReturnValue(RequestContext context, Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue instanceof WebAsyncTask task) {
      if (this.beanFactory != null) {
        task.setBeanFactory(this.beanFactory);
      }
      WebAsyncUtils.getAsyncManager(context).startCallableProcessing(task, context.getBindingContext());
    }
  }

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isReturnTypeAssignableTo(WebAsyncTask.class);
  }

}
