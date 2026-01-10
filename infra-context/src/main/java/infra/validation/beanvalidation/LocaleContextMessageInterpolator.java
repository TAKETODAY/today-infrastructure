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

package infra.validation.beanvalidation;

import java.util.Locale;

import infra.core.i18n.LocaleContextHolder;
import infra.lang.Assert;
import jakarta.validation.MessageInterpolator;

/**
 * Delegates to a target {@link MessageInterpolator} implementation but enforces Framework's
 * managed Locale. Typically used to wrap the validation provider's default interpolator.
 *
 * @author Juergen Hoeller
 * @see LocaleContextHolder#getLocale()
 * @since 4.0
 */
public class LocaleContextMessageInterpolator implements MessageInterpolator {

  private final MessageInterpolator targetInterpolator;

  /**
   * Create a new LocaleContextMessageInterpolator, wrapping the given target interpolator.
   *
   * @param targetInterpolator the target MessageInterpolator to wrap
   */
  public LocaleContextMessageInterpolator(MessageInterpolator targetInterpolator) {
    Assert.notNull(targetInterpolator, "Target MessageInterpolator is required");
    this.targetInterpolator = targetInterpolator;
  }

  @Override
  public String interpolate(String message, Context context) {
    return this.targetInterpolator.interpolate(message, context, LocaleContextHolder.getLocale());
  }

  @Override
  public String interpolate(String message, Context context, Locale locale) {
    return this.targetInterpolator.interpolate(message, context, locale);
  }

}
