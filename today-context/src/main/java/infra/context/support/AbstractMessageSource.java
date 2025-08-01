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

package infra.context.support;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import infra.beans.factory.config.PropertiesFactoryBean;
import infra.context.HierarchicalMessageSource;
import infra.context.MessageSource;
import infra.context.MessageSourceResolvable;
import infra.context.NoSuchMessageException;
import infra.lang.Nullable;
import infra.util.ObjectUtils;
import infra.validation.FieldError;

/**
 * Abstract implementation of the {@link HierarchicalMessageSource} interface,
 * implementing common handling of message variants, making it easy
 * to implement a specific strategy for a concrete MessageSource.
 *
 * <p>Subclasses must implement the abstract {@link #resolveCode}
 * method. For efficient resolution of messages without arguments, the
 * {@link #resolveCodeWithoutArguments} method should be overridden
 * as well, resolving messages without a MessageFormat being involved.
 *
 * <p><b>Note:</b> By default, message texts are only parsed through
 * MessageFormat if arguments have been passed in for the message. In case
 * of no arguments, message texts will be returned as-is. As a consequence,
 * you should only use MessageFormat escaping for messages with actual
 * arguments, and keep all other messages unescaped. If you prefer to
 * escape all messages, set the "alwaysUseMessageFormat" flag to "true".
 *
 * <p>Supports not only MessageSourceResolvables as primary messages
 * but also resolution of message arguments that are in turn
 * MessageSourceResolvables themselves.
 *
 * <p>This class does not implement caching of messages per code, thus
 * subclasses can dynamically change messages over time. Subclasses are
 * encouraged to cache their messages in a modification-aware fashion,
 * allowing for hot deployment of updated messages.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see #resolveCode(String, java.util.Locale)
 * @see #resolveCodeWithoutArguments(String, java.util.Locale)
 * @see #setAlwaysUseMessageFormat
 * @see java.text.MessageFormat
 */
public abstract class AbstractMessageSource extends MessageSourceSupport implements HierarchicalMessageSource {

  @Nullable
  private MessageSource parentMessageSource;

  @Nullable
  private Properties commonMessages;

  private boolean useCodeAsDefaultMessage = false;

  @Override
  public void setParentMessageSource(@Nullable MessageSource parent) {
    this.parentMessageSource = parent;
  }

  @Override
  @Nullable
  public MessageSource getParentMessageSource() {
    return this.parentMessageSource;
  }

  /**
   * Specify locale-independent common messages, with the message code as key
   * and the full message String (may contain argument placeholders) as value.
   * <p>May also link to an externally defined Properties object, e.g. defined
   * through a {@link PropertiesFactoryBean}.
   */
  public void setCommonMessages(@Nullable Properties commonMessages) {
    this.commonMessages = commonMessages;
  }

  /**
   * Return a Properties object defining locale-independent common messages, if any.
   */
  @Nullable
  protected Properties getCommonMessages() {
    return this.commonMessages;
  }

  /**
   * Set whether to use the message code as default message instead of
   * throwing a NoSuchMessageException. Useful for development and debugging.
   * Default is "false".
   * <p>Note: In case of a MessageSourceResolvable with multiple codes
   * (like a FieldError) and a MessageSource that has a parent MessageSource,
   * do <i>not</i> activate "useCodeAsDefaultMessage" in the <i>parent</i>:
   * Else, you'll get the first code returned as message by the parent,
   * without attempts to check further codes.
   * <p>To be able to work with "useCodeAsDefaultMessage" turned on in the parent,
   * AbstractMessageSource and AbstractApplicationContext contain special checks
   * to delegate to the internal {@link #getMessageInternal} method if available.
   * In general, it is recommended to just use "useCodeAsDefaultMessage" during
   * development and not rely on it in production in the first place, though.
   *
   * @see #getMessage(String, Object[], Locale)
   * @see FieldError
   */
  public void setUseCodeAsDefaultMessage(boolean useCodeAsDefaultMessage) {
    this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
  }

  /**
   * Return whether to use the message code as default message instead of
   * throwing a NoSuchMessageException. Useful for development and debugging.
   * Default is "false".
   * <p>Alternatively, consider overriding the {@link #getDefaultMessage}
   * method to return a custom fallback message for an unresolvable code.
   *
   * @see #getDefaultMessage(String)
   */
  protected boolean isUseCodeAsDefaultMessage() {
    return this.useCodeAsDefaultMessage;
  }

  @Nullable
  @Override
  public final String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, @Nullable Locale locale) {
    String msg = getMessageInternal(code, args, locale);
    if (msg != null) {
      return msg;
    }
    if (defaultMessage == null) {
      return getDefaultMessage(code);
    }
    return renderDefaultMessage(defaultMessage, args, locale);
  }

  @Override
  public final String getMessage(String code, @Nullable Object[] args, @Nullable Locale locale) throws NoSuchMessageException {
    String msg = getMessageInternal(code, args, locale);
    if (msg != null) {
      return msg;
    }
    String fallback = getDefaultMessage(code);
    if (fallback != null) {
      return fallback;
    }
    if (locale == null) {
      throw new NoSuchMessageException(code);
    }
    else {
      throw new NoSuchMessageException(code, locale);
    }
  }

  @Override
  public final String getMessage(MessageSourceResolvable resolvable, @Nullable Locale locale) throws NoSuchMessageException {
    String[] codes = resolvable.getCodes();
    if (codes != null) {
      for (String code : codes) {
        String message = getMessageInternal(code, resolvable.getArguments(), locale);
        if (message != null) {
          return message;
        }
      }
    }
    String defaultMessage = getDefaultMessage(resolvable, locale);
    if (defaultMessage != null) {
      return defaultMessage;
    }
    String code = ObjectUtils.isNotEmpty(codes) ? codes[codes.length - 1] : "";
    if (locale == null) {
      throw new NoSuchMessageException(code);
    }
    else {
      throw new NoSuchMessageException(code, locale);
    }
  }

  /**
   * Resolve the given code and arguments as message in the given Locale,
   * returning {@code null} if not found. Does <i>not</i> fall back to
   * the code as default message. Invoked by {@code getMessage} methods.
   *
   * @param code the code to lookup up, such as 'calculator.noRateSet'
   * @param args array of arguments that will be filled in for params
   * within the message
   * @param locale the locale in which to do the lookup
   * @return the resolved message, or {@code null} if not found
   * @see #getMessage(String, Object[], String, Locale)
   * @see #getMessage(String, Object[], Locale)
   * @see #getMessage(MessageSourceResolvable, Locale)
   * @see #setUseCodeAsDefaultMessage
   */
  @Nullable
  protected String getMessageInternal(@Nullable String code, @Nullable Object[] args, @Nullable Locale locale) {
    if (code == null) {
      return null;
    }
    if (locale == null) {
      locale = Locale.getDefault();
    }
    Object[] argsToUse = args;

    if (!isAlwaysUseMessageFormat() && ObjectUtils.isEmpty(args)) {
      // Optimized resolution: no arguments to apply,
      // therefore no MessageFormat needs to be involved.
      // Note that the default implementation still uses MessageFormat;
      // this can be overridden in specific subclasses.
      String message = resolveCodeWithoutArguments(code, locale);
      if (message != null) {
        return message;
      }
    }

    else {
      // Resolve arguments eagerly, for the case where the message
      // is defined in a parent MessageSource but resolvable arguments
      // are defined in the child MessageSource.
      argsToUse = resolveArguments(args, locale);

      MessageFormat messageFormat = resolveCode(code, locale);
      if (messageFormat != null) {
        synchronized(messageFormat) {
          return messageFormat.format(argsToUse);
        }
      }
    }

    // Check locale-independent common messages for the given message code.
    Properties commonMessages = getCommonMessages();
    if (commonMessages != null) {
      String commonMessage = commonMessages.getProperty(code);
      if (commonMessage != null) {
        return formatMessage(commonMessage, args, locale);
      }
    }

    // Not found -> check parent, if any.
    return getMessageFromParent(code, argsToUse, locale);
  }

  /**
   * Try to retrieve the given message from the parent {@code MessageSource}, if any.
   *
   * @param code the code to lookup up, such as 'calculator.noRateSet'
   * @param args array of arguments that will be filled in for params
   * within the message
   * @param locale the locale in which to do the lookup
   * @return the resolved message, or {@code null} if not found
   * @see #getParentMessageSource()
   */
  @Nullable
  protected String getMessageFromParent(String code, @Nullable Object[] args, Locale locale) {
    MessageSource parent = getParentMessageSource();
    if (parent != null) {
      if (parent instanceof AbstractMessageSource) {
        // Call internal method to avoid getting the default code back
        // in case of "useCodeAsDefaultMessage" being activated.
        return ((AbstractMessageSource) parent).getMessageInternal(code, args, locale);
      }
      else {
        // Check parent MessageSource, returning null if not found there.
        // Covers custom MessageSource impls and DelegatingMessageSource.
        return parent.getMessage(code, args, null, locale);
      }
    }
    // Not found in parent either.
    return null;
  }

  /**
   * Get a default message for the given {@code MessageSourceResolvable}.
   * <p>This implementation fully renders the default message if available,
   * or just returns the plain default message {@code String} if the primary
   * message code is being used as a default message.
   *
   * @param resolvable the value object to resolve a default message for
   * @param locale the current locale
   * @return the default message, or {@code null} if none
   * @see #renderDefaultMessage(String, Object[], Locale)
   * @see #getDefaultMessage(String)
   */
  @Nullable
  protected String getDefaultMessage(MessageSourceResolvable resolvable, @Nullable Locale locale) {
    String defaultMessage = resolvable.getDefaultMessage();
    String[] codes = resolvable.getCodes();
    if (defaultMessage != null) {
      if (resolvable instanceof DefaultMessageSourceResolvable defaultResolvable
              && !defaultResolvable.shouldRenderDefaultMessage()) {
        // Given default message does not contain any argument placeholders
        // (and isn't escaped for alwaysUseMessageFormat either) -> return as-is.
        return defaultMessage;
      }
      if (ObjectUtils.isNotEmpty(codes) && defaultMessage.equals(codes[0])) {
        // Never format a code-as-default-message, even with alwaysUseMessageFormat=true
        return defaultMessage;
      }
      return renderDefaultMessage(defaultMessage, resolvable.getArguments(), locale);
    }
    return ObjectUtils.isNotEmpty(codes) ? getDefaultMessage(codes[0]) : null;
  }

  /**
   * Return a fallback default message for the given code, if any.
   * <p>Default is to return the code itself if "useCodeAsDefaultMessage" is activated,
   * or return no fallback else. In case of no fallback, the caller will usually
   * receive a {@code NoSuchMessageException} from {@code getMessage}.
   *
   * @param code the message code that we couldn't resolve
   * and that we didn't receive an explicit default message for
   * @return the default message to use, or {@code null} if none
   * @see #setUseCodeAsDefaultMessage
   */
  @Nullable
  protected String getDefaultMessage(String code) {
    if (isUseCodeAsDefaultMessage()) {
      return code;
    }
    return null;
  }

  /**
   * Searches through the given array of objects, finds any MessageSourceResolvable
   * objects and resolves them.
   * <p>Allows for messages to have MessageSourceResolvables as arguments.
   *
   * @param args array of arguments for a message
   * @param locale the locale to resolve through
   * @return an array of arguments with any MessageSourceResolvables resolved
   */
  @Override
  protected Object[] resolveArguments(@Nullable Object[] args, @Nullable Locale locale) {
    if (ObjectUtils.isEmpty(args)) {
      return super.resolveArguments(args, locale);
    }
    List<Object> resolvedArgs = new ArrayList<>(args.length);
    for (Object arg : args) {
      if (arg instanceof MessageSourceResolvable) {
        resolvedArgs.add(getMessage((MessageSourceResolvable) arg, locale));
      }
      else {
        resolvedArgs.add(arg);
      }
    }
    return resolvedArgs.toArray();
  }

  /**
   * Subclasses can override this method to resolve a message without arguments
   * in an optimized fashion, i.e. to resolve without involving a MessageFormat.
   * <p>The default implementation <i>does</i> use MessageFormat, through
   * delegating to the {@link #resolveCode} method. Subclasses are encouraged
   * to replace this with optimized resolution.
   * <p>Unfortunately, {@code java.text.MessageFormat} is not implemented
   * in an efficient fashion. In particular, it does not detect that a message
   * pattern doesn't contain argument placeholders in the first place. Therefore,
   * it is advisable to circumvent MessageFormat for messages without arguments.
   *
   * @param code the code of the message to resolve
   * @param locale the locale to resolve the code for
   * (subclasses are encouraged to support internationalization)
   * @return the message String, or {@code null} if not found
   * @see #resolveCode
   * @see java.text.MessageFormat
   */
  @Nullable
  protected String resolveCodeWithoutArguments(String code, Locale locale) {
    MessageFormat messageFormat = resolveCode(code, locale);
    if (messageFormat != null) {
      synchronized(messageFormat) {
        return messageFormat.format(new Object[0]);
      }
    }
    return null;
  }

  /**
   * Subclasses must implement this method to resolve a message.
   * <p>Returns a MessageFormat instance rather than a message String,
   * to allow for appropriate caching of MessageFormats in subclasses.
   * <p><b>Subclasses are encouraged to provide optimized resolution
   * for messages without arguments, not involving MessageFormat.</b>
   * See the {@link #resolveCodeWithoutArguments} javadoc for details.
   *
   * @param code the code of the message to resolve
   * @param locale the locale to resolve the code for
   * (subclasses are encouraged to support internationalization)
   * @return the MessageFormat for the message, or {@code null} if not found
   * @see #resolveCodeWithoutArguments(String, java.util.Locale)
   */
  @Nullable
  protected abstract MessageFormat resolveCode(String code, Locale locale);

}
