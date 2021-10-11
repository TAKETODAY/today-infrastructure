/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.conversion.support;

import org.junit.Test;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConverterNotFoundException;
import cn.taketoday.core.conversion.ConverterRegistry;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author TODAY 2021/3/22 17:03
 * @since 3.0
 */
public class DefaultConversionServiceTests {

  private final DefaultConversionService conversionService = new DefaultConversionService();

  {
    DefaultConversionService.addDefaultConverters(conversionService);
  }

  @Test
  public void stringToCharacter() {
    assertThat(conversionService.convert("1", Character.class)).isEqualTo(Character.valueOf('1'));
  }

  @Test
  public void stringToCharacterEmptyString() {
    assertThat(conversionService.convert("", Character.class)).isEqualTo(null);
  }

  @Test
  public void stringToCharacterInvalidString() {
    assertThatExceptionOfType(ConversionFailedException.class)
            .isThrownBy(() -> conversionService.convert("invalid", Character.class));
  }

  @Test
  public void characterToString() {
    assertThat(conversionService.convert('3', String.class)).isEqualTo("3");
  }

  @Test
  public void stringToBooleanTrue() {
    assertThat(conversionService.convert("true", Boolean.class)).isEqualTo(true);
    assertThat(conversionService.convert("on", Boolean.class)).isEqualTo(true);
    assertThat(conversionService.convert("yes", Boolean.class)).isEqualTo(true);
    assertThat(conversionService.convert("1", Boolean.class)).isEqualTo(true);
    assertThat(conversionService.convert("TRUE", Boolean.class)).isEqualTo(true);
    assertThat(conversionService.convert("ON", Boolean.class)).isEqualTo(true);
    assertThat(conversionService.convert("YES", Boolean.class)).isEqualTo(true);
  }

  @Test
  public void stringToBooleanFalse() {
    assertThat(conversionService.convert("false", Boolean.class)).isEqualTo(false);
    assertThat(conversionService.convert("off", Boolean.class)).isEqualTo(false);
    assertThat(conversionService.convert("no", Boolean.class)).isEqualTo(false);
    assertThat(conversionService.convert("0", Boolean.class)).isEqualTo(false);
    assertThat(conversionService.convert("FALSE", Boolean.class)).isEqualTo(false);
    assertThat(conversionService.convert("OFF", Boolean.class)).isEqualTo(false);
    assertThat(conversionService.convert("NO", Boolean.class)).isEqualTo(false);
  }

  @Test
  public void stringToBooleanEmptyString() {
    assertThat(conversionService.convert("", Boolean.class)).isNull();
    assertThat(conversionService.convert("", boolean.class)).isEqualTo(false);
  }

  @Test
  public void stringToBooleanInvalidString() {
    assertThatExceptionOfType(ConversionFailedException.class)
            .isThrownBy(() -> conversionService.convert("invalid", Boolean.class));
  }

  @Test
  public void booleanToString() {
    assertThat(conversionService.convert(true, String.class)).isEqualTo("true");
  }

  @Test
  public void stringToByte() {
    assertThat(conversionService.convert("1", Byte.class)).isEqualTo((byte) 1);
  }

  @Test
  public void byteToString() {
    assertThat(conversionService.convert("A".getBytes()[0], String.class)).isEqualTo("65");
  }

  @Test
  public void stringToShort() {
    assertThat(conversionService.convert("1", Short.class)).isEqualTo((short) 1);
  }

  @Test
  public void shortToString() {
    short three = 3;
    assertThat(conversionService.convert(three, String.class)).isEqualTo("3");
  }

  @Test
  public void stringToInteger() {
    assertThat(conversionService.convert("1", Integer.class)).isEqualTo((int) Integer.valueOf(1));
  }

  @Test
  public void integerToString() {
    assertThat(conversionService.convert(3, String.class)).isEqualTo("3");
  }

  @Test
  public void stringToLong() {
    assertThat(conversionService.convert("1", Long.class)).isEqualTo(Long.valueOf(1));
  }

  @Test
  public void longToString() {
    assertThat(conversionService.convert(3L, String.class)).isEqualTo("3");
  }

  @Test
  public void stringToFloat() {
    assertThat(conversionService.convert("1.0", Float.class)).isEqualTo(Float.valueOf("1.0"));
  }

  @Test
  public void floatToString() {
    assertThat(conversionService.convert(Float.valueOf("1.0"), String.class)).isEqualTo("1.0");
  }

  @Test
  public void stringToDouble() {
    assertThat(conversionService.convert("1.0", Double.class)).isEqualTo(Double.valueOf("1.0"));
  }

  @Test
  public void doubleToString() {
    assertThat(conversionService.convert(Double.valueOf("1.0"), String.class)).isEqualTo("1.0");
  }

  @Test
  public void stringToBigInteger() {
    assertThat(conversionService.convert("1", BigInteger.class)).isEqualTo(new BigInteger("1"));
  }

  @Test
  public void bigIntegerToString() {
    assertThat(conversionService.convert(new BigInteger("100"), String.class)).isEqualTo("100");
  }

  @Test
  public void stringToBigDecimal() {
    assertThat(conversionService.convert("1.0", BigDecimal.class)).isEqualTo(new BigDecimal("1.0"));
  }

  @Test
  public void bigDecimalToString() {
    assertThat(conversionService.convert(new BigDecimal("100.00"), String.class)).isEqualTo("100.00");
  }

  @Test
  public void stringToNumber() {
    final Number convert = conversionService.convert("1.0", Number.class);
    assertThat(convert).isEqualTo(new BigDecimal("1.0"));
  }

  @Test
  public void stringToNumberEmptyString() {
    assertThat(conversionService.convert("", Number.class)).isNull();
    assertThat(conversionService.convert("", int.class)).isZero();
    assertThat(conversionService.convert("", Integer.class)).isNull();
  }

  @Test
  public void stringToEnum() {
    assertThat(conversionService.convert("BAR", Foo.class)).isEqualTo(Foo.BAR);
  }

  @Test
  public void stringToEnumWithSubclass() {
    assertThat(conversionService.convert("BAZ", SubFoo.BAR.getClass())).isEqualTo(SubFoo.BAZ);
  }

  @Test
  public void stringToEnumEmptyString() {
    assertThat(conversionService.convert("", Foo.class)).isEqualTo(null);
  }

  @Test
  public void enumToString() {
    assertThat(conversionService.convert(Foo.BAR, String.class)).isEqualTo("BAR");
  }

  @Test
  public void integerToEnum() {
    assertThat(conversionService.convert(0, Foo.class)).isEqualTo(Foo.BAR);
  }

  @Test
  public void integerToEnumWithSubclass() {
    final SubFoo convert = conversionService.convert(1, SubFoo.BAR.getClass());
//    final SubFoo convert = conversionService.convert(1, SubFoo.class);

    assertThat(convert).isEqualTo(SubFoo.BAZ);
  }

  @Test
  public void integerToEnumNull() {
    assertThat(conversionService.convert(null, Foo.class)).isEqualTo(null);
  }

  @Test
  public void enumToInteger() {
    assertThat(conversionService.convert(Foo.BAR, Integer.class))
            .isEqualTo(0);
  }

  @Test
  public void stringToEnumSet() throws Exception {
    final Field enumSet = getClass().getField("enumSet");
    final TypeDescriptor descriptor = TypeDescriptor.fromField(enumSet);
    final Object actual = conversionService.convert("BAR", descriptor);
    assertThat(actual).isEqualTo(EnumSet.of(Foo.BAR));
  }

  @Test
  public void stringToLocale() {
    assertThat(conversionService.convert("en", Locale.class)).isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void stringToLocaleWithCountry() {
    assertThat(conversionService.convert("en_US", Locale.class)).isEqualTo(Locale.US);
  }

  @Test
  public void stringToLocaleWithLanguageTag() {
    assertThat(conversionService.convert("en-US", Locale.class)).isEqualTo(Locale.US);
  }

  @Test
  public void stringToCharset() {
    assertThat(conversionService.convert("UTF-8", Charset.class)).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  public void charsetToString() {
    assertThat(conversionService.convert(StandardCharsets.UTF_8, String.class)).isEqualTo("UTF-8");
  }

  @Test
  public void stringToCurrency() {
    assertThat(conversionService.convert("EUR", Currency.class)).isEqualTo(Currency.getInstance("EUR"));
  }

  @Test
  public void currencyToString() {
    assertThat(conversionService.convert(Currency.getInstance("USD"), String.class)).isEqualTo("USD");
  }

  @Test
  public void stringToString() {
    String str = "test";
    assertThat(conversionService.convert(str, String.class)).isSameAs(str);
  }

  @Test
  public void uuidToStringAndStringToUuid() {
    UUID uuid = UUID.randomUUID();
    String convertToString = conversionService.convert(uuid, String.class);
    UUID convertToUUID = conversionService.convert(convertToString, UUID.class);
    assertThat(convertToUUID).isEqualTo(uuid);
  }

  @Test
  public void numberToNumber() {
    assertThat(conversionService.convert(1, Long.class)).isEqualTo(Long.valueOf(1));
  }

//  @Test
//  public void numberToNumberNotSupportedNumber() {
//    assertThatExceptionOfType(ConverterNotFoundException.class)
//            .isThrownBy(() -> conversionService.convert(1, CustomNumber.class));
//  }
//  @Test
//  public void convertObjectToObjectNoValueOfMethodOrConstructor() {
//    assertThatExceptionOfType(ConverterNotFoundException.class)
//            .isThrownBy(() -> conversionService.convert(Long.valueOf(3), SSN.class));
//  }

  @Test
  public void numberToCharacter() {
    assertThat(conversionService.convert(65, Character.class)).isEqualTo(Character.valueOf('A'));
  }

  @Test
  public void characterToNumber() {
    assertThat(conversionService.convert('A', Integer.class)).isEqualTo(65);
  }

  // collection conversion

  @Test
  public void convertArrayToCollectionInterface() {
    List<?> result = conversionService.convert(new String[] { "1", "2", "3" }, List.class);
    assertThat(result.get(0)).isEqualTo("1");
    assertThat(result.get(1)).isEqualTo("2");
    assertThat(result.get(2)).isEqualTo("3");
  }

  @Test
  public void convertArrayToCollectionGenericTypeConversion() throws Exception {
    final String[] source = { "1", "2", "3" };
    final TypeDescriptor targetType = TypeDescriptor.fromField(getClass().getDeclaredField("genericList"));
    List<Integer> result = conversionService.convert(source, targetType);

    assertThat((int) result.get(0)).isEqualTo((int) Integer.valueOf(1));
    assertThat((int) result.get(1)).isEqualTo((int) Integer.valueOf(2));
    assertThat((int) result.get(2)).isEqualTo((int) Integer.valueOf(3));
  }

  @Test
  public void convertArrayToStream() throws Exception {
    String[] source = { "1", "3", "4" };
    final TypeDescriptor targetType = TypeDescriptor.fromField(getClass().getDeclaredField("genericStream"));
    Stream<Integer> result = this.conversionService.convert(source, targetType);
    assertThat(result.mapToInt(x -> x).sum()).isEqualTo(8);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void spr7766() throws Exception {

    ConverterRegistry registry = conversionService;
    registry.addConverter(new ColorConverter());
    final Method handlerMethod = getClass().getMethod("handlerMethod", List.class);
    final TypeDescriptor descriptor = TypeDescriptor.forParameter(handlerMethod, 0);

    List<Color> colors = conversionService.convert(new String[] { "ffffff", "#000000" }, descriptor);

    assertThat(colors.size()).isEqualTo(2);
    assertThat(colors.get(0)).isEqualTo(Color.WHITE);
    assertThat(colors.get(1)).isEqualTo(Color.BLACK);
  }

  @Test
  public void convertArrayToCollectionImpl() {
    ArrayList<?> result = conversionService.convert(new String[] { "1", "2", "3" }, ArrayList.class);
    assertThat(result.get(0)).isEqualTo("1");
    assertThat(result.get(1)).isEqualTo("2");
    assertThat(result.get(2)).isEqualTo("3");
  }

  @Test
  public void convertArrayToAbstractCollection() {
    assertThatExceptionOfType(ConversionFailedException.class)
            .isThrownBy(() -> conversionService.convert(new String[] { "1", "2", "3" }, AbstractList.class));
  }

  @Test
  public void convertArrayToString() {
    String result = conversionService.convert(new String[] { "1", "2", "3" }, String.class);
    assertThat(result).isEqualTo("1,2,3");
  }

  @Test
  public void convertArrayToStringWithElementConversion() {
    String result = conversionService.convert(new Integer[] { 1, 2, 3 }, String.class);
    assertThat(result).isEqualTo("1,2,3");
  }

  @Test
  public void convertEmptyArrayToString() {
    String result = conversionService.convert(new String[0], String.class);
    assertThat(result).isEqualTo("");
  }

  @Test
  public void convertStringToArray() {
    String[] result = conversionService.convert("1,2,3", String[].class);
    assertThat(result.length).isEqualTo(3);
    assertThat(result[0]).isEqualTo("1");
    assertThat(result[1]).isEqualTo("2");
    assertThat(result[2]).isEqualTo("3");
  }

  @Test
  public void convertStringToArrayWithElementConversion() {
    Integer[] result = conversionService.convert("1,2,3", Integer[].class);
    assertThat(result.length).isEqualTo(3);
    assertThat((int) result[0]).isEqualTo((int) Integer.valueOf(1));
    assertThat((int) result[1]).isEqualTo((int) Integer.valueOf(2));
    assertThat((int) result[2]).isEqualTo((int) Integer.valueOf(3));
  }

  @Test
  public void convertStringToPrimitiveArrayWithElementConversion() {
    int[] result = conversionService.convert("1,2,3", int[].class);
    assertThat(result.length).isEqualTo(3);
    assertThat(result[0]).isEqualTo(1);
    assertThat(result[1]).isEqualTo(2);
    assertThat(result[2]).isEqualTo(3);
  }

  @Test
  public void convertEmptyStringToArray() {
    String[] result = conversionService.convert("", String[].class);
    assertThat(result.length).isEqualTo(1);
  }

  @Test
  public void convertArrayToObject() {
    Object[] array = new Object[] { 3L };
    Object result = conversionService.convert(array, Long.class);
    assertThat(result).isEqualTo(3L);
  }

  @Test
  public void convertArrayToObjectWithElementConversion() {
    String[] array = new String[] { "3" };
    Integer result = conversionService.convert(array, Integer.class);
    assertThat((int) result).isEqualTo((int) Integer.valueOf(3));
  }

  @Test
  public void convertArrayToObjectAssignableTargetType() {
    Long[] array = new Long[] { 3L };
    Long[] result = (Long[]) conversionService.convert(array, Object.class);
    assertThat(result).isEqualTo(array);
  }

  @Test
  public void convertObjectToArray() {
    Object[] result = conversionService.convert(3L, Object[].class);
    assertThat(result.length).isEqualTo(1);
    assertThat(result[0]).isEqualTo(3L);
  }

  @Test
  public void convertObjectToArrayWithElementConversion() {
    Integer[] result = conversionService.convert(3L, Integer[].class);
    assertThat(result.length).isEqualTo(1);
    assertThat((int) result[0]).isEqualTo((int) Integer.valueOf(3));
  }

  @Test
  public void convertCollectionToArray() {
    List<String> list = new ArrayList<>();
    list.add("1");
    list.add("2");
    list.add("3");
    String[] result = conversionService.convert(list, String[].class);
    assertThat(result[0]).isEqualTo("1");
    assertThat(result[1]).isEqualTo("2");
    assertThat(result[2]).isEqualTo("3");
  }

  @Test
  public void convertCollectionToArrayWithElementConversion() {
    List<String> list = new ArrayList<>();
    list.add("1");
    list.add("2");
    list.add("3");
    Integer[] result = conversionService.convert(list, Integer[].class);
    assertThat((int) result[0]).isEqualTo((int) Integer.valueOf(1));
    assertThat((int) result[1]).isEqualTo((int) Integer.valueOf(2));
    assertThat((int) result[2]).isEqualTo((int) Integer.valueOf(3));
  }

  @Test
  public void convertCollectionToString() {
    List<String> list = Arrays.asList("foo", "bar");
    String result = conversionService.convert(list, String.class);
    assertThat(result).isEqualTo("foo,bar");
  }

  @Test
  public void convertCollectionToStringWithElementConversion() throws Exception {
    List<Integer> list = Arrays.asList(3, 5);
    String result = conversionService.convert(list, String.class);
    assertThat(result).isEqualTo("3,5");
  }

  @Test
  public void convertStringToCollection() {
    List<?> result = conversionService.convert("1,2,3", List.class);
    assertThat(result.size()).isEqualTo(3);
    assertThat(result.get(0)).isEqualTo("1");
    assertThat(result.get(1)).isEqualTo("2");
    assertThat(result.get(2)).isEqualTo("3");
  }

  @Test
  public void convertStringToCollectionWithElementConversion() throws Exception {
    final Field genericList = getClass().getField("genericList");
    final TypeDescriptor descriptor = TypeDescriptor.fromField(genericList);
    List<?> result = conversionService.convert("1,2,3", descriptor);
    assertThat(result.size()).isEqualTo(3);
    assertThat(result.get(0)).isEqualTo(1);
    assertThat(result.get(1)).isEqualTo(2);
    assertThat(result.get(2)).isEqualTo(3);
  }

  @Test
  public void convertEmptyStringToCollection() {
    Collection<?> result = conversionService.convert("", Collection.class);
    assertThat(result.size()).isEqualTo(1);
    final Object next = result.iterator().next();
    assertThat(next).isEqualTo("");
  }

  @Test
  public void convertCollectionToObject() {
    List<Long> list = Collections.singletonList(3L);
    Long result = conversionService.convert(list, Long.class);
    assertThat(result).isEqualTo(Long.valueOf(3));
  }

  @Test
  public void convertCollectionToObjectWithElementConversion() {
    List<String> list = Collections.singletonList("3");
    Integer result = conversionService.convert(list, Integer.class);
    assertThat((int) result).isEqualTo((int) Integer.valueOf(3));
  }

  @Test
  public void convertCollectionToObjectAssignableTarget() throws Exception {
    Collection<String> source = new ArrayList<>();
    source.add("foo");
    final Field assignableTarget = getClass().getField("assignableTarget");
    final TypeDescriptor descriptor = TypeDescriptor.fromField(assignableTarget);

    Object result = conversionService.convert(source, descriptor);
    assertThat(result).isEqualTo(source);
  }

  @Test
  public void convertCollectionToObjectWithCustomConverter() {
    List<String> source = new ArrayList<>();
    source.add("A");
    source.add("B");

    conversionService.addConverter(ListWrapper.class, List.class, ListWrapper::new);

    ListWrapper result = conversionService.convert(source, ListWrapper.class);
    assertThat(result.getList()).isSameAs(source);
  }

  @Test
  public void convertObjectToCollection() {
    List<?> result = conversionService.convert(3L, List.class);
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0)).isEqualTo(3L);
  }

  @Test
  public void convertObjectToCollectionWithElementConversion() throws Exception {
    final Field genericList = getClass().getField("genericList");
    final TypeDescriptor descriptor = TypeDescriptor.fromField(genericList);

    List<Integer> result = conversionService.convert(3L, descriptor);
    assertThat(result.size()).isEqualTo(1);
    assertThat((int) result.get(0)).isEqualTo((int) Integer.valueOf(3));
  }

  @Test
  public void convertStringArrayToIntegerArray() {
    Integer[] result = conversionService.convert(new String[] { "1", "2", "3" }, Integer[].class);
    assertThat((int) result[0]).isEqualTo((int) Integer.valueOf(1));
    assertThat((int) result[1]).isEqualTo((int) Integer.valueOf(2));
    assertThat((int) result[2]).isEqualTo((int) Integer.valueOf(3));
  }

  @Test
  public void convertStringArrayToIntArray() {
    int[] result = conversionService.convert(new String[] { "1", "2", "3" }, int[].class);
    assertThat(result[0]).isEqualTo(1);
    assertThat(result[1]).isEqualTo(2);
    assertThat(result[2]).isEqualTo(3);
  }

  @Test
  public void convertIntegerArrayToIntegerArray() {
    Integer[] result = conversionService.convert(new Integer[] { 1, 2, 3 }, Integer[].class);
    assertThat((int) result[0]).isEqualTo((int) Integer.valueOf(1));
    assertThat((int) result[1]).isEqualTo((int) Integer.valueOf(2));
    assertThat((int) result[2]).isEqualTo((int) Integer.valueOf(3));
  }

  @Test
  public void convertIntegerArrayToIntArray() {
    int[] result = conversionService.convert(new Integer[] { 1, 2, 3 }, int[].class);
    assertThat(result[0]).isEqualTo(1);
    assertThat(result[1]).isEqualTo(2);
    assertThat(result[2]).isEqualTo(3);
  }

  @Test
  public void convertObjectArrayToIntegerArray() {
    Integer[] result = conversionService.convert(new Object[] { 1, 2, 3 }, Integer[].class);
    assertThat((int) result[0]).isEqualTo((int) Integer.valueOf(1));
    assertThat((int) result[1]).isEqualTo((int) Integer.valueOf(2));
    assertThat((int) result[2]).isEqualTo((int) Integer.valueOf(3));
  }

  @Test
  public void convertObjectArrayToIntArray() {
    int[] result = conversionService.convert(new Object[] { 1, 2, 3 }, int[].class);
    assertThat(result[0]).isEqualTo(1);
    assertThat(result[1]).isEqualTo(2);
    assertThat(result[2]).isEqualTo(3);
  }

  @Test
  public void convertByteArrayToWrapperArray() {
    byte[] byteArray = new byte[] { 1, 2, 3 };
    Byte[] converted = conversionService.convert(byteArray, Byte[].class);
    assertThat(converted).isEqualTo(new Byte[] { 1, 2, 3 });
  }

  @Test
  public void convertArrayToArrayAssignable() {
    int[] result = conversionService.convert(new int[] { 1, 2, 3 }, int[].class);
    assertThat(result[0]).isEqualTo(1);
    assertThat(result[1]).isEqualTo(2);
    assertThat(result[2]).isEqualTo(3);
  }

  @Test
  public void convertListOfNonStringifiable() {
    List<Object> list = Arrays.asList(new TestEntity(1L), new TestEntity(2L));
    assertThat(conversionService.canConvert(list.getClass(), String.class)).isTrue();
    try {
      conversionService.convert(list, String.class);
    }
    catch (ConversionFailedException ex) {
      assertThat(ex.getMessage().contains(list.getClass().getName())).isTrue();
      assertThat(ex.getCause() instanceof ConverterNotFoundException).isTrue();
      assertThat(ex.getCause().getMessage().contains(TestEntity.class.getName())).isTrue();
    }
  }

  @Test
  public void convertListOfStringToString() {
    List<String> list = Arrays.asList("Foo", "Bar");
    assertThat(conversionService.canConvert(list.getClass(), String.class)).isTrue();
    String result = conversionService.convert(list, String.class);
    assertThat(result).isEqualTo("Foo,Bar");
  }

  @Test
  public void convertListOfListToString() {
    List<String> list1 = Arrays.asList("Foo", "Bar");
    List<String> list2 = Arrays.asList("Baz", "Boop");
    List<List<String>> list = Arrays.asList(list1, list2);
    assertThat(conversionService.canConvert(list.getClass(), String.class)).isTrue();
    String result = conversionService.convert(list, String.class);
    assertThat(result).isEqualTo("Foo,Bar,Baz,Boop");
  }

  @Test
  public void convertCollectionToCollection() throws Exception {
    Set<String> foo = new LinkedHashSet<>();
    foo.add("1");
    foo.add("2");
    foo.add("3");
    final Field genericList = getClass().getField("genericList");
    final TypeDescriptor descriptor = TypeDescriptor.fromField(genericList);

    List<Integer> bar = conversionService.convert(foo, descriptor);
    assertThat((int) bar.get(0)).isEqualTo(1);
    assertThat((int) bar.get(1)).isEqualTo(2);
    assertThat((int) bar.get(2)).isEqualTo(3);
  }

  @Test
  public void convertCollectionToCollectionNull() throws Exception {
    final Field genericList = getClass().getField("genericList");
    final TypeDescriptor descriptor = TypeDescriptor.fromField(genericList);
    List<Integer> bar = conversionService.convert(null, descriptor);
    assertThat((Object) bar).isNull();
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void convertCollectionToCollectionNotGeneric() {
    Set<String> foo = new LinkedHashSet<>();
    foo.add("1");
    foo.add("2");
    foo.add("3");
    List bar = conversionService.convert(foo, List.class);
    assertThat(bar.get(0)).isEqualTo("1");
    assertThat(bar.get(1)).isEqualTo("2");
    assertThat(bar.get(2)).isEqualTo("3");
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void convertCollectionToCollectionSpecialCaseSourceImpl() throws Exception {
    Map map = new LinkedHashMap();
    map.put("1", "1");
    map.put("2", "2");
    map.put("3", "3");
    Collection values = map.values();
    final TypeDescriptor targetType = TypeDescriptor.fromField(getClass().getField("genericList"));
    List<Integer> bar = conversionService.convert(values, targetType);
    assertThat(bar.size()).isEqualTo(3);
    assertThat((int) bar.get(0)).isEqualTo((int) Integer.valueOf(1));
    assertThat((int) bar.get(1)).isEqualTo((int) Integer.valueOf(2));
    assertThat((int) bar.get(2)).isEqualTo((int) Integer.valueOf(3));
  }

  @Test
  public void collection() {
    List<String> strings = new ArrayList<>();
    strings.add("3");
    strings.add("9");

    final TypeDescriptor targetType = TypeDescriptor.collection(List.class, Integer.class);
    List<Integer> integers = conversionService.convert(strings, targetType);

    assertThat((int) integers.get(0)).isEqualTo((int) Integer.valueOf(3));
    assertThat((int) integers.get(1)).isEqualTo((int) Integer.valueOf(9));
  }

  @Test
  public void convertMapToMap() throws Exception {
    Map<String, String> foo = new HashMap<>();
    foo.put("1", "BAR");
    foo.put("2", "BAZ");

    final TypeDescriptor descriptor = TypeDescriptor.fromField(getClass().getField("genericMap"));
    Map<Integer, Foo> map = conversionService.convert(foo, descriptor);

    assertThat(map.get(1)).isEqualTo(Foo.BAR);
    assertThat(map.get(2)).isEqualTo(Foo.BAZ);
  }

  @Test
  public void convertHashMapValuesToList() {
    Map<String, Integer> hashMap = new LinkedHashMap<>();
    hashMap.put("1", 1);
    hashMap.put("2", 2);
    List<?> converted = conversionService.convert(hashMap.values(), List.class);
    assertThat(converted).isEqualTo(Arrays.asList(1, 2));
  }

  @Test
  public void map() {
    Map<String, String> strings = new HashMap<>();
    strings.put("3", "9");
    strings.put("6", "31");
    Map<Integer, Integer> integers = //
//            conversionService.convert(strings, new TypeReference<Map<Integer, Integer>>(){}.getTypeParameter());
            conversionService.convert(strings, TypeDescriptor.map(Map.class, Integer.class, Integer.class));

    assertThat((int) integers.get(3)).isEqualTo((int) Integer.valueOf(9));
    assertThat((int) integers.get(6)).isEqualTo((int) Integer.valueOf(31));
  }

  @Test
  public void convertPropertiesToString() {
    Properties foo = new Properties();
    foo.setProperty("1", "BAR");
    foo.setProperty("2", "BAZ");
    String result = conversionService.convert(foo, String.class);
    assertThat(result.contains("1=BAR")).isTrue();
    assertThat(result.contains("2=BAZ")).isTrue();
  }

  @Test
  public void convertStringToProperties() {
    Properties result = conversionService.convert("a=b\nc=2\nd=", Properties.class);
    assertThat(result.size()).isEqualTo(3);
    assertThat(result.getProperty("a")).isEqualTo("b");
    assertThat(result.getProperty("c")).isEqualTo("2");
    assertThat(result.getProperty("d")).isEqualTo("");
  }

  @Test
  public void convertStringToPropertiesWithSpaces() {
    Properties result = conversionService.convert("   foo=bar\n   bar=baz\n    baz=boop", Properties.class);
    assertThat(result.get("foo")).isEqualTo("bar");
    assertThat(result.get("bar")).isEqualTo("baz");
    assertThat(result.get("baz")).isEqualTo("boop");
  }

  // generic object conversion

  @Test
  public void convertObjectToStringWithValueOfMethodPresentUsingToString() {
    ISBN.reset();
    assertThat(conversionService.convert(new ISBN("123456789"), String.class)).isEqualTo("123456789");

    assertThat(ISBN.constructorCount).as("constructor invocations").isEqualTo(1);
    assertThat(ISBN.valueOfCount).as("valueOf() invocations").isEqualTo(0);
    assertThat(ISBN.toStringCount).as("toString() invocations").isEqualTo(1);
  }

  @Test
  public void convertObjectToObjectUsingValueOfMethod() {
    ISBN.reset();
    assertThat(conversionService.convert("123456789", ISBN.class)).isEqualTo(new ISBN("123456789"));

    assertThat(ISBN.valueOfCount).as("valueOf() invocations").isEqualTo(1);
    // valueOf() invokes the constructor
    assertThat(ISBN.constructorCount).as("constructor invocations").isEqualTo(2);
    assertThat(ISBN.toStringCount).as("toString() invocations").isEqualTo(0);
  }

  @Test
  public void convertObjectToStringUsingToString() {
    SSN.reset();
    assertThat(conversionService.convert(new SSN("123456789"), String.class)).isEqualTo("123456789");

    assertThat(SSN.constructorCount).as("constructor invocations").isEqualTo(1);
    assertThat(SSN.toStringCount).as("toString() invocations").isEqualTo(1);
  }

  @Test
  public void convertObjectToObjectUsingObjectConstructor() {
    SSN.reset();
    assertThat(conversionService.convert("123456789", SSN.class)).isEqualTo(new SSN("123456789"));

    assertThat(SSN.constructorCount).as("constructor invocations").isEqualTo(2);
    assertThat(SSN.toStringCount).as("toString() invocations").isEqualTo(0);
  }

  @Test
  public void convertStringToTimezone() {
    assertThat(conversionService.convert("GMT+2", TimeZone.class).getID()).isEqualTo("GMT+02:00");
  }

  @Test
  public void convertObjectToStringWithJavaTimeOfMethodPresent() {
    assertThat(conversionService.convert(ZoneId.of("GMT+1"), String.class).startsWith("GMT+")).isTrue();
  }

  @Test
  public void convertObjectToStringNotSupported() {
    assertThat(conversionService.canConvert(TestEntity.class, String.class)).isFalse();
  }

  @Test
  public void convertObjectToObjectWithJavaTimeOfMethod() {
    assertThat(conversionService.convert("GMT+1", ZoneId.class)).isEqualTo(ZoneId.of("GMT+1"));
  }

  @Test
  public void convertObjectToObjectFinderMethod() {
    TestEntity e = conversionService.convert(1L, TestEntity.class);
    assertThat(e.getId()).isEqualTo(Long.valueOf(1));
  }

  @Test
  public void convertObjectToObjectFinderMethodWithNull() {
    TestEntity entity = conversionService.convert(null/*String.class*/, TestEntity.class);
    assertThat((Object) entity).isNull();
  }

  @Test
  public void convertObjectToObjectFinderMethodWithIdConversion() {
    TestEntity entity = conversionService.convert("1", TestEntity.class);
    assertThat(entity.getId()).isEqualTo(Long.valueOf(1));
  }

  @Test
  public void convertCharArrayToString() {
    String converted = conversionService.convert(new char[] { 'a', 'b', 'c' }, String.class);
    assertThat(converted).isEqualTo("a,b,c");
  }

  @Test
  public void convertStringToCharArray() {
    char[] converted = conversionService.convert("a,b,c", char[].class);
    assertThat(converted).isEqualTo(new char[] { 'a', 'b', 'c' });
  }

  @Test
  public void convertStringToCustomCharArray() {

    final StopWatch stopWatch = new StopWatch();
    conversionService.addConverter(char[].class, String.class, String::toCharArray);
    stopWatch.start();
    char[] converted = conversionService.convert("abc", char[].class);
    stopWatch.stop();
    assertThat(converted).isEqualTo(new char[] { 'a', 'b', 'c' });

    System.out.println(stopWatch.getTotalTimeMillis());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void multidimensionalArrayToListConversionShouldConvertEntriesCorrectly() {
    String[][] grid = new String[][] {
            new String[] { "1", "2", "3", "4" },
            new String[] { "5", "6", "7", "8" },
            new String[] { "9", "10", "11", "12" }
    };
    List<String[]> converted = conversionService.convert(grid, List.class);
    String[][] convertedBack = conversionService.convert(converted, String[][].class);
    assertThat(convertedBack).isEqualTo(grid);
  }

  //  @Test
  public void convertCannotOptimizeArray() {
    conversionService.addConverter(Byte.class, Byte.class, source -> (byte) (source + 1));
    byte[] byteArray = new byte[] { 1, 2, 3 };
    byte[] converted = conversionService.convert(byteArray, byte[].class);
    assertThat(converted).isNotSameAs(byteArray);
    assertThat(converted).isEqualTo(new byte[] { 2, 3, 4 });
  }

  @Test
  @SuppressWarnings("unchecked")
  public void convertObjectToOptional() {
    Method method = ReflectionUtils.findMethod(TestEntity.class, "handleOptionalValue", Optional.class);
    final TypeDescriptor targetType = TypeDescriptor.forParameter(method, 0);
    Object actual = conversionService.convert("1,2,3", targetType);

    assertThat(actual.getClass()).isEqualTo(Optional.class);
    assertThat(((Optional<List<Integer>>) actual).get()).isEqualTo(Arrays.asList(1, 2, 3));
  }

  @Test
  public void convertObjectToOptionalNull() {
    assertThat(conversionService.convert(null, Optional.class)).isSameAs(Optional.empty());
    assertThat((Object) conversionService.convert(null, Optional.class)).isSameAs(Optional.empty());
  }

  @Test
  public void convertExistingOptional() {
    assertThat(conversionService.convert(Optional.empty(), Optional.class)).isSameAs(Optional.empty());
    assertThat((Object) conversionService.convert(Optional.empty(), Optional.class)).isSameAs(Optional.empty());
  }

  // test fields and helpers

  public List<Integer> genericList = new ArrayList<>();

  public Stream<Integer> genericStream;

  public Map<Integer, Foo> genericMap = new HashMap<>();

  public EnumSet<Foo> enumSet;

  public Object assignableTarget;

  public void handlerMethod(List<Color> color) {
  }

  public enum Foo {

    BAR, BAZ
  }

  public enum SubFoo {

    BAR {
      @Override
      String s() {
        return "x";
      }
    },
    BAZ {
      @Override
      String s() {
        return "y";
      }
    };

    abstract String s();
  }

  public class ColorConverter implements Converter<String, Color> {

    @Override
    public Color convert(String source) {
      if (!source.startsWith("#")) {
        source = "#" + source;
      }
      return Color.decode(source);
    }
  }

  @SuppressWarnings("serial")
  public static class CustomNumber extends Number {

    @Override
    public double doubleValue() {
      return 0;
    }

    @Override
    public float floatValue() {
      return 0;
    }

    @Override
    public int intValue() {
      return 0;
    }

    @Override
    public long longValue() {
      return 0;
    }
  }

  public static class TestEntity {

    private Long id;

    public TestEntity(Long id) {
      this.id = id;
    }

    public Long getId() {
      return id;
    }

    public static TestEntity findTestEntity(Long id) {
      return new TestEntity(id);
    }

    public void handleOptionalValue(Optional<List<Integer>> value) { }
  }

  private static class ListWrapper {

    private List<?> list;

    public ListWrapper(List<?> list) {
      this.list = list;
    }

    public List<?> getList() {
      return list;
    }
  }

  private static class SSN {

    static int constructorCount = 0;

    static int toStringCount = 0;

    static void reset() {
      constructorCount = 0;
      toStringCount = 0;
    }

    private final String value;

    public SSN(String value) {
      constructorCount++;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof SSN)) {
        return false;
      }
      SSN ssn = (SSN) o;
      return this.value.equals(ssn.value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public String toString() {
      toStringCount++;
      return value;
    }
  }

  private static class ISBN {

    static int constructorCount = 0;
    static int toStringCount = 0;
    static int valueOfCount = 0;

    static void reset() {
      constructorCount = 0;
      toStringCount = 0;
      valueOfCount = 0;
    }

    private final String value;

    public ISBN(String value) {
      constructorCount++;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ISBN)) {
        return false;
      }
      ISBN isbn = (ISBN) o;
      return this.value.equals(isbn.value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public String toString() {
      toStringCount++;
      return value;
    }

    @SuppressWarnings("unused")
    public static ISBN valueOf(String value) {
      valueOfCount++;
      return new ISBN(value);
    }
  }

}
