/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.view;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.context.support.MessageSourceAccessor;
import infra.util.Assert;
import infra.validation.BindException;
import infra.validation.BindingResult;
import infra.validation.Errors;
import infra.web.BindingContext;
import infra.web.HttpContext;

/**
 * Rendering-specific access to binding values and localized errors.
 * Values and messages are returned without output escaping.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class BindingAccessor {

  private final HttpContext context;

  private final Map<String, ?> model;

  private final MessageSourceAccessor messages;

  /**
   * Create an accessor for one rendering operation.
   *
   * @param context the context exposed to the view
   * @param model the final rendering model, taking precedence over request data
   * @param messages the message accessor with the effective rendering locale
   */
  public BindingAccessor(HttpContext context, Map<String, ?> model, MessageSourceAccessor messages) {
    Assert.notNull(context, "HttpContext is required");
    Assert.notNull(model, "Model is required");
    Assert.notNull(messages, "MessageSourceAccessor is required");
    this.context = context;
    this.model = model;
    this.messages = messages;
  }

  /**
   * Resolve a field or object status without modifying the binding result.
   *
   * @param path the bean and property path, for example {@code person.name}
   * @return a new status containing unescaped values and messages
   * @throws IllegalStateException if neither errors nor a target object exists
   */
  public BindStatus getBindStatus(String path) {
    Assert.hasText(path, "Binding path is required");
    return new BindStatus(this, path, this.messages);
  }

  @Nullable Errors getErrors(String name) {
    String key = BindingResult.MODEL_KEY_PREFIX + name;
    if (this.model.containsKey(key)) {
      Object value = this.model.get(key);
      if (value instanceof BindException exception) {
        return exception.getBindingResult();
      }
      if (value == null || value instanceof Errors) {
        return (Errors) value;
      }
      throw new IllegalStateException("Model attribute '" + key + "' is not an Errors instance");
    }
    BindingContext binding = BindingContext.find(this.context);
    return binding != null ? binding.getErrors(name) : null;
  }

  @Nullable Object getTarget(String name) {
    if (this.model.containsKey(name)) {
      return this.model.get(name);
    }
    BindingContext binding = BindingContext.find(this.context);
    if (binding != null && binding.containsAttribute(name)) {
      return binding.getModel().get(name);
    }
    return this.context.getAttribute(name);
  }
}
