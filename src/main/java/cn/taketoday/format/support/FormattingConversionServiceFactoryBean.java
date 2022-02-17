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

package cn.taketoday.format.support;

import java.util.Set;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.expression.EmbeddedValueResolverAware;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.core.conversion.support.ConversionServiceFactory;
import cn.taketoday.format.AnnotationFormatterFactory;
import cn.taketoday.format.Formatter;
import cn.taketoday.format.FormatterRegistrar;
import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.format.Parser;
import cn.taketoday.format.Printer;
import cn.taketoday.lang.Nullable;

/**
 * A factory providing convenient access to a {@code FormattingConversionService}
 * configured with converters and formatters for common types such as numbers and
 * datetimes.
 *
 * <p>Additional converters and formatters can be registered declaratively through
 * {@link #setConverters(Set)} and {@link #setFormatters(Set)}. Another option
 * is to register converters and formatters in code by implementing the
 * {@link FormatterRegistrar} interface. You can then configure provide the set
 * of registrars to use through {@link #setFormatterRegistrars(Set)}.
 *
 * <p>A good example for registering converters and formatters in code is
 * {@code JodaTimeFormatterRegistrar}, which registers a number of
 * date-related formatters and converters. For a more detailed list of cases
 * see {@link #setFormatterRegistrars(Set)}
 *
 * <p>Like all {@code FactoryBean} implementations, this class is suitable for
 * use when configuring a Framework application context using Framework {@code <beans>}
 * XML. When configuring the container with
 * {@link cn.taketoday.context.annotation.Configuration @Configuration}
 * classes, simply instantiate, configure and return the appropriate
 * {@code FormattingConversionService} object from a
 * {@link cn.taketoday.context.annotation.Bean @Bean} method.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Chris Beams
 * @since 4.0
 */
public class FormattingConversionServiceFactoryBean
        implements FactoryBean<FormattingConversionService>, EmbeddedValueResolverAware, InitializingBean {

  @Nullable
  private Set<?> converters;

  @Nullable
  private Set<?> formatters;

  @Nullable
  private Set<FormatterRegistrar> formatterRegistrars;

  private boolean registerDefaultFormatters = true;

  @Nullable
  private StringValueResolver embeddedValueResolver;

  @Nullable
  private FormattingConversionService conversionService;

  /**
   * Configure the set of custom converter objects that should be added.
   *
   * @param converters instances of any of the following:
   * {@link cn.taketoday.core.conversion.Converter},
   * {@link cn.taketoday.core.conversion.ConverterFactory},
   * {@link cn.taketoday.core.conversion.GenericConverter}
   */
  public void setConverters(Set<?> converters) {
    this.converters = converters;
  }

  /**
   * Configure the set of custom formatter objects that should be added.
   *
   * @param formatters instances of {@link Formatter} or {@link AnnotationFormatterFactory}
   */
  public void setFormatters(Set<?> formatters) {
    this.formatters = formatters;
  }

  /**
   * <p>Configure the set of FormatterRegistrars to invoke to register
   * Converters and Formatters in addition to those added declaratively
   * via {@link #setConverters(Set)} and {@link #setFormatters(Set)}.
   * <p>FormatterRegistrars are useful when registering multiple related
   * converters and formatters for a formatting category, such as Date
   * formatting. All types related needed to support the formatting
   * category can be registered from one place.
   * <p>FormatterRegistrars can also be used to register Formatters
   * indexed under a specific field type different from its own &lt;T&gt;,
   * or when registering a Formatter from a Printer/Parser pair.
   *
   * @see FormatterRegistry#addFormatterForFieldType(Class, Formatter)
   * @see FormatterRegistry#addFormatterForFieldType(Class, Printer, Parser)
   */
  public void setFormatterRegistrars(Set<FormatterRegistrar> formatterRegistrars) {
    this.formatterRegistrars = formatterRegistrars;
  }

  /**
   * Indicate whether default formatters should be registered or not.
   * <p>By default, built-in formatters are registered. This flag can be used
   * to turn that off and rely on explicitly registered formatters only.
   *
   * @see #setFormatters(Set)
   * @see #setFormatterRegistrars(Set)
   */
  public void setRegisterDefaultFormatters(boolean registerDefaultFormatters) {
    this.registerDefaultFormatters = registerDefaultFormatters;
  }

  @Override
  public void setEmbeddedValueResolver(StringValueResolver embeddedValueResolver) {
    this.embeddedValueResolver = embeddedValueResolver;
  }

  @Override
  public void afterPropertiesSet() {
    this.conversionService = new DefaultFormattingConversionService(this.embeddedValueResolver, this.registerDefaultFormatters);
    ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
    registerFormatters(this.conversionService);
  }

  private void registerFormatters(FormattingConversionService conversionService) {
    if (this.formatters != null) {
      for (Object formatter : this.formatters) {
        if (formatter instanceof Formatter<?>) {
          conversionService.addFormatter((Formatter<?>) formatter);
        }
        else if (formatter instanceof AnnotationFormatterFactory<?>) {
          conversionService.addFormatterForFieldAnnotation((AnnotationFormatterFactory<?>) formatter);
        }
        else {
          throw new IllegalArgumentException(
                  "Custom formatters must be implementations of Formatter or AnnotationFormatterFactory");
        }
      }
    }
    if (this.formatterRegistrars != null) {
      for (FormatterRegistrar registrar : this.formatterRegistrars) {
        registrar.registerFormatters(conversionService);
      }
    }
  }

  @Override
  @Nullable
  public FormattingConversionService getObject() {
    return this.conversionService;
  }

  @Override
  public Class<? extends FormattingConversionService> getObjectType() {
    return FormattingConversionService.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
