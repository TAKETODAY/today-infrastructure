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

package cn.taketoday.context.support;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import cn.taketoday.context.MessageSource;
import cn.taketoday.context.NoSuchMessageException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Helper class that allows for accessing a Framework
 * {@link cn.taketoday.context.MessageSource} as a {@link java.util.ResourceBundle}.
 * Used for example to expose a Framework MessageSource to JSTL web views.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.context.MessageSource
 * @see java.util.ResourceBundle
 * @since 4.0
 */
public class MessageSourceResourceBundle extends ResourceBundle {

  private final MessageSource messageSource;

  private final Locale locale;

  /**
   * Create a new MessageSourceResourceBundle for the given MessageSource and Locale.
   *
   * @param source the MessageSource to retrieve messages from
   * @param locale the Locale to retrieve messages for
   */
  public MessageSourceResourceBundle(MessageSource source, Locale locale) {
    Assert.notNull(source, "MessageSource is required");
    this.messageSource = source;
    this.locale = locale;
  }

  /**
   * Create a new MessageSourceResourceBundle for the given MessageSource and Locale.
   *
   * @param source the MessageSource to retrieve messages from
   * @param locale the Locale to retrieve messages for
   * @param parent the parent ResourceBundle to delegate to if no local message found
   */
  public MessageSourceResourceBundle(MessageSource source, Locale locale, ResourceBundle parent) {
    this(source, locale);
    setParent(parent);
  }

  /**
   * This implementation resolves the code in the MessageSource.
   * Returns {@code null} if the message could not be resolved.
   */
  @Override
  @Nullable
  protected Object handleGetObject(String key) {
    try {
      return this.messageSource.getMessage(key, null, this.locale);
    }
    catch (NoSuchMessageException ex) {
      return null;
    }
  }

  /**
   * This implementation checks whether the target MessageSource can resolve
   * a message for the given key, translating {@code NoSuchMessageException}
   * accordingly. In contrast to ResourceBundle's default implementation in
   * JDK 1.6, this does not rely on the capability to enumerate message keys.
   */
  @Override
  public boolean containsKey(String key) {
    try {
      this.messageSource.getMessage(key, null, this.locale);
      return true;
    }
    catch (NoSuchMessageException ex) {
      return false;
    }
  }

  /**
   * This implementation throws {@code UnsupportedOperationException},
   * as a MessageSource does not allow for enumerating the defined message codes.
   */
  @Override
  public Enumeration<String> getKeys() {
    throw new UnsupportedOperationException("MessageSourceResourceBundle does not support enumerating its keys");
  }

  /**
   * This implementation exposes the specified Locale for introspection
   * through the standard {@code ResourceBundle.getLocale()} method.
   */
  @Override
  public Locale getLocale() {
    return this.locale;
  }

}
