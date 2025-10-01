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

package infra.format.support;

import org.jspecify.annotations.Nullable;

import infra.core.StringValueResolver;
import infra.core.conversion.support.DefaultConversionService;
import infra.format.FormatterRegistry;
import infra.format.datetime.DateFormatterRegistrar;
import infra.format.datetime.standard.DateTimeFormatterRegistrar;
import infra.format.number.NumberFormatAnnotationFormatterFactory;
import infra.format.number.money.CurrencyUnitFormatter;
import infra.format.number.money.Jsr354NumberFormatAnnotationFormatterFactory;
import infra.format.number.money.MonetaryAmountFormatter;
import infra.util.ClassUtils;

/**
 * A specialization of {@link FormattingConversionService} configured by default with
 * converters and formatters appropriate for most applications.
 *
 * <p>Designed for direct instantiation but also exposes the static {@link #addDefaultFormatters}
 * utility method for ad hoc use against any {@code FormatterRegistry} instance, just
 * as {@code DefaultConversionService} exposes its own
 * {@link DefaultConversionService#addDefaultConverters addDefaultConverters} method.
 *
 * <p>Automatically registers formatters for JSR-354 Money &amp; Currency and JSR-310 Date-Time
 * depending on the presence of the corresponding API on the classpath.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultFormattingConversionService extends FormattingConversionService {
  protected static final boolean jsr354Present = ClassUtils.isPresent(
          "javax.money.MonetaryAmount", DefaultFormattingConversionService.class.getClassLoader());

  /**
   * Create a new {@code DefaultFormattingConversionService} with the set of
   * {@linkplain DefaultConversionService#addDefaultConverters default converters} and
   * {@linkplain #addDefaultFormatters default formatters}.
   */
  public DefaultFormattingConversionService() {
    this(null, true);
  }

  /**
   * Create a new {@code DefaultFormattingConversionService} with the set of
   * {@linkplain DefaultConversionService#addDefaultConverters default converters} and,
   * based on the value of {@code registerDefaultFormatters}, the set of
   * {@linkplain #addDefaultFormatters default formatters}.
   *
   * @param registerDefaultFormatters whether to register default formatters
   */
  public DefaultFormattingConversionService(boolean registerDefaultFormatters) {
    this(null, registerDefaultFormatters);
  }

  /**
   * Create a new {@code DefaultFormattingConversionService} with the set of
   * {@linkplain DefaultConversionService#addDefaultConverters default converters} and,
   * based on the value of {@code registerDefaultFormatters}, the set of
   * {@linkplain #addDefaultFormatters default formatters}.
   *
   * @param embeddedValueResolver delegated to {@link #setEmbeddedValueResolver(StringValueResolver)}
   * prior to calling {@link #addDefaultFormatters}.
   * @param registerDefaultFormatters whether to register default formatters
   */
  public DefaultFormattingConversionService(@Nullable StringValueResolver embeddedValueResolver, boolean registerDefaultFormatters) {
    if (embeddedValueResolver != null) {
      setEmbeddedValueResolver(embeddedValueResolver);
    }
    DefaultConversionService.addDefaultConverters(this);
    if (registerDefaultFormatters) {
      addDefaultFormatters(this);
    }
  }

  /**
   * Add formatters appropriate for most environments: including number formatters,
   * JSR-354 Money &amp; Currency formatters, JSR-310 Date-Time  formatters,
   * depending on the presence of the corresponding API on the classpath.
   *
   * @param formatterRegistry the service to register default formatters with
   */
  public static void addDefaultFormatters(FormatterRegistry formatterRegistry) {
    // Default handling of number values
    formatterRegistry.addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());

    // Default handling of monetary values
    if (jsr354Present) {
      formatterRegistry.addFormatter(new CurrencyUnitFormatter());
      formatterRegistry.addFormatter(new MonetaryAmountFormatter());
      formatterRegistry.addFormatterForFieldAnnotation(new Jsr354NumberFormatAnnotationFormatterFactory());
    }

    // Default handling of date-time values

    // just handling JSR-310 specific date and time types
    new DateTimeFormatterRegistrar().registerFormatters(formatterRegistry);

    // regular DateFormat-based Date, Calendar, Long converters
    new DateFormatterRegistrar().registerFormatters(formatterRegistry);
  }

}
