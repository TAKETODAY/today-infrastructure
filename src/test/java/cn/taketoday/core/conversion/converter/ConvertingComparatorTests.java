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

package cn.taketoday.core.conversion.converter;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConvertingComparator;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.util.comparator.ComparableComparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConvertingComparator}.
 *
 * @author Phillip Webb
 */
class ConvertingComparatorTests {

  private final StringToInteger converter = new StringToInteger();

  private final ConversionService conversionService = new DefaultConversionService();

  private final TestComparator comparator = new TestComparator();

  @Test
  void shouldThrowOnNullComparator() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ConvertingComparator<>(null, this.converter));
  }

  @Test
  void shouldThrowOnNullConverter() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ConvertingComparator<String, Integer>(this.comparator, null));
  }

  @Test
  void shouldThrowOnNullConversionService() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ConvertingComparator<String, Integer>(this.comparator, null, Integer.class));
  }

  @Test
  void shouldThrowOnNullType() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new ConvertingComparator<String, Integer>(this.comparator, this.conversionService, null));
  }

  @Test
  void shouldUseConverterOnCompare() throws Exception {
    ConvertingComparator<String, Integer> convertingComparator = new ConvertingComparator<>(
            this.comparator, this.converter);
    testConversion(convertingComparator);
  }

  @Test
  void shouldUseConversionServiceOnCompare() throws Exception {
    ConvertingComparator<String, Integer> convertingComparator = new ConvertingComparator<>(
            comparator, conversionService, Integer.class);
    testConversion(convertingComparator);
  }

  @Test
  void shouldGetForConverter() throws Exception {
    testConversion(new ConvertingComparator<>(comparator, converter));
  }

  private void testConversion(ConvertingComparator<String, Integer> convertingComparator) {
    assertThat(convertingComparator.compare("0", "0")).isEqualTo(0);
    assertThat(convertingComparator.compare("0", "1")).isEqualTo(-1);
    assertThat(convertingComparator.compare("1", "0")).isEqualTo(1);
    comparator.assertCalled();
  }

  @Test
  void shouldGetMapEntryKeys() throws Exception {
    ArrayList<Entry<String, Integer>> list = createReverseOrderMapEntryList();
    Comparator<Entry<String, Integer>> comparator = ConvertingComparator.mapEntryKeys(new ComparableComparator<String>());
    list.sort(comparator);
    assertThat(list.get(0).getKey()).isEqualTo("a");
  }

  @Test
  void shouldGetMapEntryValues() throws Exception {
    ArrayList<Entry<String, Integer>> list = createReverseOrderMapEntryList();
    Comparator<Entry<String, Integer>> comparator = ConvertingComparator.mapEntryValues(new ComparableComparator<Integer>());
    list.sort(comparator);
    assertThat(list.get(0).getValue()).isEqualTo(1);
  }

  private ArrayList<Entry<String, Integer>> createReverseOrderMapEntryList() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("b", 2);
    map.put("a", 1);
    ArrayList<Entry<String, Integer>> list = new ArrayList<>(
            map.entrySet());
    assertThat(list.get(0).getKey()).isEqualTo("b");
    return list;
  }

  private static class StringToInteger implements Converter<String, Integer> {

    @Override
    public Integer convert(String source) {
      return Integer.valueOf(source);
    }

  }

  private static class TestComparator extends ComparableComparator<Integer> {

    private boolean called;

    @Override
    public int compare(Integer o1, Integer o2) {
      assertThat(o1).isInstanceOf(Integer.class);
      assertThat(o2).isInstanceOf(Integer.class);
      this.called = true;
      return super.compare(o1, o2);
    }

    public void assertCalled() {
      assertThat(this.called).isTrue();
    }
  }

}
