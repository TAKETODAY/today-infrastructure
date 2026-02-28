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

package infra.web;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import infra.core.ResolvableType;
import infra.ui.Model;
import infra.ui.ModelMap;
import infra.validation.BindException;
import infra.validation.BindingResult;
import infra.validation.Errors;
import infra.web.bind.EscapedErrors;
import infra.web.bind.RequestContextDataBinder;
import infra.web.bind.support.BindParamNameResolver;
import infra.web.bind.support.WebBindingInitializer;
import infra.web.view.BindingAwareModelMap;
import infra.web.view.ModelAndView;

/**
 * Context to assist with binding request data onto Objects and provide access
 * to a shared {@link Model} with controller-specific attributes.
 *
 * <p>Provides methods to create a {@link RequestContextDataBinder} for a specific
 * target, command Object to apply data binding and validation to, or without a
 * target Object for simple type conversion from request values.
 *
 * <p>Container for the default model for the request.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:40
 */
public class BindingContext {

  private final @Nullable WebBindingInitializer initializer;

  private @Nullable ModelMap model;

  private @Nullable RedirectModel redirectModel;

  private @Nullable Set<String> noBinding;

  private @Nullable Set<String> bindingDisabled;

  protected @Nullable ModelAndView modelAndView;

  private @Nullable Map<String, Errors> errorsMap;

  /**
   * Create a new {@code BindingContext}.
   */
  public BindingContext() {
    this(null);
  }

  /**
   * Create a new {@code BindingContext} with the given initializer.
   *
   * @param initializer the binding initializer to apply (may be {@code null})
   */
  public BindingContext(@Nullable WebBindingInitializer initializer) {
    this.initializer = initializer;
  }

  /**
   * Create a {@link RequestContextDataBinder} without a target object for type
   * conversion of request values to simple types.
   *
   * @param context the current exchange
   * @param objectName the name of the target object
   * @return the created data binder
   * @throws Throwable if {@code @InitBinder} method invocation fails
   */
  public RequestContextDataBinder createBinder(RequestContext context, String objectName) throws Throwable {
    return createBinder(context, null, objectName, null);
  }

  /**
   * Create a {@link RequestContextDataBinder} to apply data binding and
   * validation with on the target, command object.
   *
   * @param request the current request
   * @param target the object to create a data binder for
   * @param objectName the name of the target object
   * @return the created data binder
   * @throws Throwable if {@code @InitBinder} method invocation fails
   */
  public RequestContextDataBinder createBinder(RequestContext request, @Nullable Object target, String objectName) throws Throwable {
    return createBinder(request, target, objectName, null);
  }

  /**
   * Variant of {@link #createBinder(RequestContext, Object, String)} with a
   * {@link ResolvableType} for which the {@code DataBinder} is created.
   * This may be used to construct the target, or otherwise provide more
   * insight on how to initialize the binder.
   */
  public RequestContextDataBinder createBinder(RequestContext request, @Nullable Object target,
          String objectName, @Nullable ResolvableType targetType) throws Throwable {

    RequestContextDataBinder dataBinder = createBinderInstance(target, objectName, request);
    dataBinder.setNameResolver(new BindParamNameResolver());

    if (target == null && targetType != null) {
      dataBinder.setTargetType(targetType);
    }

    if (initializer != null) {
      initializer.initBinder(dataBinder);
    }
    initBinder(dataBinder, request);

    return dataBinder;
  }

  /**
   * Extension point to create the WebDataBinder instance.
   * By default this is {@code WebDataBinder}.
   *
   * @param target the binding target or {@code null} for type conversion only
   * @param objectName the binding target object name
   * @param request the current request
   * @throws Exception in case of invalid state or arguments
   */
  protected RequestContextDataBinder createBinderInstance(@Nullable Object target, String objectName, RequestContext request) throws Exception {
    return new RequestContextDataBinder(target, objectName);
  }

  /**
   * Initialize the data binder instance for the given exchange.
   *
   * @throws Throwable if {@code @InitBinder} method invocation fails
   */
  public void initBinder(RequestContextDataBinder dataBinder, RequestContext request) throws Throwable {
  }

  /**
   * Get a {@link ModelAndView}
   * <p>
   * If there isn't a {@link ModelAndView} in this {@link RequestContext},
   * <b>Create One</b>
   *
   * @return Returns {@link ModelAndView}
   */
  public ModelAndView getModelAndView() {
    if (modelAndView == null) {
      this.modelAndView = new ModelAndView();
    }
    return modelAndView;
  }

  /**
   * Check if a {@link ModelAndView} instance exists in this context.
   *
   * @return {@code true} if a {@link ModelAndView} is present, {@code false} otherwise
   * @since 3.0
   */
  public boolean hasModelAndView() {
    return modelAndView != null;
  }

  /**
   * Check if a model exists in this context.
   *
   * @return {@code true} if a model is present, {@code false} otherwise
   * @since 4.0
   */
  public boolean hasModel() {
    return model != null;
  }

  /**
   * Return the default model.
   */
  public ModelMap getModel() {
    ModelMap model = this.model;
    if (model == null) {
      model = new BindingAwareModelMap();
      this.model = model;
    }
    return model;
  }

  /**
   * Promote model attributes listed as {@code @SessionAttributes} to the session.
   * Add {@link BindingResult} attributes where necessary.
   *
   * @param request the current request
   * @throws Throwable if creating BindingResult attributes fails
   */
  public void updateModel(RequestContext request) throws Throwable {
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
   * @throws Throwable may arise from {@code @ModelAttribute} methods
   */
  public void initModel(RequestContext request) throws Throwable {
  }

  /**
   * Get the redirect model associated with this binding context.
   *
   * @return the redirect model, or {@code null} if none is set
   */
  public @Nullable RedirectModel getRedirectModel() {
    return redirectModel;
  }

  /**
   * Set the redirect model for this binding context.
   *
   * @param redirectModel the redirect model to set, or {@code null} to clear it
   */
  public void setRedirectModel(@Nullable RedirectModel redirectModel) {
    this.redirectModel = redirectModel;
  }

  /**
   * Programmatically register an attribute for which data binding should not occur,
   * not even for a subsequent {@code @ModelAttribute} declaration.
   *
   * @param attributeName the name of the attribute
   */
  public void setBindingDisabled(String attributeName) {
    if (bindingDisabled == null) {
      bindingDisabled = new HashSet<>(4);
    }
    bindingDisabled.add(attributeName);
  }

  /**
   * Whether binding is disabled for the given model attribute.
   */
  public boolean isBindingDisabled(String name) {
    if (bindingDisabled != null) {
      if (noBinding != null) {
        return bindingDisabled.contains(name) || noBinding.contains(name);
      }
      return bindingDisabled.contains(name);
    }
    else if (noBinding != null) {
      return noBinding.contains(name);
    }
    return false;
  }

  /**
   * Register whether data binding should occur for a corresponding model attribute,
   * corresponding to an {@code @ModelAttribute(binding=true/false)} declaration.
   * <p>Note: While this flag will be taken into account by {@link #isBindingDisabled},
   * a hard {@link #setBindingDisabled} declaration will always override it.
   *
   * @param attributeName the name of the attribute
   */
  public void setBinding(String attributeName, boolean enabled) {
    if (noBinding == null) {
      noBinding = new HashSet<>(4);
    }
    if (!enabled) {
      noBinding.add(attributeName);
    }
    else {
      noBinding.remove(attributeName);
    }
  }

  /**
   * Add the supplied attribute to the underlying model.
   * A shortcut for {@code getModel().addAttribute(String, Object)}.
   */
  public BindingContext addAttribute(String name, @Nullable Object value) {
    getModel().addAttribute(name, value);
    return this;
  }

  /**
   * Add the supplied attribute to the underlying model.
   * A shortcut for {@code getModel().addAttribute(Object)}.
   */
  public BindingContext addAttribute(Object value) {
    getModel().addAttribute(value);
    return this;
  }

  /**
   * Copy all attributes to the underlying model.
   * A shortcut for {@code getModel().addAllAttributes(Map)}.
   */
  public BindingContext addAllAttributes(@Nullable Map<String, ?> attributes) {
    if (attributes != null) {
      getModel().addAllAttributes(attributes);
    }
    return this;
  }

  /**
   * Copy attributes in the supplied {@code Map} with existing objects of
   * the same name taking precedence (i.e. not getting replaced).
   * A shortcut for {@code getModel().mergeAttributes(Map<String, ?>)}.
   */
  public BindingContext mergeAttributes(@Nullable Map<String, ?> attributes) {
    if (attributes != null) {
      getModel().mergeAttributes(attributes);
    }
    return this;
  }

  /**
   * Remove the given attributes from the model.
   */
  public BindingContext removeAttributes(@Nullable Map<String, ?> attributes) {
    if (attributes != null && hasModel()) {
      ModelMap modelMap = getModel();
      for (String key : attributes.keySet()) {
        modelMap.removeAttribute(key);
      }
    }
    return this;
  }

  /**
   * Whether the underlying model contains the given attribute name.
   * A shortcut for {@code getModel().containsAttribute(String)}.
   */
  public boolean containsAttribute(String name) {
    return hasModel() && getModel().containsAttribute(name);
  }

  /**
   * Retrieve the Errors instance for the given bind object.
   *
   * @param name the name of the bind object
   * @param htmlEscape create an Errors instance with automatic HTML escaping?
   * @return the Errors instance, or {@code null} if not found
   * @since 5.0
   */
  public @Nullable Errors getErrors(String name, boolean htmlEscape) {
    if (this.errorsMap == null) {
      this.errorsMap = new HashMap<>();
    }
    Errors errors = this.errorsMap.get(name);
    boolean put = false;
    if (errors == null) {
      if (model != null && model.get(BindingResult.MODEL_KEY_PREFIX + name) instanceof Errors e) {
        errors = e;
      }
      // Check old BindException prefix for backwards compatibility.
      if (errors instanceof BindException be) {
        errors = be.getBindingResult();
      }
      if (errors == null) {
        return null;
      }
      put = true;
    }
    if (htmlEscape && !(errors instanceof EscapedErrors)) {
      errors = new EscapedErrors(errors);
      put = true;
    }
    else if (!htmlEscape && errors instanceof EscapedErrors ee) {
      errors = ee.getSource();
      put = true;
    }
    if (put) {
      this.errorsMap.put(name, errors);
    }
    return errors;
  }

  /**
   * Return diagnostic information.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("BindingContext: model: ");
    sb.append(model);

    RedirectModel redirectModel = getRedirectModel();
    if (redirectModel != null) {
      sb.append("; redirect model ");
      sb.append(redirectModel);
    }
    return sb.toString();
  }

}
