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

import java.util.Set;

import infra.beans.factory.FactoryBean;
import infra.beans.factory.InitializingBean;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.expression.EmbeddedValueResolverAware;
import infra.core.StringValueResolver;
import infra.core.conversion.Converter;
import infra.core.conversion.ConverterFactory;
import infra.core.conversion.ConverterRegistry;
import infra.core.conversion.GenericConverter;
import infra.format.AnnotationFormatterFactory;
import infra.format.Formatter;
import infra.format.FormatterRegistrar;
import infra.format.FormatterRegistry;
import infra.format.Parser;
import infra.format.Printer;

/**
 * A factory providing convenient access to a {@link FormattingConversionService}
 * configured with converters and formatters for common types such as numbers, dates,
 * and times.
 *
 * <p>Additional converters and formatters can be registered declaratively through
 * {@link #setConverters(Set)} and {@link #setFormatters(Set)}. Another option
 * is to register converters and formatters in code by implementing the
 * {@link FormatterRegistrar} interface. You can then provide the set of registrars
 * to use through {@link #setFormatterRegistrars(Set)}.
 *
 * <p>Like all {@code FactoryBean} implementations, this class is suitable for
 * use when configuring a Framework application context using Framework {@code <beans>}
 * XML configuration files. When configuring the container with
 * {@link Configuration @Configuration}
 * classes, simply instantiate, configure and return the appropriate
 * {@code FormattingConversionService} object from a
 * {@link Bean @Bean} method.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
   * {@link Converter},
   * {@link ConverterFactory},
   * {@link GenericConverter}
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
    this.conversionService = new DefaultFormattingConversionService(embeddedValueResolver, registerDefaultFormatters);
    ConverterRegistry.registerConverters(converters, conversionService);
    registerFormatters(conversionService);
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
