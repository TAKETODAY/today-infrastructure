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

package infra.core.type.classreading;

import org.junit.jupiter.api.Test;

import infra.core.io.Resource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/22 23:13
 */
class ConcurrentReferenceCachingMetadataReaderFactoryTests {

  @Test
  void getMetadataReaderUsesCache() throws Exception {
    TestConcurrentReferenceCachingMetadataReaderFactory factory = spy(
            new TestConcurrentReferenceCachingMetadataReaderFactory());
    MetadataReader metadataReader1 = factory.getMetadataReader(getClass().getName());
    MetadataReader metadataReader2 = factory.getMetadataReader(getClass().getName());
    assertThat(metadataReader1).isSameAs(metadataReader2);
    then(factory).should().createMetadataReader(any(Resource.class));
  }

  @Test
  void clearResetsCache() throws Exception {
    TestConcurrentReferenceCachingMetadataReaderFactory factory = spy(
            new TestConcurrentReferenceCachingMetadataReaderFactory());
    MetadataReader metadataReader1 = factory.getMetadataReader(getClass().getName());
    factory.clearCache();
    MetadataReader metadataReader2 = factory.getMetadataReader(getClass().getName());
    assertThat(metadataReader1).isNotSameAs(metadataReader2);
    then(factory).should(times(2)).createMetadataReader(any(Resource.class));
  }

  static class TestConcurrentReferenceCachingMetadataReaderFactory
          extends ConcurrentReferenceCachingMetadataReaderFactory {

    @Override
    public MetadataReader createMetadataReader(Resource resource) {
      return mock(MetadataReader.class);
    }

  }

}