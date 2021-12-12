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

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;

/**
 * Base class for message source implementations, providing support infrastructure
 * such as {@link java.text.MessageFormat} handling but not implementing concrete
 * methods defined in the {@link cn.taketoday.context.MessageSource}.
 *
 * <p>{@link AbstractMessageSource} derives from this class, providing concrete
 * {@code getMessage} implementations that delegate to a central template
 * method for message code resolution.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class MessageSourceSupport {

  private static final MessageFormat INVALID_MESSAGE_FORMAT = new MessageFormat("");

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private boolean alwaysUseMessageFormat = false;

  /**
   * Cache to hold already generated MessageFormats per message.
   * Used for passed-in default messages. MessageFormats for resolved
   * codes are cached on a specific basis in subclasses.
   */
  private final Map<String, Map<Locale, MessageFormat>> messageFormatsPerMessage = new HashMap<>();

  /**
   * Set whether to always apply the {@code MessageFormat} rules, parsing even
   * messages without arguments.
   * <p>Default is {@code false}: Messages without arguments are by default
   * returned as-is, without parsing them through {@code MessageFormat}.
   * Set this to {@code true} to enforce {@code MessageFormat} for all messages,
   * expecting all message texts to be written with {@code MessageFormat} escaping.
   * <p>For example, {@code MessageFormat} expects a single quote to be escaped
   * as two adjacent single quotes ({@code "''"}). If your message texts are all
   * written with such escaping, even when not defining argument placeholders,
   * you need to set this flag to {@code true}. Otherwise, only message texts
   * with actual arguments are supposed to be written with {@code MessageFormat}
   * escaping.
   *
   * @see java.text.MessageFormat
   */
  public void setAlwaysUseMessageFormat(boolean alwaysUseMessageFormat) {
    this.alwaysUseMessageFormat = alwaysUseMessageFormat;
  }

  /**
   * Return whether to always apply the {@code MessageFormat} rules, parsing even
   * messages without arguments.
   */
  protected boolean isAlwaysUseMessageFormat() {
    return this.alwaysUseMessageFormat;
  }

  /**
   * Render the given default message String. The default message is
   * passed in as specified by the caller and can be rendered into
   * a fully formatted default message shown to the user.
   * <p>The default implementation passes the String to {@code formatMessage},
   * resolving any argument placeholders found in them. Subclasses may override
   * this method to plug in custom processing of default messages.
   *
   * @param defaultMessage the passed-in default message String
   * @param args array of arguments that will be filled in for params within
   * the message, or {@code null} if none.
   * @param locale the Locale used for formatting
   * @return the rendered default message (with resolved arguments)
   * @see #formatMessage(String, Object[], java.util.Locale)
   */
  protected String renderDefaultMessage(String defaultMessage, @Nullable Object[] args, Locale locale) {
    return formatMessage(defaultMessage, args, locale);
  }

  /**
   * Format the given message String, using cached MessageFormats.
   * By default invoked for passed-in default messages, to resolve
   * any argument placeholders found in them.
   *
   * @param msg the message to format
   * @param args array of arguments that will be filled in for params within
   * the message, or {@code null} if none
   * @param locale the Locale used for formatting
   * @return the formatted message (with resolved arguments)
   */
  protected String formatMessage(String msg, @Nullable Object[] args, Locale locale) {
    if (!isAlwaysUseMessageFormat() && ObjectUtils.isEmpty(args)) {
      return msg;
    }
    MessageFormat messageFormat = null;
    synchronized(this.messageFormatsPerMessage) {
      Map<Locale, MessageFormat> messageFormatsPerLocale = this.messageFormatsPerMessage.get(msg);
      if (messageFormatsPerLocale != null) {
        messageFormat = messageFormatsPerLocale.get(locale);
      }
      else {
        messageFormatsPerLocale = new HashMap<>();
        this.messageFormatsPerMessage.put(msg, messageFormatsPerLocale);
      }
      if (messageFormat == null) {
        try {
          messageFormat = createMessageFormat(msg, locale);
        }
        catch (IllegalArgumentException ex) {
          // Invalid message format - probably not intended for formatting,
          // rather using a message structure with no arguments involved...
          if (isAlwaysUseMessageFormat()) {
            throw ex;
          }
          // Silently proceed with raw message if format not enforced...
          messageFormat = INVALID_MESSAGE_FORMAT;
        }
        messageFormatsPerLocale.put(locale, messageFormat);
      }
    }
    if (messageFormat == INVALID_MESSAGE_FORMAT) {
      return msg;
    }
    synchronized(messageFormat) {
      return messageFormat.format(resolveArguments(args, locale));
    }
  }

  /**
   * Create a {@code MessageFormat} for the given message and Locale.
   *
   * @param msg the message to create a {@code MessageFormat} for
   * @param locale the Locale to create a {@code MessageFormat} for
   * @return the {@code MessageFormat} instance
   */
  protected MessageFormat createMessageFormat(String msg, Locale locale) {
    return new MessageFormat(msg, locale);
  }

  /**
   * Template method for resolving argument objects.
   * <p>The default implementation simply returns the given argument array as-is.
   * Can be overridden in subclasses in order to resolve special argument types.
   *
   * @param args the original argument array
   * @param locale the Locale to resolve against
   * @return the resolved argument array
   */
  protected Object[] resolveArguments(@Nullable Object[] args, Locale locale) {
    return (args != null ? args : new Object[0]);
  }

}
