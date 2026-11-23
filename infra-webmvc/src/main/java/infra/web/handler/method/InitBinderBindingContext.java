/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.util.Assert;
import infra.util.ObjectUtils;
import infra.web.BindingContext;
import infra.web.HttpContext;
import infra.web.bind.WebDataBinder;
import infra.web.bind.annotation.InitBinder;
import infra.web.bind.support.WebBindingInitializer;

/**
 * Extends {@link BindingContext} with {@code @InitBinder} method initialization.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/26 14:24
 */
public class InitBinderBindingContext extends BindingContext {

  private final ModelHandler modelHandler;

  private final InvocableHandlerMethod handlerMethod;

  private final ControllerMethodResolver methodResolver;

  private final @Nullable WebBindingInitializer initializer;

  private @Nullable BindingContext binderMethodContext;

  private @Nullable List<InvocableHandlerMethod> binderMethods;

  /**
   * Create a new InitBinderBindingContext instance.
   *
   * @param initializer for global data binder initialization
   */
  InitBinderBindingContext(ModelHandler modelHandler, @Nullable WebBindingInitializer initializer,
          ControllerMethodResolver methodResolver, InvocableHandlerMethod handlerMethod) {

    this(modelHandler, initializer, methodResolver, null, handlerMethod);
  }

  /**
   * Create a new InitBinderBindingContext instance.
   *
   * @param initializer for global data binder initialization
   */
  InitBinderBindingContext(ModelHandler modelHandler, @Nullable WebBindingInitializer initializer,
          ControllerMethodResolver methodResolver, @Nullable List<InvocableHandlerMethod> binderMethods, InvocableHandlerMethod handlerMethod) {

    super(initializer);
    this.modelHandler = modelHandler;
    this.handlerMethod = handlerMethod;
    this.binderMethods = binderMethods;
    this.methodResolver = methodResolver;
    this.initializer = initializer;
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
  public void initBinder(WebDataBinder dataBinder, HttpContext context) throws Exception {
    List<InvocableHandlerMethod> binderMethods = this.binderMethods;
    if (binderMethods == null) {
      binderMethods = methodResolver.getBinderMethods(handlerMethod);
      this.binderMethods = binderMethods;
    }

    if (binderMethods.isEmpty()) {
      return;
    }

    BindingContext originalContext = null;
    BindingContext methodContext = null;
    try {
      for (InvocableHandlerMethod binderMethod : binderMethods) {
        if (isBinderMethodApplicable(binderMethod, dataBinder)) {
          if (methodContext == null) {
            methodContext = this.binderMethodContext;
            if (methodContext == null) {
              methodContext = new BindingContext(this.initializer);
              this.binderMethodContext = methodContext;
            }
            originalContext = BindingContext.find(context);
            BindingContext.set(context, methodContext);
          }
          Object returnValue = binderMethod.invokeForRequest(context, null, new Object[] { dataBinder });
          if (returnValue != null) {
            throw new IllegalStateException(
                    "@InitBinder methods must not return a value (should be void): " + binderMethod);
          }
          // Should not happen (no Model argument resolution) ...
          if (!methodContext.getModel().isEmpty()) {
            throw new IllegalStateException(
                    "@InitBinder methods are not allowed to add model attributes: " + binderMethod);
          }
        }
      }
    }
    finally {
      if (methodContext != null) {
        BindingContext.set(context, originalContext);
      }
    }
  }

  /**
   * Determine whether the given {@code @InitBinder} method should be used
   * to initialize the given {@link WebDataBinder} instance. By default we
   * check the specified attribute names in the annotation value, if any.
   */
  protected boolean isBinderMethodApplicable(HandlerMethod initBinderMethod, WebDataBinder dataBinder) {
    InitBinder ann = initBinderMethod.getAnnotation(InitBinder.class);
    Assert.state(ann != null, "No InitBinder annotation");
    String[] names = ann.value();
    return ObjectUtils.isEmpty(names) || ObjectUtils.containsElement(names, dataBinder.getObjectName());
  }

  @Override
  public void updateModel(HttpContext context) throws Exception {
    modelHandler.updateModel(context, this);
  }

  @Override
  public void initModel(HttpContext context) throws Exception {
    modelHandler.initModel(context, this, handlerMethod);
  }

}
