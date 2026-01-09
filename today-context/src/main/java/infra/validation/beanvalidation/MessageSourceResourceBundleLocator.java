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

import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;

import java.util.Locale;
import java.util.ResourceBundle;

import infra.context.MessageSource;
import infra.context.support.MessageSourceResourceBundle;
import infra.lang.Assert;

/**
 * Implementation of Hibernate Validator 4.3/5.x's {@link ResourceBundleLocator} interface,
 * exposing a Framework {@link MessageSource} as localized {@link MessageSourceResourceBundle}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ResourceBundleLocator
 * @see MessageSource
 * @see MessageSourceResourceBundle
 * @since 4.0
 */
public class MessageSourceResourceBundleLocator implements ResourceBundleLocator {

  private final MessageSource messageSource;

  /**
   * Build a MessageSourceResourceBundleLocator for the given MessageSource.
   *
   * @param messageSource the Framework MessageSource to wrap
   */
  public MessageSourceResourceBundleLocator(MessageSource messageSource) {
    Assert.notNull(messageSource, "MessageSource is required");
    this.messageSource = messageSource;
  }

  @Override
  public ResourceBundle getResourceBundle(Locale locale) {
    return new MessageSourceResourceBundle(this.messageSource, locale);
  }

}
