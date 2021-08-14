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

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConverterNotFoundException;
import cn.taketoday.core.utils.GenericDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author TODAY 2021/3/26 10:46
 * @since 3.0
 */
public class StreamConverterTests {

  private final DefaultConversionService conversionService = new DefaultConversionService();

  private final StreamConverter streamConverter = new StreamConverter(this.conversionService);

  @Before
  public void setup() {
    this.conversionService.addConverter(new CollectionToCollectionConverter(this.conversionService));
    this.conversionService.addConverter(new ArrayToCollectionConverter(this.conversionService));
    this.conversionService.addConverter(new CollectionToArrayConverter(this.conversionService));
    this.conversionService.addConverter(this.streamConverter);
  }

  @Test
  public void convertFromStreamToList() throws NoSuchFieldException {
    this.conversionService.addConverter(String.class, Number.class, new ObjectToStringConverter());

    Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
    GenericDescriptor listOfStrings = new GenericDescriptor(Types.class.getField("listOfStrings"));
    Object result = this.conversionService.convert(stream, listOfStrings);

    assertThat(result).as("Converted object must not be null").isNotNull();
    boolean condition = result instanceof List;
    assertThat(condition).as("Converted object must be a list").isTrue();
    @SuppressWarnings("unchecked")
    List<String> content = (List<String>) result;
    assertThat(content.get(0)).isEqualTo("1");
    assertThat(content.get(1)).isEqualTo("2");
    assertThat(content.get(2)).isEqualTo("3");
    assertThat(content.size()).as("Wrong number of elements").isEqualTo(3);
  }

  @Test
  public void convertFromStreamToArray() throws NoSuchFieldException {

    conversionService.addConverters(new IntegerConverter(int.class),
                                    new IntegerConverter(Integer.class),
                                    new LongConverter(long.class),
                                    new LongConverter(Long.class));

    Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
    GenericDescriptor arrayOfLongs = new GenericDescriptor(Types.class.getField("arrayOfLongs"));
    Object result = this.conversionService.convert(stream, arrayOfLongs);

    assertThat(result).as("Converted object must not be null").isNotNull();
    assertThat(result.getClass().isArray()).as("Converted object must be an array").isTrue();
    Long[] content = (Long[]) result;
    assertThat(content[0]).isEqualTo(Long.valueOf(1L));
    assertThat(content[1]).isEqualTo(Long.valueOf(2L));
    assertThat(content[2]).isEqualTo(Long.valueOf(3L));
    assertThat(content.length).as("Wrong number of elements").isEqualTo(3);
  }

  @Test
  public void convertFromStreamToRawList() throws NoSuchFieldException {
    Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
    GenericDescriptor listOfStrings = new GenericDescriptor(Types.class.getField("rawList"));
    Object result = this.conversionService.convert(stream, listOfStrings);

    assertThat(result).as("Converted object must not be null").isNotNull();
    boolean condition = result instanceof List;
    assertThat(condition).as("Converted object must be a list").isTrue();
    @SuppressWarnings("unchecked")
    List<Object> content = (List<Object>) result;
    assertThat(content.get(0)).isEqualTo(1);
    assertThat(content.get(1)).isEqualTo(2);
    assertThat(content.get(2)).isEqualTo(3);
    assertThat(content.size()).as("Wrong number of elements").isEqualTo(3);
  }

  @Test
  public void convertFromStreamToArrayNoConverter() throws NoSuchFieldException {
    Stream<Integer> stream = Arrays.asList(1, 2, 3).stream();
    GenericDescriptor arrayOfLongs = new GenericDescriptor(Types.class.getField("arrayOfLongs"));
    assertThatExceptionOfType(ConversionFailedException.class)
            .isThrownBy(() -> this.conversionService.convert(stream, arrayOfLongs))
            .withCauseInstanceOf(ConverterNotFoundException.class);
  }

  @Test
  @SuppressWarnings("resource")
  public void convertFromListToStream() throws NoSuchFieldException {

    conversionService.addConverters(new IntegerConverter(int.class),
                                    new IntegerConverter(Integer.class));

    List<String> stream = Arrays.asList("1", "2", "3");
    GenericDescriptor streamOfInteger = new GenericDescriptor(Types.class.getField("streamOfIntegers"));
    Object result = this.conversionService.convert(stream, streamOfInteger);

    assertThat(result).as("Converted object must not be null").isNotNull();
    boolean condition = result instanceof Stream;
    assertThat(condition).as("Converted object must be a stream").isTrue();
    @SuppressWarnings("unchecked")
    Stream<Integer> content = (Stream<Integer>) result;
    assertThat(content.mapToInt(x -> x).sum()).isEqualTo(6);
  }

  @Test
  @SuppressWarnings("resource")
  public void convertFromArrayToStream() throws NoSuchFieldException {
    Integer[] stream = new Integer[] { 1, 0, 1 };
    this.conversionService.addConverter(new Converter<Integer, Boolean>() {
      @Override
      public Boolean convert(Integer source) {
        return source == 1;
      }
    });
    GenericDescriptor streamOfBoolean = new GenericDescriptor(Types.class.getField("streamOfBooleans"));
    Object result = this.conversionService.convert(stream, streamOfBoolean);

    assertThat(result).as("Converted object must not be null").isNotNull();
    boolean condition = result instanceof Stream;
    assertThat(condition).as("Converted object must be a stream").isTrue();
    @SuppressWarnings("unchecked")
    Stream<Boolean> content = (Stream<Boolean>) result;
    assertThat(content.filter(x -> x).count()).isEqualTo(2);
  }

  @Test
  @SuppressWarnings("resource")
  public void convertFromListToRawStream() throws NoSuchFieldException {
    List<String> stream = Arrays.asList("1", "2", "3");
    GenericDescriptor streamOfInteger = new GenericDescriptor(Types.class.getField("rawStream"));
    Object result = this.conversionService.convert(stream, streamOfInteger);

    assertThat(result).as("Converted object must not be null").isNotNull();
    boolean condition = result instanceof Stream;
    assertThat(condition).as("Converted object must be a stream").isTrue();
    @SuppressWarnings("unchecked")
    Stream<Object> content = (Stream<Object>) result;
    StringBuilder sb = new StringBuilder();
    content.forEach(sb::append);
    assertThat(sb.toString()).isEqualTo("123");
  }

  @Test
  public void doesNotMatchIfNoStream() throws NoSuchFieldException {
    assertThat(this.streamConverter.supports(
            new GenericDescriptor(Types.class.getField("arrayOfLongs")),
            new GenericDescriptor(Types.class.getField("listOfStrings")).getType())
    ).as("Should not match non stream type").isFalse();
  }

  @Test
  public void shouldFailToConvertIfNoStream() throws NoSuchFieldException {
    GenericDescriptor targetType = new GenericDescriptor(Types.class.getField("arrayOfLongs"));
    assertThatIllegalStateException()
            .isThrownBy(() -> this.streamConverter.convert(targetType, new Object()));
  }

  @SuppressWarnings({ "rawtypes" })
  static class Types {

    public List<String> listOfStrings;

    public Long[] arrayOfLongs;

    public Stream<Integer> streamOfIntegers;

    public Stream<Boolean> streamOfBooleans;

    public Stream rawStream;

    public List rawList;
  }

}
