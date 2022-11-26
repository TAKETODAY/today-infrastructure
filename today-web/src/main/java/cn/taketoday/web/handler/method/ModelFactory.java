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

package cn.taketoday.web.handler.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.core.Conventions;
import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.session.WebSessionRequiredException;
import cn.taketoday.ui.Model;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.annotation.ModelAttribute;

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
public final class ModelFactory {

  private static final Logger logger = LoggerFactory.getLogger(ModelFactory.class);

  @Nullable
  private ArrayList<ModelMethod> modelMethods;

  private final SessionAttributesHandler sessionAttributesHandler;

  /**
   * Create a new instance with the given {@code @ModelAttribute} methods.
   *
   * @param handlerMethods the {@code @ModelAttribute} methods to invoke
   * @param attributeHandler for access to session attributes
   */
  public ModelFactory(@Nullable List<InvocableHandlerMethod> handlerMethods,
          SessionAttributesHandler attributeHandler) {
    if (handlerMethods != null) {
      ArrayList<ModelMethod> modelMethods = new ArrayList<>();
      for (InvocableHandlerMethod handlerMethod : handlerMethods) {
        modelMethods.add(new ModelMethod(handlerMethod));
      }
      this.modelMethods = modelMethods;
    }
    this.sessionAttributesHandler = attributeHandler;
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
  public void initModel(RequestContext request, BindingContext container, HandlerMethod handlerMethod)
          throws Throwable {

    Map<String, ?> sessionAttributes = sessionAttributesHandler.retrieveAttributes(request);
    container.mergeAttributes(sessionAttributes);
    invokeModelAttributeMethods(request, container);

    for (String name : findSessionAttributeArguments(handlerMethod)) {
      if (!container.containsAttribute(name)) {
        Object value = sessionAttributesHandler.retrieveAttribute(request, name);
        if (value == null) {
          throw new WebSessionRequiredException("Expected session attribute '" + name + "'", name);
        }
        container.addAttribute(name, value);
      }
    }
  }

  /**
   * Invoke model attribute methods to populate the model.
   * Attributes are added only if not already present in the model.
   */
  private void invokeModelAttributeMethods(
          RequestContext request, BindingContext container) throws Throwable {
    ArrayList<ModelMethod> modelMethods = this.modelMethods;
    if (modelMethods == null) {
      return;
    }
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

      Object returnValue = modelMethod.invokeForRequest(request, container);
      if (modelMethod.isVoid()) {
        if (StringUtils.hasText(ann.value())) {
          if (logger.isDebugEnabled()) {
            logger.debug("Name in @ModelAttribute is ignored because method returns void: {}",
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
   * Find {@code @ModelAttribute} arguments also listed as {@code @SessionAttributes}.
   */
  private List<String> findSessionAttributeArguments(HandlerMethod handlerMethod) {
    ArrayList<String> result = new ArrayList<>();
    for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
      if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
        String name = getNameForParameter(parameter);
        Class<?> paramType = parameter.getParameterType();
        if (this.sessionAttributesHandler.isHandlerSessionAttribute(name, paramType)) {
          result.add(name);
        }
      }
    }
    return result;
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
    Model model = container.getModel();
    if (container.getSessionStatus().isComplete()) {
      sessionAttributesHandler.cleanupAttributes(request);
    }
    else {
      sessionAttributesHandler.storeAttributes(request, model.asMap());
    }

    updateBindingResult(request, container, model.asMap());
  }

  /**
   * Add {@link BindingResult} attributes to the model for attributes that require it.
   */
  private void updateBindingResult(RequestContext request,
          BindingContext bindingContext, Map<String, Object> model) throws Throwable {
    ArrayList<String> keyNames = new ArrayList<>(model.keySet());
    for (String name : keyNames) {
      Object value = model.get(name);
      if (value != null && isBindingCandidate(name, value)) {
        String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + name;
        if (!model.containsKey(bindingResultKey)) {
          WebDataBinder dataBinder = bindingContext.createBinder(request, value, name);
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

    if (sessionAttributesHandler.isHandlerSessionAttribute(attributeName, value.getClass())) {
      return true;
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
      Method method = handler.getMethod();
      Class<?> containingClass = method.getDeclaringClass();
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
