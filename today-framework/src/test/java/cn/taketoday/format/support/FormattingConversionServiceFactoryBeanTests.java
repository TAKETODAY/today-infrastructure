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

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.format.AnnotationFormatterFactory;
import cn.taketoday.format.Formatter;
import cn.taketoday.format.FormatterRegistrar;
import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.format.Parser;
import cn.taketoday.format.Printer;
import cn.taketoday.format.annotation.NumberFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class FormattingConversionServiceFactoryBeanTests {

  @Test
  public void testDefaultFormattersOn() throws Exception {
    FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
    factory.afterPropertiesSet();
    FormattingConversionService fcs = factory.getObject();
    TypeDescriptor descriptor = new TypeDescriptor(TestBean.class.getDeclaredField("pattern"));

    LocaleContextHolder.setLocale(Locale.GERMAN);
    try {
      Object value = fcs.convert("15,00", TypeDescriptor.valueOf(String.class), descriptor);
      assertThat(value).isEqualTo(15.0);
      value = fcs.convert(15.0, descriptor, TypeDescriptor.valueOf(String.class));
      assertThat(value).isEqualTo("15");
    }
    finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  @Test
  public void testDefaultFormattersOff() throws Exception {
    FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
    factory.setRegisterDefaultFormatters(false);
    factory.afterPropertiesSet();
    FormattingConversionService fcs = factory.getObject();
    TypeDescriptor descriptor = new TypeDescriptor(TestBean.class.getDeclaredField("pattern"));

    assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
                    fcs.convert("15,00", TypeDescriptor.valueOf(String.class), descriptor))
            .withCauseInstanceOf(NumberFormatException.class);
  }

  @Test
  public void testCustomFormatter() throws Exception {
    FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
    Set<Object> formatters = new HashSet<>();
    formatters.add(new TestBeanFormatter());
    formatters.add(new SpecialIntAnnotationFormatterFactory());
    factory.setFormatters(formatters);
    factory.afterPropertiesSet();
    FormattingConversionService fcs = factory.getObject();

    TestBean testBean = fcs.convert("5", TestBean.class);
    assertThat(testBean.getSpecialInt()).isEqualTo(5);
    assertThat(fcs.convert(testBean, String.class)).isEqualTo("5");

    TypeDescriptor descriptor = new TypeDescriptor(TestBean.class.getDeclaredField("specialInt"));
    Object value = fcs.convert(":5", TypeDescriptor.valueOf(String.class), descriptor);
    assertThat(value).isEqualTo(5);
    value = fcs.convert(5, descriptor, TypeDescriptor.valueOf(String.class));
    assertThat(value).isEqualTo(":5");
  }

  @Test
  public void testFormatterRegistrar() throws Exception {
    FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
    Set<FormatterRegistrar> registrars = new HashSet<>();
    registrars.add(new TestFormatterRegistrar());
    factory.setFormatterRegistrars(registrars);
    factory.afterPropertiesSet();
    FormattingConversionService fcs = factory.getObject();

    TestBean testBean = fcs.convert("5", TestBean.class);
    assertThat(testBean.getSpecialInt()).isEqualTo(5);
    assertThat(fcs.convert(testBean, String.class)).isEqualTo("5");
  }

  @Test
  public void testInvalidFormatter() throws Exception {
    FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
    Set<Object> formatters = new HashSet<>();
    formatters.add(new Object());
    factory.setFormatters(formatters);
    assertThatIllegalArgumentException().isThrownBy(factory::afterPropertiesSet);
  }

  @Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
  @Retention(RetentionPolicy.RUNTIME)
  private @interface SpecialInt {

    @AliasFor("alias")
    String value() default "";

    @AliasFor("value")
    String alias() default "";
  }

  private static class TestBean {

    @NumberFormat(pattern = "##,00")
    private double pattern;

    @SpecialInt("aliased")
    private int specialInt;

    public int getSpecialInt() {
      return specialInt;
    }

    public void setSpecialInt(int field) {
      this.specialInt = field;
    }
  }

  private static class TestBeanFormatter implements Formatter<TestBean> {

    @Override
    public String print(TestBean object, Locale locale) {
      return String.valueOf(object.getSpecialInt());
    }

    @Override
    public TestBean parse(String text, Locale locale) throws ParseException {
      TestBean object = new TestBean();
      object.setSpecialInt(Integer.parseInt(text));
      return object;
    }
  }

  private static class SpecialIntAnnotationFormatterFactory implements AnnotationFormatterFactory<SpecialInt> {

    private final Set<Class<?>> fieldTypes = new HashSet<>(1);

    public SpecialIntAnnotationFormatterFactory() {
      fieldTypes.add(Integer.class);
    }

    @Override
    public Set<Class<?>> getFieldTypes() {
      return fieldTypes;
    }

    @Override
    public Printer<?> getPrinter(SpecialInt annotation, Class<?> fieldType) {
      assertThat(annotation.value()).isEqualTo("aliased");
      assertThat(annotation.alias()).isEqualTo("aliased");
      return (object, locale) -> ":" + object.toString();
    }

    @Override
    public Parser<?> getParser(SpecialInt annotation, Class<?> fieldType) {
      assertThat(annotation.value()).isEqualTo("aliased");
      assertThat(annotation.alias()).isEqualTo("aliased");
      return (text, locale) -> Integer.parseInt(text, 1, text.length(), 10);
    }
  }

  private static class TestFormatterRegistrar implements FormatterRegistrar {

    @Override
    public void registerFormatters(FormatterRegistry registry) {
      registry.addFormatter(new TestBeanFormatter());
    }
  }

}
