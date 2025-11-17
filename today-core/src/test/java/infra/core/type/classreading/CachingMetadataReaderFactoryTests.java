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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/6/29 20:49
 */
class CachingMetadataReaderFactoryTests {

  @Test
  void shouldCacheClassNameCalls() throws Exception {
    MetadataReaderFactory delegate = mock(MetadataReaderFactory.class);
    when(delegate.getMetadataReader(any(Resource.class))).thenReturn(mock(MetadataReader.class));

    CachingMetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(delegate);
    readerFactory.getMetadataReader(TestClass.class.getName());
    readerFactory.getMetadataReader(TestClass.class.getName());

    verify(delegate, times(1)).getMetadataReader(any(Resource.class));
  }

  public static class TestClass {
  }

}