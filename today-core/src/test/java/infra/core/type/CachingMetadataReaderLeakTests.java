/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.core.type;

import org.junit.jupiter.api.Test;

import java.net.URL;

import infra.core.io.Resource;
import infra.core.io.UrlResource;
import infra.core.testfixture.EnabledForTestGroups;
import infra.core.type.classreading.CachingMetadataReaderFactory;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;

import static infra.core.testfixture.TestGroup.LONG_RUNNING;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for checking the behaviour of {@link CachingMetadataReaderFactory} under
 * load. If the cache is not controlled, this test should fail with an out of memory
 * exception around entry 5k.
 *
 * @author Costin Leau
 * @author Sam Brannen
 */
@EnabledForTestGroups(LONG_RUNNING)
class CachingMetadataReaderLeakTests {

  private static final int ITEMS_TO_LOAD = 9999;

  private final MetadataReaderFactory mrf = new CachingMetadataReaderFactory();

  @Test
  void significantLoad() throws Exception {
    // the biggest public class in the JDK (>60k)
    URL url = getClass().getResource("/java/awt/Component.class");
    assertThat(url).isNotNull();

    // look at a LOT of items
    for (int i = 0; i < ITEMS_TO_LOAD; i++) {
      Resource resource = new UrlResource(url) {

        @Override
        public boolean equals(Object obj) {
          return (obj == this);
        }

        @Override
        public int hashCode() {
          return System.identityHashCode(this);
        }
      };

      MetadataReader reader = mrf.getMetadataReader(resource);
      assertThat(reader).isNotNull();
    }

    // useful for profiling to take snapshots
    // System.in.read();
  }

}
