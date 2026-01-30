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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import infra.beans.BeanUtils;
import infra.core.Conventions;
import infra.core.GenericTypeResolver;
import infra.core.MethodParameter;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.ui.Model;
import infra.ui.ModelMap;
import infra.util.StringUtils;
import infra.validation.BindingResult;
import infra.web.BindingContext;
import infra.web.RequestContext;
import infra.web.bind.RequestContextDataBinder;
import infra.web.bind.annotation.ModelAttribute;

/**
 * Assist with initialization of the {@link Model} before controller method
 * invocation and with updates to it after the invocation.
 *
 * <p>On initialization the model is populated with attributes temporarily stored
 * in the session and through the invocation of {@code @ModelAttribute} methods.
 *
 * <p>On update model attributes are synchronized with the session and also
 * {@link BindingResult} attributes are added if missing.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:00
 */
final class ModelHandler {

  private static final Logger log = LoggerFactory.getLogger(ModelHandler.class);

  private final ControllerMethodResolver methodResolver;

  /**
   * Create a new instance with the given {@code @ModelAttribute} methods.
   */
  public ModelHandler(ControllerMethodResolver methodResolver) {
    this.methodResolver = methodResolver;
  }

  /**
   * Populate the model in the following order:
   * <ol>
   * <li>Retrieve "known" session attributes listed as {@code @SessionAttributes}.
   * <li>Invoke {@code @ModelAttribute} methods
   * <li>Find {@code @ModelAttribute} method arguments also listed as
   * {@code @SessionAttributes} and ensure they're present in the model raising
   * an exception if necessary.
   * </ol>
   *
   * @param request the current request
   * @param container a container with the model to be initialized
   * @param handlerMethod the method for which the model is initialized
   * @throws Exception may arise from {@code @ModelAttribute} methods
   */
  public void initModel(RequestContext request, BindingContext container, HandlerMethod handlerMethod) throws Throwable {
    ArrayList<ModelMethod> modelMethods = getModelMethods(handlerMethod);
    if (modelMethods != null) {
      invokeModelAttributeMethods(request, container, modelMethods);
    }
  }

  @Nullable
  private ArrayList<ModelMethod> getModelMethods(HandlerMethod handlerMethod) {
    List<InvocableHandlerMethod> handlerMethods = methodResolver.getModelAttributeMethods(handlerMethod);
    if (!handlerMethods.isEmpty()) {
      ArrayList<ModelMethod> modelMethods = new ArrayList<>();
      for (InvocableHandlerMethod method : handlerMethods) {
        modelMethods.add(new ModelMethod(method));
      }
      return modelMethods;
    }
    return null;
  }

  /**
   * Invoke model attribute methods to populate the model.
   * Attributes are added only if not already present in the model.
   */
  private void invokeModelAttributeMethods(RequestContext request,
          BindingContext container, ArrayList<ModelMethod> modelMethods) throws Throwable {

    while (!modelMethods.isEmpty()) {
      InvocableHandlerMethod modelMethod = getNextModelMethod(container, modelMethods).handlerMethod;
      ModelAttribute ann = modelMethod.getMethodAnnotation(ModelAttribute.class);
      Assert.state(ann != null, "No ModelAttribute annotation");
      if (container.containsAttribute(ann.name())) {
        if (!ann.binding()) {
          container.setBindingDisabled(ann.name());
        }
        continue;
      }

      Object returnValue = modelMethod.invokeForRequest(request, (Object[]) null);
      if (modelMethod.isVoid()) {
        if (StringUtils.hasText(ann.value())) {
          if (log.isDebugEnabled()) {
            log.debug("Name in @ModelAttribute is ignored because method returns void: {}",
                    modelMethod.getShortLogMessage());
          }
        }
        continue;
      }

      String returnValueName = getNameForReturnValue(returnValue, modelMethod);
      if (!ann.binding()) {
        container.setBindingDisabled(returnValueName);
      }
      if (!container.containsAttribute(returnValueName)) {
        container.addAttribute(returnValueName, returnValue);
      }
    }
  }

  private ModelMethod getNextModelMethod(BindingContext container, ArrayList<ModelMethod> modelMethods) {
    for (ModelMethod modelMethod : modelMethods) {
      if (modelMethod.checkDependencies(container)) {
        modelMethods.remove(modelMethod);
        return modelMethod;
      }
    }
    ModelMethod modelMethod = modelMethods.get(0);
    modelMethods.remove(modelMethod);
    return modelMethod;
  }

  /**
   * Promote model attributes listed as {@code @SessionAttributes} to the session.
   * Add {@link BindingResult} attributes where necessary.
   *
   * @param request the current request
   * @param container contains the model to update
   * @throws Exception if creating BindingResult attributes fails
   */
  public void updateModel(RequestContext request, BindingContext container) throws Throwable {
    if (container.hasModel()) {
      ModelMap model = container.getModel();
      updateBindingResult(request, container, model);
    }
  }

  /**
   * Add {@link BindingResult} attributes to the model for attributes that require it.
   */
  private void updateBindingResult(RequestContext request, BindingContext bindingContext, ModelMap model) throws Throwable {
    for (String name : new ArrayList<>(model.keySet())) {
      Object value = model.get(name);
      if (value != null && isBindingCandidate(name, value)) {
        String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + name;
        if (!model.containsAttribute(bindingResultKey)) {
          RequestContextDataBinder dataBinder = bindingContext.createBinder(request, value, name);
          model.put(bindingResultKey, dataBinder.getBindingResult());
        }
      }
    }
  }

  /**
   * Whether the given attribute requires a {@link BindingResult} in the model.
   */
  private boolean isBindingCandidate(String attributeName, Object value) {
    if (attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
      return false;
    }

    return !value.getClass().isArray() && !(value instanceof Collection)
            && !(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass());
  }

  /**
   * Derive the model attribute name for the given method parameter based on
   * a {@code @ModelAttribute} parameter annotation (if present) or falling
   * back on parameter type based conventions.
   *
   * @param parameter a descriptor for the method parameter
   * @return the derived name
   * @see Conventions#getVariableNameForParameter(MethodParameter)
   */
  public static String getNameForParameter(MethodParameter parameter) {
    ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
    String name = ann != null ? ann.value() : null;
    return StringUtils.hasText(name) ? name : Conventions.getVariableNameForParameter(parameter);
  }

  /**
   * Derive the model attribute name for the given return value. Results will be
   * based on:
   * <ol>
   * <li>the method {@code ModelAttribute} annotation value
   * <li>the declared return type if it is more specific than {@code Object}
   * <li>the actual return value type
   * </ol>
   *
   * @param returnValue the value returned from a method invocation
   * @param handler a descriptor for the method
   * @return the derived name (never {@code null} or empty String)
   */
  public static String getNameForReturnValue(@Nullable Object returnValue, HandlerMethod handler) {
    ModelAttribute ann = handler.getMethodAnnotation(ModelAttribute.class);
    if (ann != null && StringUtils.hasText(ann.value())) {
      return ann.value();
    }
    else {
      MethodParameter returnType = handler.getReturnType();
      Method method = returnType.getMethod();
      Assert.state(method != null, "No handler method");
      Class<?> containingClass = returnType.getContainingClass();
      Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
      return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
    }
  }

  private static class ModelMethod {

    public final InvocableHandlerMethod handlerMethod;
    public final HashSet<String> dependencies = new HashSet<>();

    public ModelMethod(InvocableHandlerMethod handlerMethod) {
      this.handlerMethod = handlerMethod;
      for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
        if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
          dependencies.add(getNameForParameter(parameter));
        }
      }
    }

    public boolean checkDependencies(BindingContext mavContainer) {
      for (String name : dependencies) {
        if (!mavContainer.containsAttribute(name)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      return this.handlerMethod.getMethod().toGenericString();
    }
  }

}
