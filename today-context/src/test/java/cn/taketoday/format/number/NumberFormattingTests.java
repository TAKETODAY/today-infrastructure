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

package cn.taketoday.format.number;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.format.annotation.NumberFormat;
import cn.taketoday.format.annotation.NumberFormat.Style;
import cn.taketoday.format.support.FormattingConversionService;
import cn.taketoday.validation.DataBinder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class NumberFormattingTests {

  private final FormattingConversionService conversionService = new FormattingConversionService();

  private DataBinder binder;

  @BeforeEach
  public void setUp() {
    DefaultConversionService.addDefaultConverters(conversionService);
    conversionService.setEmbeddedValueResolver(strVal -> {
      if ("${pattern}".equals(strVal)) {
        return "#,##.00";
      }
      else {
        return strVal;
      }
    });
    conversionService.addFormatterForFieldType(Number.class, new NumberStyleFormatter());
    conversionService.addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());
    LocaleContextHolder.setLocale(Locale.US);
    binder = new DataBinder(new TestBean());
    binder.setConversionService(conversionService);
  }

  @AfterEach
  public void tearDown() {
    LocaleContextHolder.setLocale(null);
  }

  @Test
  public void testDefaultNumberFormatting() {
    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("numberDefault", "3,339.12");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("numberDefault")).isEqualTo("3,339");
  }

  @Test
  public void testDefaultNumberFormattingAnnotated() {
    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("numberDefaultAnnotated", "3,339.12");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("numberDefaultAnnotated")).isEqualTo("3,339.12");
  }

  @Test
  public void testCurrencyFormatting() {
    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("currency", "$3,339.12");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("currency")).isEqualTo("$3,339.12");
  }

  @Test
  public void testPercentFormatting() {
    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("percent", "53%");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("percent")).isEqualTo("53%");
  }

  @Test
  public void testPatternFormatting() {
    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("pattern", "1,25.00");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("pattern")).isEqualTo("1,25.00");
  }

  @Test
  public void testPatternArrayFormatting() {
    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("patternArray", new String[] { "1,25.00", "2,35.00" });
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("patternArray[0]")).isEqualTo("1,25.00");
    assertThat(binder.getBindingResult().getFieldValue("patternArray[1]")).isEqualTo("2,35.00");

    propertyValues = new PropertyValues();
    propertyValues.add("patternArray[0]", "1,25.00");
    propertyValues.add("patternArray[1]", "2,35.00");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("patternArray[0]")).isEqualTo("1,25.00");
    assertThat(binder.getBindingResult().getFieldValue("patternArray[1]")).isEqualTo("2,35.00");
  }

  @Test
  public void testPatternListFormatting() {
    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("patternList", new String[] { "1,25.00", "2,35.00" });
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("patternList[0]")).isEqualTo("1,25.00");
    assertThat(binder.getBindingResult().getFieldValue("patternList[1]")).isEqualTo("2,35.00");

    propertyValues = new PropertyValues();
    propertyValues.add("patternList[0]", "1,25.00");
    propertyValues.add("patternList[1]", "2,35.00");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("patternList[0]")).isEqualTo("1,25.00");
    assertThat(binder.getBindingResult().getFieldValue("patternList[1]")).isEqualTo("2,35.00");
  }

  @Test
  public void testPatternList2FormattingListElement() {
    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("patternList2[0]", "1,25.00");
    propertyValues.add("patternList2[1]", "2,35.00");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("patternList2[0]")).isEqualTo("1,25.00");
    assertThat(binder.getBindingResult().getFieldValue("patternList2[1]")).isEqualTo("2,35.00");
  }

  @Test
  public void testPatternList2FormattingList() {
    PropertyValues propertyValues = new PropertyValues();
    propertyValues.add("patternList2[0]", "1,25.00");
    propertyValues.add("patternList2[1]", "2,35.00");
    binder.bind(propertyValues);
    assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
    assertThat(binder.getBindingResult().getFieldValue("patternList2")).isEqualTo("1,25.00,2,35.00");
  }

  @SuppressWarnings("unused")
  private static class TestBean {

    private Integer numberDefault;

    @NumberFormat
    private Double numberDefaultAnnotated;

    @NumberFormat(style = Style.CURRENCY)
    private BigDecimal currency;

    @NumberFormat(style = Style.PERCENT)
    private BigDecimal percent;

    @NumberFormat(pattern = "${pattern}")
    private BigDecimal pattern;

    @NumberFormat(pattern = "#,##.00")
    private BigDecimal[] patternArray;

    @NumberFormat(pattern = "#,##.00")
    private List<BigDecimal> patternList;

    @NumberFormat(pattern = "#,##.00")
    private List<BigDecimal> patternList2;

    public Integer getNumberDefault() {
      return numberDefault;
    }

    public void setNumberDefault(Integer numberDefault) {
      this.numberDefault = numberDefault;
    }

    public Double getNumberDefaultAnnotated() {
      return numberDefaultAnnotated;
    }

    public void setNumberDefaultAnnotated(Double numberDefaultAnnotated) {
      this.numberDefaultAnnotated = numberDefaultAnnotated;
    }

    public BigDecimal getCurrency() {
      return currency;
    }

    public void setCurrency(BigDecimal currency) {
      this.currency = currency;
    }

    public BigDecimal getPercent() {
      return percent;
    }

    public void setPercent(BigDecimal percent) {
      this.percent = percent;
    }

    public BigDecimal getPattern() {
      return pattern;
    }

    public void setPattern(BigDecimal pattern) {
      this.pattern = pattern;
    }

    public BigDecimal[] getPatternArray() {
      return patternArray;
    }

    public void setPatternArray(BigDecimal[] patternArray) {
      this.patternArray = patternArray;
    }

    public List<BigDecimal> getPatternList() {
      return patternList;
    }

    public void setPatternList(List<BigDecimal> patternList) {
      this.patternList = patternList;
    }

    public List<BigDecimal> getPatternList2() {
      return patternList2;
    }

    public void setPatternList2(List<BigDecimal> patternList2) {
      this.patternList2 = patternList2;
    }
  }

}
