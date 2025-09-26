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

package infra.context;

import org.jspecify.annotations.Nullable;

import infra.context.support.AbstractMessageSource;
import infra.validation.FieldError;
import infra.validation.ObjectError;

/**
 * Interface for objects that are suitable for message resolution in a
 * {@link MessageSource}.
 *
 * <p>Framework's own validation error classes implement this interface.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see MessageSource#getMessage(MessageSourceResolvable, java.util.Locale)
 * @see ObjectError
 * @see FieldError
 */
@FunctionalInterface
public interface MessageSourceResolvable {

  /**
   * Return the codes to be used to resolve this message, in the order that
   * they should get tried. The last code will therefore be the default one.
   *
   * @return a String array of codes which are associated with this message
   */
  @Nullable
  String[] getCodes();

  /**
   * Return the array of arguments to be used to resolve this message.
   * <p>The default implementation simply returns {@code null}.
   *
   * @return an array of objects to be used as parameters to replace
   * placeholders within the message text
   * @see java.text.MessageFormat
   */
  @Nullable
  default Object[] getArguments() {
    return null;
  }

  /**
   * Return the default message to be used to resolve this message.
   * <p>The default implementation simply returns {@code null}.
   * Note that the default message may be identical to the primary
   * message code ({@link #getCodes()}), which effectively enforces
   * {@link AbstractMessageSource#setUseCodeAsDefaultMessage}
   * for this particular message.
   *
   * @return the default message, or {@code null} if no default
   */
  @Nullable
  default String getDefaultMessage() {
    return null;
  }

}
