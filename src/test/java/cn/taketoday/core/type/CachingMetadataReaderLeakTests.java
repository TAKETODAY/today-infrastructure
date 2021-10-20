/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.core.type;

import org.junit.jupiter.api.Test;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.UrlResource;
import cn.taketoday.core.testfixture.EnabledForTestGroups;
import cn.taketoday.core.type.classreading.CachingMetadataReaderFactory;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.core.testfixture.TestGroup.LONG_RUNNING;

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
