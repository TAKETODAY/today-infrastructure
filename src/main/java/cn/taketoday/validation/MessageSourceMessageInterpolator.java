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

package cn.taketoday.validation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import cn.taketoday.context.MessageSource;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.lang.Nullable;
import jakarta.validation.MessageInterpolator;

/**
 * Resolves any message parameters via {@link MessageSource} and then interpolates a
 * message using the underlying {@link MessageInterpolator}.
 *
 * @author Dmytro Nosan
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Scott Frederick
 * @since 4.0
 */
class MessageSourceMessageInterpolator implements MessageInterpolator {

  private static final char PREFIX = '{';

  private static final char SUFFIX = '}';

  private static final char ESCAPE = '\\';

  private final MessageSource messageSource;

  private final MessageInterpolator messageInterpolator;

  MessageSourceMessageInterpolator(MessageSource messageSource, MessageInterpolator messageInterpolator) {
    this.messageSource = messageSource;
    this.messageInterpolator = messageInterpolator;
  }

  @Override
  public String interpolate(String messageTemplate, Context context) {
    return interpolate(messageTemplate, context, LocaleContextHolder.getLocale());
  }

  @Override
  public String interpolate(String messageTemplate, Context context, Locale locale) {
    String message = replaceParameters(messageTemplate, locale);
    return this.messageInterpolator.interpolate(message, context, locale);
  }

  /**
   * Recursively replaces all message parameters.
   * <p>
   * The message parameter prefix <code>&#123;</code> and suffix <code>&#125;</code> can
   * be escaped using {@code \}, e.g. <code>\&#123;escaped\&#125;</code>.
   *
   * @param message the message containing the parameters to be replaced
   * @param locale the locale to use when resolving replacements
   * @return the message with parameters replaced
   */
  private String replaceParameters(String message, Locale locale) {
    return replaceParameters(message, locale, new LinkedHashSet<>(4));
  }

  private String replaceParameters(String message, Locale locale, Set<String> visitedParameters) {
    StringBuilder buf = new StringBuilder(message);
    int parentheses = 0;
    int startIndex = -1;
    int endIndex = -1;
    for (int i = 0; i < buf.length(); i++) {
      if (buf.charAt(i) == ESCAPE) {
        i++;
      }
      else if (buf.charAt(i) == PREFIX) {
        if (startIndex == -1) {
          startIndex = i;
        }
        parentheses++;
      }
      else if (buf.charAt(i) == SUFFIX) {
        if (parentheses > 0) {
          parentheses--;
        }
        endIndex = i;
      }
      if (parentheses == 0 && startIndex < endIndex) {
        String parameter = buf.substring(startIndex + 1, endIndex);
        if (!visitedParameters.add(parameter)) {
          throw new IllegalArgumentException("Circular reference '{" + String.join(" -> ", visitedParameters)
                  + " -> " + parameter + "}'");
        }
        String value = replaceParameter(parameter, locale, visitedParameters);
        if (value != null) {
          buf.replace(startIndex, endIndex + 1, value);
          i = startIndex + value.length() - 1;
        }
        visitedParameters.remove(parameter);
        startIndex = -1;
        endIndex = -1;
      }
    }
    return buf.toString();
  }

  @Nullable
  private String replaceParameter(String parameter, Locale locale, Set<String> visitedParameters) {
    parameter = replaceParameters(parameter, locale, visitedParameters);
    String value = this.messageSource.getMessage(parameter, null, null, locale);
    return (value != null && !isUsingCodeAsDefaultMessage(value, parameter))
           ? replaceParameters(value, locale, visitedParameters) : null;
  }

  private boolean isUsingCodeAsDefaultMessage(String value, String parameter) {
    return value.equals(parameter);
  }

}
