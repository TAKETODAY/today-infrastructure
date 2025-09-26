/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.bind.support;

import org.jspecify.annotations.Nullable;

import infra.beans.PropertyEditorRegistrar;
import infra.core.conversion.ConversionService;
import infra.validation.BindingErrorProcessor;
import infra.validation.DataBinder;
import infra.validation.MessageCodesResolver;
import infra.validation.Validator;
import infra.web.bind.WebDataBinder;

/**
 * Convenient {@link WebBindingInitializer} for declarative configuration
 * in a Infra application context. Allows for reusing pre-configured
 * initializers with multiple controller/handlers.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setDirectFieldAccess
 * @see #setMessageCodesResolver
 * @see #setBindingErrorProcessor
 * @see #setValidator(Validator)
 * @see #setConversionService(ConversionService)
 * @see #setPropertyEditorRegistrar
 * @since 4.0 2022/4/8 22:52
 */
public class ConfigurableWebBindingInitializer implements WebBindingInitializer {

  private boolean autoGrowNestedPaths = true;

  private boolean directFieldAccess = false;

  @Nullable
  private Boolean declarativeBinding;

  @Nullable
  private MessageCodesResolver messageCodesResolver;

  @Nullable
  private BindingErrorProcessor bindingErrorProcessor;

  @Nullable
  private Validator validator;

  @Nullable
  private ConversionService conversionService;

  @Nullable
  private PropertyEditorRegistrar[] propertyEditorRegistrars;

  /**
   * Set whether a binder should attempt to "auto-grow" a nested path that contains a null value.
   * <p>If "true", a null path location will be populated with a default object value and traversed
   * instead of resulting in an exception. This flag also enables auto-growth of collection elements
   * when accessing an out-of-bounds index.
   * <p>Default is "true" on a standard DataBinder. Note that this feature is only supported
   * for bean property access (DataBinder's default mode), not for field access.
   *
   * @see DataBinder#initBeanPropertyAccess()
   * @see DataBinder#setAutoGrowNestedPaths
   */
  public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
    this.autoGrowNestedPaths = autoGrowNestedPaths;
  }

  /**
   * Return whether a binder should attempt to "auto-grow" a nested path that contains a null value.
   */
  public boolean isAutoGrowNestedPaths() {
    return this.autoGrowNestedPaths;
  }

  /**
   * Set whether to use direct field access instead of bean property access.
   * <p>Default is {@code false}, using bean property access.
   * Switch this to {@code true} in order to enforce direct field access.
   *
   * @see DataBinder#initDirectFieldAccess()
   * @see DataBinder#initBeanPropertyAccess()
   */
  public final void setDirectFieldAccess(boolean directFieldAccess) {
    this.directFieldAccess = directFieldAccess;
  }

  /**
   * Return whether to use direct field access instead of bean property access.
   */
  public boolean isDirectFieldAccess() {
    return this.directFieldAccess;
  }

  /**
   * Set whether to bind only fields intended for binding as described in
   * {@link DataBinder#setDeclarativeBinding}.
   */
  public void setDeclarativeBinding(boolean declarativeBinding) {
    this.declarativeBinding = declarativeBinding;
  }

  /**
   * Return whether to bind only fields intended for binding.
   */
  public boolean isDeclarativeBinding() {
    return (this.declarativeBinding != null ? this.declarativeBinding : false);
  }

  /**
   * Set the strategy to use for resolving errors into message codes.
   * Applies the given strategy to all data binders used by this controller.
   * <p>Default is {@code null}, i.e. using the default strategy of
   * the data binder.
   *
   * @see DataBinder#setMessageCodesResolver
   */
  public final void setMessageCodesResolver(@Nullable MessageCodesResolver messageCodesResolver) {
    this.messageCodesResolver = messageCodesResolver;
  }

  /**
   * Return the strategy to use for resolving errors into message codes.
   */
  @Nullable
  public final MessageCodesResolver getMessageCodesResolver() {
    return this.messageCodesResolver;
  }

  /**
   * Set the strategy to use for processing binding errors, that is,
   * required field errors and {@code PropertyAccessException}s.
   * <p>Default is {@code null}, that is, using the default strategy
   * of the data binder.
   *
   * @see DataBinder#setBindingErrorProcessor
   */
  public final void setBindingErrorProcessor(@Nullable BindingErrorProcessor bindingErrorProcessor) {
    this.bindingErrorProcessor = bindingErrorProcessor;
  }

  /**
   * Return the strategy to use for processing binding errors.
   */
  @Nullable
  public final BindingErrorProcessor getBindingErrorProcessor() {
    return this.bindingErrorProcessor;
  }

  /**
   * Set the Validator to apply after each binding step.
   */
  public final void setValidator(@Nullable Validator validator) {
    this.validator = validator;
  }

  /**
   * Return the Validator to apply after each binding step, if any.
   */
  @Nullable
  public final Validator getValidator() {
    return this.validator;
  }

  /**
   * Specify a ConversionService which will apply to every DataBinder.
   */
  public final void setConversionService(@Nullable ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  /**
   * Return the ConversionService which will apply to every DataBinder.
   */
  @Nullable
  public final ConversionService getConversionService() {
    return this.conversionService;
  }

  /**
   * Specify a single PropertyEditorRegistrar to be applied to every DataBinder.
   */
  public final void setPropertyEditorRegistrar(PropertyEditorRegistrar propertyEditorRegistrar) {
    this.propertyEditorRegistrars = new PropertyEditorRegistrar[] { propertyEditorRegistrar };
  }

  /**
   * Specify multiple PropertyEditorRegistrars to be applied to every DataBinder.
   */
  public final void setPropertyEditorRegistrars(@Nullable PropertyEditorRegistrar[] propertyEditorRegistrars) {
    this.propertyEditorRegistrars = propertyEditorRegistrars;
  }

  /**
   * Return the PropertyEditorRegistrars to be applied to every DataBinder.
   */
  @Nullable
  public final PropertyEditorRegistrar[] getPropertyEditorRegistrars() {
    return this.propertyEditorRegistrars;
  }

  @Override
  public void initBinder(WebDataBinder binder) {
    binder.setAutoGrowNestedPaths(autoGrowNestedPaths);
    if (directFieldAccess) {
      binder.initDirectFieldAccess();
    }
    if (declarativeBinding != null) {
      binder.setDeclarativeBinding(declarativeBinding);
    }
    if (messageCodesResolver != null) {
      binder.setMessageCodesResolver(messageCodesResolver);
    }
    if (bindingErrorProcessor != null) {
      binder.setBindingErrorProcessor(bindingErrorProcessor);
    }
    if (validator != null) {
      Class<?> type = getTargetType(binder);
      if (type != null && validator.supports(type)) {
        binder.setValidator(validator);
      }
    }
    if (conversionService != null) {
      binder.setConversionService(conversionService);
    }
    if (propertyEditorRegistrars != null) {
      for (PropertyEditorRegistrar propertyEditorRegistrar : propertyEditorRegistrars) {
        propertyEditorRegistrar.registerCustomEditors(binder);
      }
    }
  }

  @Nullable
  private static Class<?> getTargetType(WebDataBinder binder) {
    Class<?> type = null;
    if (binder.getTarget() != null) {
      type = binder.getTarget().getClass();
    }
    else if (binder.getTargetType() != null) {
      type = binder.getTargetType().resolve();
    }
    return type;
  }

}
