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

package infra.core.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import infra.core.env.CompositePropertySource;
import infra.core.env.PropertySource;
import infra.core.env.StandardEnvironment;
import infra.util.ClassUtils;
import infra.util.PlaceholderResolutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/12 00:34
 */
class PropertySourceProcessorTests {

  private static final String PROPS_FILE = ClassUtils.classPackageAsResourcePath(PropertySourceProcessorTests.class) + "/test.properties";

  private final StandardEnvironment environment = new StandardEnvironment();

  private final ResourceLoader resourceLoader = new DefaultResourceLoader();

  private final PropertySourceProcessor processor = new PropertySourceProcessor(environment, resourceLoader);

  @BeforeEach
  void checkInitialPropertySources() {
    assertThat(environment.getPropertySources()).hasSize(2);
  }

  @Test
  void processorRegistersPropertySource() throws Exception {
    PropertySourceDescriptor descriptor = new PropertySourceDescriptor(
            List.of(PROPS_FILE), false, null, DefaultPropertySourceFactory.class, null);
    processor.processPropertySource(descriptor);
    assertThat(environment.getPropertySources()).hasSize(3);
    assertThat(environment.getProperty("enigma")).isEqualTo("42");
  }

  @Test
  void processPropertySourceWithWildcardLocation() throws Exception {
    PropertySourceDescriptor descriptor = new PropertySourceDescriptor(
            List.of("classpath*:infra/core/io/*.properties"),
            false, null, DefaultPropertySourceFactory.class, null);
    processor.processPropertySource(descriptor);
    assertThat(environment.getPropertySources()).hasSize(8);
  }

  @Test
  void processPropertySourceWithUnresolvedPlaceholder() {
    PropertySourceDescriptor descriptor = new PropertySourceDescriptor(
            List.of("${unresolved.placeholder}/test.properties"),
            false, null, DefaultPropertySourceFactory.class, null);
    assertThatExceptionOfType(PlaceholderResolutionException.class)
            .isThrownBy(() -> processor.processPropertySource(descriptor));
  }

  @Test
  void processPropertySourceWithCustomEncoding() throws Exception {
    PropertySourceDescriptor descriptor = new PropertySourceDescriptor(
            List.of(PROPS_FILE), false, "UTF-16", DefaultPropertySourceFactory.class, null);
    processor.processPropertySource(descriptor);
    assertThat(environment.getPropertySources()).hasSize(3);
  }

  @Test
  void processPropertySourceWithSameNameAppendsToExisting() throws Exception {
    PropertySourceDescriptor first = new PropertySourceDescriptor(
            List.of(PROPS_FILE), false, null, DefaultPropertySourceFactory.class, "UTF-8");
    PropertySourceDescriptor second = new PropertySourceDescriptor(
            List.of(PROPS_FILE), false, null, DefaultPropertySourceFactory.class, "UTF-8");
    processor.processPropertySource(first);
    processor.processPropertySource(second);
    assertThat(environment.getPropertySources()).hasSize(3);
    List<PropertySource<?>> list = environment.getPropertySources().stream().toList();
    assertThat(list.get(2)).isInstanceOf(CompositePropertySource.class);
  }

  @Test
  void processPropertySourceWithEmptyLocations() {
    PropertySourceDescriptor descriptor = new PropertySourceDescriptor(
            List.of(), false, null, DefaultPropertySourceFactory.class, null);
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> processor.processPropertySource(descriptor));
  }

  @Test
  void processPropertySourceWithInvalidCustomFactory() {
    PropertySourceDescriptor descriptor = new PropertySourceDescriptor(
            List.of(PROPS_FILE), false, null, InvalidPropertySourceFactory.class, null);
    assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> processor.processPropertySource(descriptor));
  }

  private static class InvalidPropertySourceFactory implements PropertySourceFactory {
    private InvalidPropertySourceFactory(String invalid) { }

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) {
      return null;
    }
  }

  @Nested
  class FailOnErrorTests {

    @Test
    void processorFailsOnPlaceholderResolutionException() {
      assertProcessorFailsOnError(PlaceholderResolutionExceptionPropertySourceFactory.class, PlaceholderResolutionException.class);
    }

    @Test
    void processorFailsOnFileNotFoundException() {
      assertProcessorFailsOnError(FileNotFoundExceptionPropertySourceFactory.class, FileNotFoundException.class);
    }

    private void assertProcessorFailsOnError(
            Class<? extends PropertySourceFactory> factoryClass, Class<? extends Throwable> exceptionType) {

      PropertySourceDescriptor descriptor =
              new PropertySourceDescriptor(List.of(PROPS_FILE), false, null, factoryClass, null);
      assertThatExceptionOfType(exceptionType).isThrownBy(() -> processor.processPropertySource(descriptor));
      assertThat(environment.getPropertySources()).hasSize(2);
    }
  }

  @Nested
  class IgnoreResourceNotFoundTests {

    @Test
    void processorIgnoresIllegalArgumentException() {
      assertProcessorIgnoresFailure(PlaceholderResolutionExceptionPropertySourceFactory.class);
    }

    @Test
    void processorIgnoresFileNotFoundException() {
      assertProcessorIgnoresFailure(FileNotFoundExceptionPropertySourceFactory.class);
    }

    @Test
    void processorIgnoresUnknownHostException() {
      assertProcessorIgnoresFailure(UnknownHostExceptionPropertySourceFactory.class);
    }

    @Test
    void processorIgnoresSocketException() {
      assertProcessorIgnoresFailure(SocketExceptionPropertySourceFactory.class);
    }

    @Test
    void processorIgnoresSupportedExceptionWrappedInIllegalStateException() {
      assertProcessorIgnoresFailure(WrappedIOExceptionPropertySourceFactory.class);
    }

    @Test
    void processorIgnoresSupportedExceptionWrappedInUncheckedIOException() {
      assertProcessorIgnoresFailure(UncheckedIOExceptionPropertySourceFactory.class);
    }

    private void assertProcessorIgnoresFailure(Class<? extends PropertySourceFactory> factoryClass) {
      PropertySourceDescriptor descriptor = new PropertySourceDescriptor(List.of(PROPS_FILE), true, null, factoryClass, null);
      assertThatNoException().isThrownBy(() -> processor.processPropertySource(descriptor));
      assertThat(environment.getPropertySources()).hasSize(2);
    }
  }

  private static class PlaceholderResolutionExceptionPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) {
      throw mock(PlaceholderResolutionException.class);
    }
  }

  private static class FileNotFoundExceptionPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
      throw new FileNotFoundException("bogus");
    }
  }

  private static class UnknownHostExceptionPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
      throw new UnknownHostException("bogus");
    }
  }

  private static class SocketExceptionPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
      throw new SocketException("bogus");
    }
  }

  private static class WrappedIOExceptionPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) {
      throw new IllegalStateException("Wrapped", new FileNotFoundException("bogus"));
    }
  }

  private static class UncheckedIOExceptionPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) {
      throw new UncheckedIOException("Wrapped", new FileNotFoundException("bogus"));
    }
  }

}