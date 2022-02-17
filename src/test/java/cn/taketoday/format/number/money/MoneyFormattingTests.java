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

package cn.taketoday.format.number.money;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.format.annotation.NumberFormat;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.format.support.FormattingConversionService;
import cn.taketoday.validation.DataBinder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 4.2
 */
public class MoneyFormattingTests {

  private final FormattingConversionService conversionService = new DefaultFormattingConversionService();

  @BeforeEach
  public void setUp() {
    LocaleContextHolder.setLocale(Locale.US);
  }

  @AfterEach
  public void tearDown() {
    LocaleContextHolder.setLocale(null);
  }

  @Test
  public void testAmountAndUnit() {
    MoneyHolder bean = new MoneyHolder();
    DataBinder binder = new DataBinder(bean);
    binder.setConversionService(conversionService);

    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("amount", "USD 10.50");
    propertyValues.add("unit", "USD");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("USD10.50");
    assertThat(binder.getBindingResult().getFieldValue("unit")).isEqualTo("USD");
    assertThat(bean.getAmount().getNumber().doubleValue() == 10.5d).isTrue();
    assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");

    LocaleContextHolder.setLocale(Locale.CANADA);
    binder.bind(propertyValues);
    LocaleContextHolder.setLocale(Locale.US);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("USD10.50");
    assertThat(binder.getBindingResult().getFieldValue("unit")).isEqualTo("USD");
    assertThat(bean.getAmount().getNumber().doubleValue() == 10.5d).isTrue();
    assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");
  }

  @Test
  public void testAmountWithNumberFormat1() {
    FormattedMoneyHolder1 bean = new FormattedMoneyHolder1();
    DataBinder binder = new DataBinder(bean);
    binder.setConversionService(conversionService);

    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("amount", "$10.50");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("$10.50");
    assertThat(bean.getAmount().getNumber().doubleValue() == 10.5d).isTrue();
    assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");

    LocaleContextHolder.setLocale(Locale.CANADA);
    binder.bind(propertyValues);
    LocaleContextHolder.setLocale(Locale.US);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("$10.50");
    assertThat(bean.getAmount().getNumber().doubleValue() == 10.5d).isTrue();
    assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("CAD");
  }

  @Test
  public void testAmountWithNumberFormat2() {
    FormattedMoneyHolder2 bean = new FormattedMoneyHolder2();
    DataBinder binder = new DataBinder(bean);
    binder.setConversionService(conversionService);

    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("amount", "10.50");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("10.5");
    assertThat(bean.getAmount().getNumber().doubleValue() == 10.5d).isTrue();
    assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");
  }

  @Test
  public void testAmountWithNumberFormat3() {
    FormattedMoneyHolder3 bean = new FormattedMoneyHolder3();
    DataBinder binder = new DataBinder(bean);
    binder.setConversionService(conversionService);

    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("amount", "10%");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("10%");
    assertThat(bean.getAmount().getNumber().doubleValue() == 0.1d).isTrue();
    assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");
  }

  @Test
  public void testAmountWithNumberFormat4() {
    FormattedMoneyHolder4 bean = new FormattedMoneyHolder4();
    DataBinder binder = new DataBinder(bean);
    binder.setConversionService(conversionService);

    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("amount", "010.500");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("010.500");
    assertThat(bean.getAmount().getNumber().doubleValue() == 10.5d).isTrue();
    assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");
  }

  @Test
  public void testAmountWithNumberFormat5() {
    FormattedMoneyHolder5 bean = new FormattedMoneyHolder5();
    DataBinder binder = new DataBinder(bean);
    binder.setConversionService(conversionService);

    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("amount", "USD 10.50");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("USD 010.500");
    assertThat(bean.getAmount().getNumber().doubleValue() == 10.5d).isTrue();
    assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");

    LocaleContextHolder.setLocale(Locale.CANADA);
    binder.bind(propertyValues);
    LocaleContextHolder.setLocale(Locale.US);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("amount")).isEqualTo("USD 010.500");
    assertThat(bean.getAmount().getNumber().doubleValue() == 10.5d).isTrue();
    assertThat(bean.getAmount().getCurrency().getCurrencyCode()).isEqualTo("USD");
  }

  public static class MoneyHolder {

    private MonetaryAmount amount;

    private CurrencyUnit unit;

    public MonetaryAmount getAmount() {
      return amount;
    }

    public void setAmount(MonetaryAmount amount) {
      this.amount = amount;
    }

    public CurrencyUnit getUnit() {
      return unit;
    }

    public void setUnit(CurrencyUnit unit) {
      this.unit = unit;
    }
  }

  public static class FormattedMoneyHolder1 {

    @NumberFormat
    private MonetaryAmount amount;

    public MonetaryAmount getAmount() {
      return amount;
    }

    public void setAmount(MonetaryAmount amount) {
      this.amount = amount;
    }
  }

  public static class FormattedMoneyHolder2 {

    @NumberFormat(style = NumberFormat.Style.NUMBER)
    private MonetaryAmount amount;

    public MonetaryAmount getAmount() {
      return amount;
    }

    public void setAmount(MonetaryAmount amount) {
      this.amount = amount;
    }
  }

  public static class FormattedMoneyHolder3 {

    @NumberFormat(style = NumberFormat.Style.PERCENT)
    private MonetaryAmount amount;

    public MonetaryAmount getAmount() {
      return amount;
    }

    public void setAmount(MonetaryAmount amount) {
      this.amount = amount;
    }
  }

  public static class FormattedMoneyHolder4 {

    @NumberFormat(pattern = "#000.000#")
    private MonetaryAmount amount;

    public MonetaryAmount getAmount() {
      return amount;
    }

    public void setAmount(MonetaryAmount amount) {
      this.amount = amount;
    }
  }

  public static class FormattedMoneyHolder5 {

    @NumberFormat(pattern = "\u00A4\u00A4 #000.000#")
    private MonetaryAmount amount;

    public MonetaryAmount getAmount() {
      return amount;
    }

    public void setAmount(MonetaryAmount amount) {
      this.amount = amount;
    }
  }

}
