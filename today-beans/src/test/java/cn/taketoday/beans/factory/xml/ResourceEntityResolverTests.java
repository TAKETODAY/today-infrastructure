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

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.xml.sax.InputSource;

import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/18 22:03
 */
class ResourceEntityResolverTests {

  @ParameterizedTest
  @ValueSource(strings = { "https://example.org/schema/", "https://example.org/schema.xml" })
  void resolveEntityDoesNotCallFallbackIfNotSchema(String systemId) throws Exception {
    ConfigurableFallbackEntityResolver resolver = new ConfigurableFallbackEntityResolver(true);

    assertThat(resolver.resolveEntity("testPublicId", systemId)).isNull();
    assertThat(resolver.fallbackInvoked).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = { "https://example.org/schema.dtd", "https://example.org/schema.xsd" })
  void resolveEntityCallsFallbackThatReturnsNull(String systemId) throws Exception {
    ConfigurableFallbackEntityResolver resolver = new ConfigurableFallbackEntityResolver(null);

    assertThat(resolver.resolveEntity("testPublicId", systemId)).isNull();
    assertThat(resolver.fallbackInvoked).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = { "https://example.org/schema.dtd", "https://example.org/schema.xsd" })
  void resolveEntityCallsFallbackThatThrowsException(String systemId) {
    ConfigurableFallbackEntityResolver resolver = new ConfigurableFallbackEntityResolver(true);

    assertThatExceptionOfType(ResolutionRejectedException.class)
            .isThrownBy(() -> resolver.resolveEntity("testPublicId", systemId));
    assertThat(resolver.fallbackInvoked).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = { "https://example.org/schema.dtd", "https://example.org/schema.xsd" })
  void resolveEntityCallsFallbackThatReturnsInputSource(String systemId) throws Exception {
    InputSource expected = Mockito.mock(InputSource.class);
    ConfigurableFallbackEntityResolver resolver = new ConfigurableFallbackEntityResolver(expected);

    assertThat(resolver.resolveEntity("testPublicId", systemId)).isSameAs(expected);
    assertThat(resolver.fallbackInvoked).isTrue();
  }

  private static final class NoOpResourceLoader implements ResourceLoader {

    @Override
    public Resource getResource(String location) {
      return null;
    }

    @Override
    public ClassLoader getClassLoader() {
      return ResourceEntityResolverTests.class.getClassLoader();
    }
  }

  private static class ConfigurableFallbackEntityResolver extends ResourceEntityResolver {

    private final boolean shouldThrow;

    @Nullable
    private final InputSource returnValue;

    boolean fallbackInvoked = false;

    private ConfigurableFallbackEntityResolver(boolean shouldThrow) {
      super(new NoOpResourceLoader());
      this.shouldThrow = shouldThrow;
      this.returnValue = null;
    }

    private ConfigurableFallbackEntityResolver(@Nullable InputSource returnValue) {
      super(new NoOpResourceLoader());
      this.shouldThrow = false;
      this.returnValue = returnValue;
    }

    @Nullable
    @Override
    protected InputSource resolveSchemaEntity(String publicId, String systemId) {
      this.fallbackInvoked = true;
      if (this.shouldThrow) {
        throw new ResolutionRejectedException();
      }
      return this.returnValue;
    }
  }

  @SuppressWarnings("serial")
  static class ResolutionRejectedException extends RuntimeException { }

}