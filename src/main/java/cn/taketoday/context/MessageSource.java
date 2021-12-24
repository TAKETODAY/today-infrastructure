/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context;

import java.util.Locale;

import cn.taketoday.lang.Nullable;

/**
 * Strategy interface for resolving messages, with support for the parameterization
 * and internationalization of such messages.
 *
 * <p>Framework provides two out-of-the-box implementations for production:
 * <ul>
 * <li>{@link cn.taketoday.context.support.ResourceBundleMessageSource}: built
 * on top of the standard {@link java.util.ResourceBundle}, sharing its limitations.
 * <li>{@link cn.taketoday.context.support.ReloadableResourceBundleMessageSource}:
 * highly configurable, in particular with respect to reloading message definitions.
 * </ul>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see cn.taketoday.context.support.ResourceBundleMessageSource
 * @see cn.taketoday.context.support.ReloadableResourceBundleMessageSource
 */
public interface MessageSource {

  /**
   * Try to resolve the message. Return default message if no message was found.
   *
   * @param code the message code to look up, e.g. 'calculator.noRateSet'.
   * MessageSource users are encouraged to base message names on qualified class
   * or package names, avoiding potential conflicts and ensuring maximum clarity.
   * @param args an array of arguments that will be filled in for params within
   * the message (params look like "{0}", "{1,date}", "{2,time}" within a message),
   * or {@code null} if none
   * @param defaultMessage a default message to return if the lookup fails
   * @param locale the locale in which to do the lookup
   * @return the resolved message if the lookup was successful, otherwise
   * the default message passed as a parameter (which may be {@code null})
   * @see #getMessage(MessageSourceResolvable, Locale)
   * @see java.text.MessageFormat
   */
  @Nullable
  String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale);

  /**
   * Try to resolve the message. Treat as an error if the message can't be found.
   *
   * @param code the message code to look up, e.g. 'calculator.noRateSet'.
   * MessageSource users are encouraged to base message names on qualified class
   * or package names, avoiding potential conflicts and ensuring maximum clarity.
   * @param args an array of arguments that will be filled in for params within
   * the message (params look like "{0}", "{1,date}", "{2,time}" within a message),
   * or {@code null} if none
   * @param locale the locale in which to do the lookup
   * @return the resolved message (never {@code null})
   * @throws NoSuchMessageException if no corresponding message was found
   * @see #getMessage(MessageSourceResolvable, Locale)
   * @see java.text.MessageFormat
   */
  String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException;

  /**
   * Try to resolve the message using all the attributes contained within the
   * {@code MessageSourceResolvable} argument that was passed in.
   * <p>NOTE: We must throw a {@code NoSuchMessageException} on this method
   * since at the time of calling this method we aren't able to determine if the
   * {@code defaultMessage} property of the resolvable is {@code null} or not.
   *
   * @param resolvable the value object storing attributes required to resolve a message
   * (may include a default message)
   * @param locale the locale in which to do the lookup
   * @return the resolved message (never {@code null} since even a
   * {@code MessageSourceResolvable}-provided default message needs to be non-null)
   * @throws NoSuchMessageException if no corresponding message was found
   * (and no default message was provided by the {@code MessageSourceResolvable})
   * @see MessageSourceResolvable#getCodes()
   * @see MessageSourceResolvable#getArguments()
   * @see MessageSourceResolvable#getDefaultMessage()
   * @see java.text.MessageFormat
   */
  String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException;

}
