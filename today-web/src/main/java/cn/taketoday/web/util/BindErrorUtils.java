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

package cn.taketoday.web.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import cn.taketoday.context.MessageSource;
import cn.taketoday.context.MessageSourceResolvable;
import cn.taketoday.context.support.StaticMessageSource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.FieldError;

/**
 * Utility methods to resolve a list of {@link MessageSourceResolvable}s, and
 * optionally join them.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public abstract class BindErrorUtils {

  private static final MessageSource defaultMessageSource = new MethodArgumentErrorMessageSource();

  /**
   * Shortcut for {@link #resolveAndJoin(List, MessageSource, Locale)} with
   * an empty {@link MessageSource} that simply formats the default message,
   * or first error code, also prepending the field name for field errors.
   */
  public static String resolveAndJoin(List<? extends MessageSourceResolvable> errors) {
    return resolveAndJoin(errors, defaultMessageSource, Locale.getDefault());
  }

  /**
   * Shortcut for {@link #resolveAndJoin(CharSequence, CharSequence, CharSequence, List, MessageSource, Locale)}
   * with {@code ", and "} as delimiter, and an empty prefix and suffix.
   */
  public static String resolveAndJoin(List<? extends MessageSourceResolvable> errors, MessageSource messageSource, Locale locale) {
    return resolveAndJoin(", and ", "", "", errors, messageSource, locale);
  }

  /**
   * Resolve all errors through the given {@link MessageSource} and join them.
   *
   * @param delimiter the delimiter to use between each error
   * @param prefix characters to insert at the beginning
   * @param suffix characters to insert at the end
   * @param errors the errors to resolve and join
   * @param messageSource the {@code MessageSource} to resolve with
   * @param locale the locale to resolve with
   * @return the resolved errors formatted as a string
   */
  public static String resolveAndJoin(CharSequence delimiter, CharSequence prefix, CharSequence suffix,
          List<? extends MessageSourceResolvable> errors, MessageSource messageSource, Locale locale) {

    return errors.stream()
            .map(error -> messageSource.getMessage(error, locale))
            .filter(StringUtils::hasText)
            .collect(Collectors.joining(delimiter, prefix, suffix));
  }

  /**
   * Shortcut for {@link #resolve(List, MessageSource, Locale)} with an empty
   * {@link MessageSource} that simply formats the default message, or first
   * error code, also prepending the field name for field errors.
   */
  public static <E extends MessageSourceResolvable> Map<E, String> resolve(List<E> errors) {
    return resolve(errors, defaultMessageSource, Locale.getDefault());
  }

  /**
   * Resolve all errors through the given {@link MessageSource}.
   *
   * @param errors the errors to resolve
   * @param messageSource the {@code MessageSource} to resolve with
   * @param locale the locale to resolve with an empty {@link MessageSource}
   * @return map with resolved errors as values, in the order of the input list
   */
  public static <E extends MessageSourceResolvable> Map<E, String> resolve(
          List<E> errors, MessageSource messageSource, Locale locale) {

    Map<E, String> map = new LinkedHashMap<>(errors.size());
    errors.forEach(error -> map.put(error, messageSource.getMessage(error, locale)));
    return map;
  }

  /**
   * {@code MessageSource} for default error formatting.
   */
  private static class MethodArgumentErrorMessageSource extends StaticMessageSource {

    MethodArgumentErrorMessageSource() {
      setUseCodeAsDefaultMessage(true);
    }

    @Override
    @Nullable
    protected String getDefaultMessage(MessageSourceResolvable resolvable, Locale locale) {
      String message = super.getDefaultMessage(resolvable, locale);
      return (resolvable instanceof FieldError error ? error.getField() + ": " + message : message);
    }
  }

}
