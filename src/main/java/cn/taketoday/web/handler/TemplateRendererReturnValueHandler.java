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
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

import cn.taketoday.core.OrderedSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.WebUtils;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.view.RedirectModel;
import cn.taketoday.web.view.RedirectModelManager;
import cn.taketoday.web.view.template.DefaultTemplateRenderer;
import cn.taketoday.web.view.template.TemplateRenderer;

/**
 * @author TODAY 2019-07-14 11:32
 */
public class TemplateRendererReturnValueHandler
        extends OrderedSupport implements ReturnValueHandler {

  private boolean allowLambdaDetect;
  /** Template renderer */
  private final TemplateRenderer templateRenderer;
  /** @since 4.0 */
  @Nullable
  private RedirectModelManager modelManager;

  public TemplateRendererReturnValueHandler() {
    this(new DefaultTemplateRenderer());
  }

  public TemplateRendererReturnValueHandler(TemplateRenderer templateRenderer) {
    this(false, templateRenderer);
  }

  public TemplateRendererReturnValueHandler(boolean lambdaDetect, TemplateRenderer templateRenderer) {
    Assert.notNull(templateRenderer, "templateRenderer must not be null");
    this.allowLambdaDetect = lambdaDetect;
    this.templateRenderer = templateRenderer;
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
              return WebUtils.isResponseBody(declaredMethod);
            }
          }
        }
      }
    }
    return false;
  }

  public static boolean supportsHandlerMethod(final HandlerMethod handlerMethod) {
    if (handlerMethod.isReturn(String.class)) {
      return handlerMethod.isResponseBody();
    }
    return false;
  }

  @Override
  public boolean supportsReturnValue(Object returnValue) {
    return returnValue instanceof String;
  }

  @Override
  public void handleReturnValue(
          RequestContext context, final Object handler, final Object returnValue) throws IOException {
    if (returnValue instanceof String) {
      renderTemplate((String) returnValue, context);
    }
  }

  /**
   * use template-renderer render template to response
   *
   * @param templateName template name
   * @param context request context
   * @throws IOException If any {@link IOException} occurred when render template
   * @see TemplateRenderer#render(String, RequestContext)
   */
  public void renderTemplate(String templateName, RequestContext context) throws IOException {
    final RedirectModelManager modelManager = getModelManager();
    if (modelManager != null) { // @since 3.0.3 checking model manager
      final RedirectModel redirectModel = modelManager.getModel(context);
      if (redirectModel != null) {
        context.setAttributes(redirectModel.asMap());
        modelManager.saveRedirectModel(context, null);
      }
    }
    templateRenderer.render(templateName, context);
  }

  public boolean isAllowLambdaDetect() {
    return allowLambdaDetect;
  }

  public void setAllowLambdaDetect(boolean allowLambdaDetect) {
    this.allowLambdaDetect = allowLambdaDetect;
  }

  /** @since 4.0 */
  public TemplateRenderer getTemplateRenderer() {
    return templateRenderer;
  }

  /** @since 4.0 */
  public void setModelManager(@Nullable RedirectModelManager modelManager) {
    this.modelManager = modelManager;
  }

  /** @since 4.0 */
  @Nullable
  public RedirectModelManager getModelManager() {
    return modelManager;
  }
}
