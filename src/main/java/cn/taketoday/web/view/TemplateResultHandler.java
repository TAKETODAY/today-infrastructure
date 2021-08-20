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

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.view.template.DefaultTemplateViewResolver;
import cn.taketoday.web.view.template.TemplateViewResolver;

/**
 * @author TODAY <br>
 * 2019-07-14 11:32
 */
public class TemplateResultHandler extends AbstractResultHandler implements RuntimeResultHandler {

  private boolean allowLambdaDetect;

  public TemplateResultHandler() {
    this(false, new DefaultTemplateViewResolver());
  }

  public TemplateResultHandler(TemplateViewResolver viewResolver) {
    this(false, viewResolver);
  }

  public TemplateResultHandler(boolean lambdaDetect, TemplateViewResolver viewResolver) {
    setAllowLambdaDetect(lambdaDetect);
    setTemplateViewResolver(viewResolver);
  }

  @Override
  public boolean supportsHandler(Object handler) {
    if (handler instanceof HandlerMethod) {
      return supportsHandlerMethod((HandlerMethod) handler);
    }
    return isAllowLambdaDetect() && supportsLambda(handler);
  }

  public static boolean supportsLambda(final Object handler) {
    if (handler != null) {
      try {
        final Method m = ReflectionUtils.makeAccessible(handler.getClass().getDeclaredMethod("writeReplace"));

        final SerializedLambda lambda = (SerializedLambda) m.invoke(handler);
        final Class<?> implClass = ClassUtils.loadClass(lambda.getImplClass().replace('/', '.'));

        final Method declaredMethod = implClass.getDeclaredMethod(lambda.getImplMethodName(), RequestContext.class);

        if (declaredMethod.isAnnotationPresent(ResponseBody.class)) {
          return !declaredMethod.getAnnotation(ResponseBody.class).value();
        }
        else {
          final Class<?> declaringClass = declaredMethod.getDeclaringClass();
          if (declaringClass.isAnnotationPresent(ResponseBody.class)) {
            return !declaringClass.getAnnotation(ResponseBody.class).value();
          }
        }
      }
      catch (Exception ignored) {}
    }
    return false;
  }

  public static boolean supportsHandlerMethod(final HandlerMethod handlerMethod) {

    if (handlerMethod.is(String.class)) {
      if (handlerMethod.isMethodPresent(ResponseBody.class)) {
        return !handlerMethod.getMethodAnnotation(ResponseBody.class).value();
      }
      else if (handlerMethod.isDeclaringClassPresent(ResponseBody.class)) {
        return !handlerMethod.getDeclaringClassAnnotation(ResponseBody.class).value();
      }
      return true;
    }
    return false;
  }

  @Override
  public void handleResult(final RequestContext context,
                           final Object handler, final Object result) throws Throwable {
    handleString((String) result, context);
  }

  public boolean isAllowLambdaDetect() {
    return allowLambdaDetect;
  }

  public void setAllowLambdaDetect(boolean allowLambdaDetect) {
    this.allowLambdaDetect = allowLambdaDetect;
  }

}
