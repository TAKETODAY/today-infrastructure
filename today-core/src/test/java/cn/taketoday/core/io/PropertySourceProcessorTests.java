/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

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
    PropertySourceDescriptor descriptor = new PropertySourceDescriptor(List.of(PROPS_FILE), false, null, DefaultPropertySourceFactory.class, null);
    processor.processPropertySource(descriptor);
    assertThat(environment.getPropertySources()).hasSize(3);
    assertThat(environment.getProperty("enigma")).isEqualTo("42");
  }

  @Nested
  class FailOnErrorTests {

    @Test
    void processorFailsOnIllegalArgumentException() {
      assertProcessorFailsOnError(IllegalArgumentExceptionPropertySourceFactory.class, IllegalArgumentException.class);
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
      assertProcessorIgnoresFailure(IllegalArgumentExceptionPropertySourceFactory.class);
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

  private static class IllegalArgumentExceptionPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
      throw new IllegalArgumentException("bogus");
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