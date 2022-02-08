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

package cn.taketoday.context.support;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Simple implementation of {@link cn.taketoday.context.MessageSource}
 * which allows messages to be registered programmatically.
 * This MessageSource supports basic internationalization.
 *
 * <p>Intended for testing rather than for use in production systems.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class StaticMessageSource extends AbstractMessageSource {

  private final Map<String, Map<Locale, MessageHolder>> messageMap = new HashMap<>();

  @Override
  @Nullable
  protected String resolveCodeWithoutArguments(String code, Locale locale) {
    Map<Locale, MessageHolder> localeMap = this.messageMap.get(code);
    if (localeMap == null) {
      return null;
    }
    MessageHolder holder = localeMap.get(locale);
    if (holder == null) {
      return null;
    }
    return holder.getMessage();
  }

  @Override
  @Nullable
  protected MessageFormat resolveCode(String code, Locale locale) {
    Map<Locale, MessageHolder> localeMap = this.messageMap.get(code);
    if (localeMap == null) {
      return null;
    }
    MessageHolder holder = localeMap.get(locale);
    if (holder == null) {
      return null;
    }
    return holder.getMessageFormat();
  }

  /**
   * Associate the given message with the given code.
   *
   * @param code the lookup code
   * @param locale the locale that the message should be found within
   * @param msg the message associated with this lookup code
   */
  public void addMessage(String code, Locale locale, String msg) {
    Assert.notNull(code, "Code must not be null");
    Assert.notNull(locale, "Locale must not be null");
    Assert.notNull(msg, "Message must not be null");
    this.messageMap.computeIfAbsent(code, key -> new HashMap<>(4)).put(locale, new MessageHolder(msg, locale));
    if (logger.isDebugEnabled()) {
      logger.debug("Added message [{}] for code [{}] and Locale [{}]", msg, code, locale);
    }
  }

  /**
   * Associate the given message values with the given keys as codes.
   *
   * @param messages the messages to register, with messages codes
   * as keys and message texts as values
   * @param locale the locale that the messages should be found within
   */
  public void addMessages(Map<String, String> messages, Locale locale) {
    Assert.notNull(messages, "Messages Map must not be null");
    for (Map.Entry<String, String> entry : messages.entrySet()) {
      String code = entry.getKey();
      String msg = entry.getValue();
      addMessage(code, locale, msg);
    }
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + this.messageMap;
  }

  private class MessageHolder {

    private final String message;

    private final Locale locale;

    @Nullable
    private volatile MessageFormat cachedFormat;

    public MessageHolder(String message, Locale locale) {
      this.message = message;
      this.locale = locale;
    }

    public String getMessage() {
      return this.message;
    }

    public MessageFormat getMessageFormat() {
      MessageFormat messageFormat = this.cachedFormat;
      if (messageFormat == null) {
        messageFormat = createMessageFormat(this.message, this.locale);
        this.cachedFormat = messageFormat;
      }
      return messageFormat;
    }

    @Override
    public String toString() {
      return this.message;
    }
  }

}
