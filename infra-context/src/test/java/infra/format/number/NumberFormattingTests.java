/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.format.number;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import infra.beans.PropertyValues;
import infra.core.conversion.support.DefaultConversionService;
import infra.core.i18n.LocaleContextHolder;
import infra.format.annotation.NumberFormat;
import infra.format.annotation.NumberFormat.Style;
import infra.format.support.FormattingConversionService;
import infra.validation.DataBinder;

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
