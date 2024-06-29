/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.ui.Model;
import cn.taketoday.ui.ModelMap;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.support.BindParamNameResolver;
import cn.taketoday.web.bind.support.WebBindingInitializer;
import cn.taketoday.web.view.BindingAwareModelMap;
import cn.taketoday.web.view.ModelAndView;

/**
 * Context to assist with binding request data onto Objects and provide access
 * to a shared {@link Model} with controller-specific attributes.
 *
 * <p>Provides methods to create a {@link WebDataBinder} for a specific
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

  @Nullable
  private ModelMap model;

  @Nullable
  private RedirectModel redirectModel;

  @Nullable
  private Set<String> noBinding;

  @Nullable
  private Set<String> bindingDisabled;

  @Nullable
  private final WebBindingInitializer initializer;

  @Nullable
  protected ModelAndView modelAndView;

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
   * Create a {@link WebDataBinder} without a target object for type
   * conversion of request values to simple types.
   *
   * @param context the current exchange
   * @param objectName the name of the target object
   * @return the created data binder
   * @throws Throwable if {@code @InitBinder} method invocation fails
   */
  public WebDataBinder createBinder(RequestContext context, String objectName) throws Throwable {
    return createBinder(context, null, objectName, null);
  }

  /**
   * Create a {@link WebDataBinder} to apply data binding and
   * validation with on the target, command object.
   *
   * @param request the current request
   * @param target the object to create a data binder for
   * @param objectName the name of the target object
   * @return the created data binder
   * @throws Throwable if {@code @InitBinder} method invocation fails
   */
  public WebDataBinder createBinder(RequestContext request,
          @Nullable Object target, String objectName) throws Throwable {
    return createBinder(request, target, objectName, null);
  }

  /**
   * Variant of {@link #createBinder(RequestContext, Object, String)} with a
   * {@link ResolvableType} for which the {@code DataBinder} is created.
   * This may be used to construct the target, or otherwise provide more
   * insight on how to initialize the binder.
   */
  public WebDataBinder createBinder(RequestContext request,
          @Nullable Object target, String objectName, @Nullable ResolvableType targetType) throws Throwable {
    WebDataBinder dataBinder = createBinderInstance(target, objectName, request);
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
  protected WebDataBinder createBinderInstance(
          @Nullable Object target, String objectName, RequestContext request) throws Exception {

    return new WebDataBinder(target, objectName);
  }

  /**
   * Initialize the data binder instance for the given exchange.
   *
   * @throws Throwable if {@code @InitBinder} method invocation fails
   */
  public void initBinder(WebDataBinder dataBinder, RequestContext request) throws Throwable {

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
   * @since 3.0
   */
  public boolean hasModelAndView() {
    return modelAndView != null;
  }

  /**
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

  @Nullable
  public RedirectModel getRedirectModel() {
    return redirectModel;
  }

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
   * Return diagnostic information.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("BindingContext: model: ");
    sb.append(getModel());

    RedirectModel redirectModel = getRedirectModel();
    if (redirectModel != null) {
      sb.append("; redirect model ");
      sb.append(redirectModel);
    }
    return sb.toString();
  }

}
