/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.properties.bind;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.beans.PropertyEditorSupport;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.beans.PropertyEditorRegistry;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConverterNotFoundException;
import cn.taketoday.core.conversion.GenericConverter;
import cn.taketoday.core.conversion.support.GenericConversionService;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link BindConverter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@ExtendWith(MockitoExtension.class)
class BindConverterTests {

  @Mock
  private Consumer<PropertyEditorRegistry> propertyEditorInitializer;

  @Test
  void createWhenPropertyEditorInitializerIsNullShouldCreate() {
    BindConverter.get(null, null);
  }

  @Test
  void createWhenPropertyEditorInitializerIsNotNullShouldUseToInitialize() {
    BindConverter.get(null, this.propertyEditorInitializer);
    then(this.propertyEditorInitializer).should().accept(any(PropertyEditorRegistry.class));
  }

  @Test
  void canConvertWhenHasDefaultEditorShouldReturnTrue() {
    BindConverter bindConverter = getPropertyEditorOnlyBindConverter(null);
    assertThat(bindConverter.canConvert("java.lang.RuntimeException", ResolvableType.forClass(Class.class)))
            .isTrue();
  }

  @Test
  void canConvertWhenHasCustomEditorShouldReturnTrue() {
    BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
    assertThat(bindConverter.canConvert("test", ResolvableType.forClass(SampleType.class))).isTrue();
  }

  @Test
  void canConvertWhenHasEditorByConventionShouldReturnTrue() {
    BindConverter bindConverter = getPropertyEditorOnlyBindConverter(null);
    assertThat(bindConverter.canConvert("test", ResolvableType.forClass(ConventionType.class))).isTrue();
  }

  @Test
  void canConvertWhenHasEditorForCollectionElementShouldReturnTrue() {
    BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
    assertThat(bindConverter.canConvert("test", ResolvableType.forClassWithGenerics(List.class, SampleType.class)))
            .isTrue();
  }

  @Test
  void canConvertWhenHasEditorForArrayElementShouldReturnTrue() {
    BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
    assertThat(bindConverter.canConvert("test", ResolvableType.forClass(SampleType[].class))).isTrue();
  }

  @Test
  void canConvertWhenConversionServiceCanConvertShouldReturnTrue() {
    BindConverter bindConverter = getBindConverter(new SampleTypeConverter());
    assertThat(bindConverter.canConvert("test", ResolvableType.forClass(SampleType.class))).isTrue();
  }

  @Test
  void canConvertWhenNotPropertyEditorAndConversionServiceCannotConvertShouldReturnFalse() {
    BindConverter bindConverter = BindConverter.get(null, null);
    assertThat(bindConverter.canConvert("test", ResolvableType.forClass(SampleType.class))).isFalse();
  }

  @Test
  void convertWhenHasDefaultEditorShouldConvert() {
    BindConverter bindConverter = getPropertyEditorOnlyBindConverter(null);
    Class<?> converted = bindConverter.convert("java.lang.RuntimeException", ResolvableType.forClass(Class.class));
    assertThat(converted).isEqualTo(RuntimeException.class);
  }

  @Test
  void convertWhenHasCustomEditorShouldConvert() {
    BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
    SampleType converted = bindConverter.convert("test", ResolvableType.forClass(SampleType.class));
    assertThat(converted.getText()).isEqualTo("test");
  }

  @Test
  void convertWhenHasEditorByConventionShouldConvert() {
    BindConverter bindConverter = getPropertyEditorOnlyBindConverter(null);
    ConventionType converted = bindConverter.convert("test", ResolvableType.forClass(ConventionType.class));
    assertThat(converted.getText()).isEqualTo("test");
  }

  @Test
  void convertWhenHasEditorForCollectionElementShouldConvert() {
    BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
    List<SampleType> converted = bindConverter.convert("test",
            ResolvableType.forClassWithGenerics(List.class, SampleType.class));
    assertThat(converted).hasSize(1);
    assertThat(converted.get(0).getText()).isEqualTo("test");
  }

  @Test
  void convertWhenHasEditorForArrayElementShouldConvert() {
    BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
    SampleType[] converted = bindConverter.convert("test", ResolvableType.forClass(SampleType[].class));
    assertThat(converted).isNotEmpty();
    assertThat(converted[0].getText()).isEqualTo("test");
  }

  @Test
  void convertWhenConversionServiceCanConvertShouldConvert() {
    BindConverter bindConverter = getBindConverter(new SampleTypeConverter());
    SampleType converted = bindConverter.convert("test", ResolvableType.forClass(SampleType.class));
    assertThat(converted.getText()).isEqualTo("test");
  }

  @Test
  void convertWhenNotPropertyEditorAndConversionServiceCannotConvertShouldThrowException() {
    BindConverter bindConverter = BindConverter.get(null, null);
    assertThatExceptionOfType(ConverterNotFoundException.class)
            .isThrownBy(() -> bindConverter.convert("test", ResolvableType.forClass(SampleType.class)));
  }

  @Test
  void convertWhenConvertingToFileShouldExcludeFileEditor() {
    // For back compatibility we want true file conversion and not an accidental
    // classpath resource reference. See gh-12163
    BindConverter bindConverter = BindConverter.get(Collections.singletonList(new GenericConversionService()),
            null);
    File result = bindConverter.convert(".", ResolvableType.forClass(File.class));
    assertThat(result.getPath()).isEqualTo(".");
  }

  @Test
  void fallsBackToApplicationConversionService() {
    BindConverter bindConverter = BindConverter.get(Collections.singletonList(new GenericConversionService()),
            null);
    Duration result = bindConverter.convert("10s", ResolvableType.forClass(Duration.class));
    assertThat(result.getSeconds()).isEqualTo(10);
  }

  @Test
    // gh-27028
  void convertWhenConversionFailsThrowsConversionFailedExceptionRatherThanConverterNotFoundException() {
    BindConverter bindConverter = BindConverter.get(Collections.singletonList(new GenericConversionService()),
            null);
    assertThatExceptionOfType(ConversionFailedException.class)
            .isThrownBy(() -> bindConverter.convert("com.example.Missing", ResolvableType.forClass(Class.class)))
            .withRootCauseInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void convertWhenUsingTypeConverterConversionServiceFromMultipleThreads() {
    BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
    ResolvableType type = ResolvableType.forClass(SampleType.class);
    List<Thread> threads = new ArrayList<>();
    List<SampleType> results = Collections.synchronizedList(new ArrayList<>());
    for (int i = 0; i < 40; i++) {
      threads.add(new Thread(() -> {
        for (int j = 0; j < 20; j++) {
          results.add(bindConverter.convert("test", type));
        }
      }));
    }
    threads.forEach(Thread::start);
    for (Thread thread : threads) {
      try {
        thread.join();
      }
      catch (InterruptedException ex) {
      }
    }
    assertThat(results).isNotEmpty().doesNotContainNull();
  }

  private BindConverter getPropertyEditorOnlyBindConverter(
          Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
    return BindConverter.get(Collections.singletonList(new ThrowingConversionService()), propertyEditorInitializer);
  }

  private BindConverter getBindConverter(Converter<?, ?> converter) {
    GenericConversionService conversionService = new GenericConversionService();
    conversionService.addConverter(converter);
    return BindConverter.get(Collections.singletonList(conversionService), null);
  }

  private void registerSampleTypeEditor(PropertyEditorRegistry registry) {
    registry.registerCustomEditor(SampleType.class, new SampleTypePropertyEditor());
  }

  static class SampleType {

    private String text;

    String getText() {
      return this.text;
    }

  }

  static class SampleTypePropertyEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
      setValue(null);
      if (text != null) {
        SampleType value = new SampleType();
        value.text = text;
        setValue(value);
      }
    }

  }

  static class SampleTypeConverter implements Converter<String, SampleType> {

    @Override
    public SampleType convert(String source) {
      SampleType result = new SampleType();
      result.text = source;
      return result;
    }

  }

  static class ConventionType {

    private String text;

    String getText() {
      return this.text;
    }

  }

  static class ConventionTypeEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
      ConventionType value = new ConventionType();
      value.text = text;
      setValue(value);
    }

  }

  /**
   * {@link ConversionService} that always throws an {@link AssertionError}.
   */
  static class ThrowingConversionService implements ConversionService {

    @Nullable
    @Override
    public GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
      throw new AssertionError("Should not call conversion service");
    }

    @Override
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
      throw new AssertionError("Should not call conversion service");
    }

    @Override
    public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
      throw new AssertionError("Should not call conversion service");
    }

    @Override
    public <T> T convert(Object source, Class<T> targetType) {
      throw new AssertionError("Should not call conversion service");
    }

    @Nullable
    @Override
    public <T> T convert(@Nullable Object source, TypeDescriptor targetType) {
      throw new AssertionError("Should not call conversion service");
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      throw new AssertionError("Should not call conversion service");
    }

  }

}
