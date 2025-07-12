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

package infra.core.conversion.support;

import java.nio.charset.Charset;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import infra.core.conversion.ConversionService;
import infra.core.conversion.ConverterRegistry;
import infra.lang.Nullable;

/**
 * A specialization of {@link GenericConversionService} configured by default
 * with converters appropriate for most environments.
 *
 * <p>Designed for direct instantiation but also exposes the static
 * {@link #addDefaultConverters(ConverterRegistry)} utility method for ad-hoc
 * use against any {@code ConverterRegistry} instance.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0
 */
public class DefaultConversionService extends GenericConversionService {

  @Nullable
  private static volatile DefaultConversionService sharedInstance;

  /**
   * Create a new {@code DefaultConversionService} with the set of
   * {@linkplain DefaultConversionService#addDefaultConverters(ConverterRegistry) default converters}.
   */
  public DefaultConversionService() {
    addDefaultConverters(this);
  }

  /**
   * Return a shared default {@code ConversionService} instance,
   * lazily building it once needed.
   * <p><b>NOTE:</b> We highly recommend constructing individual
   * {@code ConversionService} instances for customization purposes.
   * This accessor is only meant as a fallback for code paths which
   * need simple type coercion but cannot access a longer-lived
   * {@code ConversionService} instance any other way.
   *
   * @return the shared {@code ConversionService} instance (never {@code null})
   */
  public static DefaultConversionService getSharedInstance() {
    DefaultConversionService cs = sharedInstance;
    if (cs == null) {
      synchronized(DefaultConversionService.class) {
        cs = sharedInstance;
        if (cs == null) {
          cs = new DefaultConversionService();
          sharedInstance = cs;
        }
      }
    }
    return cs;
  }

  /**
   * Add converters appropriate for most environments.
   *
   * @param registry the registry of converters to add to
   * (must also be castable to ConversionService, e.g. being a {@link ConfigurableConversionService})
   * @throws ClassCastException if the given ConverterRegistry could not be cast to a ConversionService
   */
  public static void addDefaultConverters(ConverterRegistry registry) {
    addScalarConverters(registry);
    addCollectionConverters(registry);

    registry.addConverter(new ByteBufferConverter((ConversionService) registry));
    registry.addConverter(new DateToInstantConverter());
    registry.addConverter(new InstantToDateConverter());
    registry.addConverter(new StringToTimeZoneConverter());
    registry.addConverter(new ZoneIdToTimeZoneConverter());
    registry.addConverter(new ZonedDateTimeToCalendarConverter());

    registry.addConverter(new ObjectToObjectConverter());
    registry.addConverter(new IdToEntityConverter((ConversionService) registry));
    registry.addConverter(new FallbackObjectToStringConverter());
    registry.addConverter(new ObjectToOptionalConverter((ConversionService) registry));
    registry.addConverter(new OptionalToObjectConverter((ConversionService) registry));
  }

  /**
   * Add common collection converters.
   *
   * @param registry the registry of converters to add to
   * (must also be castable to ConversionService, e.g. being a {@link ConfigurableConversionService})
   * @throws ClassCastException if the given ConverterRegistry could not be cast to a ConversionService
   */
  public static void addCollectionConverters(ConverterRegistry registry) {
    ConversionService conversionService = (ConversionService) registry;

    registry.addConverter(new ArrayToCollectionConverter(conversionService));
    registry.addConverter(new CollectionToArrayConverter(conversionService));

    registry.addConverter(new ArrayToArrayConverter(conversionService));
    registry.addConverter(new CollectionToCollectionConverter(conversionService));
    registry.addConverter(new MapToMapConverter(conversionService));

    registry.addConverter(new ArrayToStringConverter(conversionService));
    registry.addConverter(new StringToArrayConverter(conversionService));

    registry.addConverter(new ArrayToObjectConverter(conversionService));
    registry.addConverter(new ObjectToArrayConverter(conversionService));

    registry.addConverter(new CollectionToStringConverter(conversionService));
    registry.addConverter(new StringToCollectionConverter(conversionService));

    registry.addConverter(new CollectionToObjectConverter(conversionService));
    registry.addConverter(new ObjectToCollectionConverter(conversionService));

    registry.addConverter(new StreamConverter(conversionService));
  }

  private static void addScalarConverters(ConverterRegistry registry) {
    registry.addConverterFactory(new NumberToNumberConverterFactory());

    registry.addConverterFactory(new StringToNumberConverterFactory());
    registry.addConverter(Number.class, String.class, new ObjectToStringConverter());

    registry.addConverter(new StringToCharacterConverter());
    registry.addConverter(Character.class, String.class, new ObjectToStringConverter());

    registry.addConverter(new NumberToCharacterConverter());
    registry.addConverterFactory(new CharacterToNumberFactory());

    registry.addConverter(new StringToBooleanConverter());
    registry.addConverter(Boolean.class, String.class, new ObjectToStringConverter());

    registry.addConverterFactory(new StringToEnumConverterFactory());
    registry.addConverter(new EnumToStringConverter((ConversionService) registry));

    registry.addConverterFactory(new IntegerToEnumConverterFactory());
    registry.addConverter(new EnumToIntegerConverter((ConversionService) registry));

    registry.addConverter(new StringToLocaleConverter());
    registry.addConverter(Locale.class, String.class, new ObjectToStringConverter());

    registry.addConverter(new StringToCharsetConverter());
    registry.addConverter(Charset.class, String.class, new ObjectToStringConverter());

    registry.addConverter(new StringToCurrencyConverter());
    registry.addConverter(Currency.class, String.class, new ObjectToStringConverter());

    registry.addConverter(new StringToPropertiesConverter());
    registry.addConverter(new PropertiesToStringConverter());

    registry.addConverter(new StringToUUIDConverter());
    registry.addConverter(UUID.class, String.class, new ObjectToStringConverter());

    registry.addConverter(new StringToPatternConverter());
    registry.addConverter(Pattern.class, String.class, new ObjectToStringConverter());
  }

}
