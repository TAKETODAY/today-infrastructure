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

package cn.taketoday.web.view;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.handler.HandlerMethodReturnValueHandler;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.view.template.TemplateRenderingException;

/**
 * view-name or {@link View} ReturnValueHandler
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see View
 * @since 4.0 2022/2/9 20:34
 */
public class ViewReturnValueHandler
        extends HandlerMethodReturnValueHandler implements ReturnValueHandler {

  private final ViewResolver viewResolver;

  @Nullable
  private RedirectModelManager modelManager;

  public ViewReturnValueHandler(ViewResolver viewResolver) {
    Assert.notNull(viewResolver, "viewResolver is required");
    this.viewResolver = viewResolver;
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof String || returnValue instanceof View;
  }

  @Override
  protected boolean supportsHandlerMethod(HandlerMethod handler) {
    if (handler.isReturn(String.class)) {
      return !handler.isResponseBody();
    }
    return handler.isReturnTypeAssignableTo(View.class);
  }

  public static boolean supportsLambda(@Nullable Object handler) {
    if (handler != null) {
      Class<?> handlerClass = handler.getClass();
      Method method = ReflectionUtils.findMethod(handlerClass, "writeReplace");
      if (method != null) {
        ReflectionUtils.makeAccessible(method);

        Object returnValue = ReflectionUtils.invokeMethod(method, handler);
        if (returnValue instanceof SerializedLambda lambda) {
          Class<?> implClass = ClassUtils.load(lambda.getImplClass().replace('/', '.'));
          if (implClass != null) {
            Method declaredMethod = ReflectionUtils.findMethod(implClass, lambda.getImplMethodName(), RequestContext.class);
            if (declaredMethod != null) {
              return HandlerMethod.isResponseBody(declaredMethod);
            }
          }
        }
      }
    }
    return false;
  }

  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, Object returnValue) throws Exception {
    View view;
    if (returnValue instanceof String viewName) {
      Locale locale = RequestContextUtils.getLocale(context);
      view = viewResolver.resolveViewName(viewName, locale);
      if (view == null) {
        throw new TemplateRenderingException(
                "Could not resolve view with name '" + viewName + "' in handler '" + handler + "'");
      }
    }
    else {
      view = (View) returnValue;
    }
    renderView(context, view);
  }

  public void renderView(RequestContext context, View view) throws Exception {
    LinkedHashMap<String, Object> model = new LinkedHashMap<>();
    RedirectModel redirectModel = RequestContextUtils.getInputRedirectModel(context, modelManager);
    if (redirectModel != null) {
      model.putAll(redirectModel.asMap());
    }

    if (context.hasModelAndView()) {
      ModelAndView modelAndView = context.modelAndView();
      model.putAll(modelAndView.asMap());
    }
    view.render(model, context);
  }

  public void setModelManager(@Nullable RedirectModelManager modelManager) {
    this.modelManager = modelManager;
  }

  @Nullable
  public RedirectModelManager getModelManager() {
    return modelManager;
  }

  public ViewResolver getViewResolver() {
    return viewResolver;
  }

}
