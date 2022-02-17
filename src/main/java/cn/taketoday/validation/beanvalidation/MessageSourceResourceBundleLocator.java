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

package cn.taketoday.validation.beanvalidation;

import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;

import java.util.Locale;
import java.util.ResourceBundle;

import cn.taketoday.context.MessageSource;
import cn.taketoday.context.support.MessageSourceResourceBundle;
import cn.taketoday.lang.Assert;

/**
 * Implementation of Hibernate Validator 4.3/5.x's {@link ResourceBundleLocator} interface,
 * exposing a Spring {@link MessageSource} as localized {@link MessageSourceResourceBundle}.
 *
 * @author Juergen Hoeller
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
   * @param messageSource the Spring MessageSource to wrap
   */
  public MessageSourceResourceBundleLocator(MessageSource messageSource) {
    Assert.notNull(messageSource, "MessageSource must not be null");
    this.messageSource = messageSource;
  }

  @Override
  public ResourceBundle getResourceBundle(Locale locale) {
    return new MessageSourceResourceBundle(this.messageSource, locale);
  }

}
