/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.handler.method;

import java.util.List;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.annotation.InitBinder;
import cn.taketoday.web.bind.support.WebBindingInitializer;

/**
 * Extends {@link BindingContext} with {@code @InitBinder} method initialization.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/26 14:24
 */
public class InitBinderBindingContext extends BindingContext {
  private final Class<?> beanType;

  private final ModelHandler modelHandler;

  private final HandlerMethod handlerMethod;

  private final BindingContext binderMethodContext;

  private final ControllerMethodResolver methodResolver;

  @Nullable
  private List<InvocableHandlerMethod> binderMethods;

  /**
   * Create a new InitBinderBindingContext instance.
   *
   * @param initializer for global data binder initialization
   */
  InitBinderBindingContext(ModelHandler modelHandler, @Nullable WebBindingInitializer initializer,
          ControllerMethodResolver methodResolver, HandlerMethod handlerMethod) {

    super(initializer);
    this.modelHandler = modelHandler;
    this.handlerMethod = handlerMethod;
    this.methodResolver = methodResolver;
    this.beanType = handlerMethod.getBeanType();
    this.binderMethodContext = new BindingContext(initializer);
  }

  /**
   * Create a new InitBinderBindingContext instance.
   *
   * @param initializer for global data binder initialization
   */
  InitBinderBindingContext(ModelHandler modelHandler, @Nullable WebBindingInitializer initializer,
          ControllerMethodResolver methodResolver, List<InvocableHandlerMethod> binderMethods, HandlerMethod handlerMethod) {

    super(initializer);
    this.modelHandler = modelHandler;
    this.handlerMethod = handlerMethod;
    this.binderMethods = binderMethods;
    this.methodResolver = methodResolver;
    this.beanType = handlerMethod.getBeanType();
    this.binderMethodContext = new BindingContext(initializer);
  }

  /**
   * Initialize a WebDataBinder with {@code @InitBinder} methods.
   * <p>If the {@code @InitBinder} annotation specifies attributes names,
   * it is invoked only if the names include the target object name.
   *
   * @throws Exception if one of the invoked @{@link InitBinder} methods fails
   * @see #isBinderMethodApplicable(HandlerMethod, WebDataBinder)
   */
  @Override
  public void initBinder(WebDataBinder dataBinder, RequestContext request) throws Throwable {
    List<InvocableHandlerMethod> binderMethods = this.binderMethods;
    if (binderMethods == null) {
      binderMethods = methodResolver.getBinderMethods(handlerMethod);
      this.binderMethods = binderMethods;
    }

    if (!binderMethods.isEmpty()) {
      BindingContext bindingContext = request.getBinding();
      request.setBinding(binderMethodContext);
      for (InvocableHandlerMethod binderMethod : binderMethods) {
        if (isBinderMethodApplicable(binderMethod, dataBinder)) {
          Object returnValue = binderMethod.invokeForRequest(request, dataBinder);
          if (returnValue != null) {
            throw new IllegalStateException(
                    "@InitBinder methods must not return a value (should be void): " + binderMethod);
          }
          // Should not happen (no Model argument resolution) ...
          if (!binderMethodContext.getModel().isEmpty()) {
            throw new IllegalStateException(
                    "@InitBinder methods are not allowed to add model attributes: " + binderMethod);
          }
        }
      }
      request.setBinding(bindingContext);
    }
  }

  /**
   * Determine whether the given {@code @InitBinder} method should be used
   * to initialize the given {@link WebDataBinder} instance. By default we
   * check the specified attribute names in the annotation value, if any.
   */
  protected boolean isBinderMethodApplicable(HandlerMethod initBinderMethod, WebDataBinder dataBinder) {
    InitBinder ann = initBinderMethod.getMethodAnnotation(InitBinder.class);
    Assert.state(ann != null, "No InitBinder annotation");
    String[] names = ann.value();
    return ObjectUtils.isEmpty(names) || ObjectUtils.containsElement(names, dataBinder.getObjectName());
  }

  @Override
  public void updateModel(RequestContext request) throws Throwable {
    modelHandler.updateModel(request, this, beanType);
  }

  @Override
  public void initModel(RequestContext request) throws Throwable {
    modelHandler.initModel(request, this, handlerMethod);
  }

}
