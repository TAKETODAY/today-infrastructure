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

package cn.taketoday.beans;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.beans.testfixture.beans.CustomEnum;
import cn.taketoday.beans.testfixture.beans.GenericBean;
import cn.taketoday.core.conversion.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class BeanWrapperEnumTests {

  @Test
  public void testCustomEnum() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setPropertyValue("customEnum", "VALUE_1");
    assertThat(gb.getCustomEnum()).isEqualTo(CustomEnum.VALUE_1);
  }

  @Test
  public void testCustomEnumWithNull() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setPropertyValue("customEnum", null);
    assertThat(gb.getCustomEnum()).isNull();
  }

  @Test
  public void testCustomEnumWithEmptyString() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setPropertyValue("customEnum", "");
    assertThat(gb.getCustomEnum()).isNull();
  }

  @Test
  public void testCustomEnumArrayWithSingleValue() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setPropertyValue("customEnumArray", "VALUE_1");
    assertThat(gb.getCustomEnumArray().length).isEqualTo(1);
    assertThat(gb.getCustomEnumArray()[0]).isEqualTo(CustomEnum.VALUE_1);
  }

  @Test
  public void testCustomEnumArrayWithMultipleValues() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setPropertyValue("customEnumArray", new String[] { "VALUE_1", "VALUE_2" });
    assertThat(gb.getCustomEnumArray().length).isEqualTo(2);
    assertThat(gb.getCustomEnumArray()[0]).isEqualTo(CustomEnum.VALUE_1);
    assertThat(gb.getCustomEnumArray()[1]).isEqualTo(CustomEnum.VALUE_2);
  }

  @Test
  public void testCustomEnumArrayWithMultipleValuesAsCsv() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setPropertyValue("customEnumArray", "VALUE_1,VALUE_2");
    assertThat(gb.getCustomEnumArray().length).isEqualTo(2);
    assertThat(gb.getCustomEnumArray()[0]).isEqualTo(CustomEnum.VALUE_1);
    assertThat(gb.getCustomEnumArray()[1]).isEqualTo(CustomEnum.VALUE_2);
  }

  @Test
  public void testCustomEnumSetWithSingleValue() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setPropertyValue("customEnumSet", "VALUE_1");
    assertThat(gb.getCustomEnumSet().size()).isEqualTo(1);
    assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
  }

  @Test
  public void testCustomEnumSetWithMultipleValues() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setPropertyValue("customEnumSet", new String[] { "VALUE_1", "VALUE_2" });
    assertThat(gb.getCustomEnumSet().size()).isEqualTo(2);
    assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
    assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_2)).isTrue();
  }

  @Test
  public void testCustomEnumSetWithMultipleValuesAsCsv() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setPropertyValue("customEnumSet", "VALUE_1,VALUE_2");
    assertThat(gb.getCustomEnumSet().size()).isEqualTo(2);
    assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
    assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_2)).isTrue();
  }

  @Test
  public void testCustomEnumSetWithGetterSetterMismatch() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setPropertyValue("customEnumSetMismatch", new String[] { "VALUE_1", "VALUE_2" });
    assertThat(gb.getCustomEnumSet().size()).isEqualTo(2);
    assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
    assertThat(gb.getCustomEnumSet().contains(CustomEnum.VALUE_2)).isTrue();
  }

  @Test
  public void testStandardEnumSetWithMultipleValues() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setConversionService(new DefaultConversionService());
    assertThat(gb.getStandardEnumSet()).isNull();
    bw.setPropertyValue("standardEnumSet", new String[] { "VALUE_1", "VALUE_2" });
    assertThat(gb.getStandardEnumSet().size()).isEqualTo(2);
    assertThat(gb.getStandardEnumSet().contains(CustomEnum.VALUE_1)).isTrue();
    assertThat(gb.getStandardEnumSet().contains(CustomEnum.VALUE_2)).isTrue();
  }

  @Test
  public void testStandardEnumSetWithAutoGrowing() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setAutoGrowNestedPaths(true);
    assertThat(gb.getStandardEnumSet()).isNull();
    bw.getPropertyValue("standardEnumSet.class");
    assertThat(gb.getStandardEnumSet().size()).isEqualTo(0);
  }

  @Test
  public void testStandardEnumMapWithMultipleValues() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setConversionService(new DefaultConversionService());
    assertThat(gb.getStandardEnumMap()).isNull();
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("VALUE_1", 1);
    map.put("VALUE_2", 2);
    bw.setPropertyValue("standardEnumMap", map);
    assertThat(gb.getStandardEnumMap().size()).isEqualTo(2);
    assertThat(gb.getStandardEnumMap().get(CustomEnum.VALUE_1)).isEqualTo(1);
    assertThat(gb.getStandardEnumMap().get(CustomEnum.VALUE_2)).isEqualTo(2);
  }

  @Test
  public void testStandardEnumMapWithAutoGrowing() {
    GenericBean<?> gb = new GenericBean<>();
    BeanWrapper bw = new BeanWrapperImpl(gb);
    bw.setAutoGrowNestedPaths(true);
    assertThat(gb.getStandardEnumMap()).isNull();
    bw.setPropertyValue("standardEnumMap[VALUE_1]", 1);
    assertThat(gb.getStandardEnumMap().size()).isEqualTo(1);
    assertThat(gb.getStandardEnumMap().get(CustomEnum.VALUE_1)).isEqualTo(1);
  }

  @Test
  public void testNonPublicEnum() {
    NonPublicEnumHolder holder = new NonPublicEnumHolder();
    BeanWrapper bw = new BeanWrapperImpl(holder);
    bw.setPropertyValue("nonPublicEnum", "VALUE_1");
    assertThat(holder.getNonPublicEnum()).isEqualTo(NonPublicEnum.VALUE_1);
  }

  enum NonPublicEnum {

    VALUE_1, VALUE_2
  }

  static class NonPublicEnumHolder {

    private NonPublicEnum nonPublicEnum;

    public NonPublicEnum getNonPublicEnum() {
      return nonPublicEnum;
    }

    public void setNonPublicEnum(NonPublicEnum nonPublicEnum) {
      this.nonPublicEnum = nonPublicEnum;
    }
  }

}
