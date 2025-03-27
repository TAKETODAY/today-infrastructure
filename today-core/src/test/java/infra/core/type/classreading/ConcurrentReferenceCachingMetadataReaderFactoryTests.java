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

import java.io.IOException;

import infra.core.io.Resource;
import infra.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
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

  @Test
  void getMetadataReaderReusesCachedMetadataReaderFromResourceCache() throws IOException {
    ConcurrentReferenceCachingMetadataReaderFactory factory = spy(
            new TestConcurrentReferenceCachingMetadataReaderFactory());
    Resource resource = mock(Resource.class);
    MetadataReader reader1 = factory.getMetadataReader(resource);
    MetadataReader reader2 = factory.getMetadataReader(resource);
    assertThat(reader1).isSameAs(reader2);
    then(factory).should().createMetadataReader(resource);
  }

  @Test
  void getMetadataReaderWithDifferentResourcesCreatesDifferentReaders() throws IOException {
    ConcurrentReferenceCachingMetadataReaderFactory factory = spy(
            new TestConcurrentReferenceCachingMetadataReaderFactory());
    Resource resource1 = mock(Resource.class);
    Resource resource2 = mock(Resource.class);
    MetadataReader reader1 = factory.getMetadataReader(resource1);
    MetadataReader reader2 = factory.getMetadataReader(resource2);
    assertThat(reader1).isNotSameAs(reader2);
    then(factory).should().createMetadataReader(resource1);
    then(factory).should().createMetadataReader(resource2);
  }

  @Test
  void getMetadataReaderWithDifferentClassNamesCreatesDifferentReaders() throws IOException {
    ConcurrentReferenceCachingMetadataReaderFactory factory = spy(
            new TestConcurrentReferenceCachingMetadataReaderFactory());
    MetadataReader reader1 = factory.getMetadataReader("com.example.Class1");
    MetadataReader reader2 = factory.getMetadataReader("com.example.Class2");
    assertThat(reader1).isNotSameAs(reader2);
  }

  @Test
  void clearCacheRemovesBothResourceAndClassNameCaches() throws IOException {
    ConcurrentReferenceCachingMetadataReaderFactory factory = spy(
            new TestConcurrentReferenceCachingMetadataReaderFactory());
    Resource resource = mock(Resource.class);

    factory.getMetadataReader(resource);
    factory.getMetadataReader("com.example.Class");
    factory.clearCache();

    factory.getMetadataReader(resource);
    factory.getMetadataReader("com.example.Class");

    then(factory).should(times(2)).createMetadataReader(resource);
  }

  @Test
  void delegatesMetadataReaderCreationToProvidedFactory() throws IOException {
    ResourceLoader resourceLoader = mock(ResourceLoader.class);
    ConcurrentReferenceCachingMetadataReaderFactory factory =
            new ConcurrentReferenceCachingMetadataReaderFactory(resourceLoader);
    Resource resource = mock(Resource.class);

    factory.getMetadataReader(resource);
    factory.getMetadataReader("com.example.Class");
  }

  @Test
  void handlesClassLoaderConstructor() {
    ClassLoader classLoader = mock(ClassLoader.class);
    ConcurrentReferenceCachingMetadataReaderFactory factory =
            new ConcurrentReferenceCachingMetadataReaderFactory(classLoader);
    assertThat(factory).isNotNull();
  }

  static class TestConcurrentReferenceCachingMetadataReaderFactory
          extends ConcurrentReferenceCachingMetadataReaderFactory {

    @Override
    public MetadataReader createMetadataReader(Resource resource) {
      return mock(MetadataReader.class);
    }

  }

}