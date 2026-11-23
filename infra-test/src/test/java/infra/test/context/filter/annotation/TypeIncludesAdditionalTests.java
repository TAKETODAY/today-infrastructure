/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.context.filter.annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TypeIncludesAdditionalTests {

  @TempDir
  Path directory;

  @Test
  void loadsUniqueTypesAndIgnoresCommentsAndUnavailableClasses() throws Exception {
    Path includes = directory.resolve("types.includes");
    Files.writeString(includes, "# comment\n\njava.lang.String # inline\njava.lang.Integer\njava.lang.String\nnot.present.Type\n");
    TypeIncludes types = TypeIncludes.load(Test.class, loader(includes.toUri().toURL()));
    assertThat(types).containsExactlyInAnyOrder(String.class, Integer.class);
    assertThat(types.getIncludes()).containsExactlyInAnyOrder(String.class, Integer.class);
    assertThat(TypeIncludes.load(getClass(), null)).isEmpty();
  }

  @Test
  void resourceEnumerationAndReadingFailuresIncludeLocation() throws Exception {
    ClassLoader broken = new ClassLoader() {
      @Override
      public Enumeration<URL> getResources(String name) throws IOException {
        throw new IOException("enumeration failed");
      }
    };
    assertThatIllegalArgumentException().isThrownBy(() -> TypeIncludes.load(Test.class, broken))
            .withMessageContaining("Failed to load includes").withCauseInstanceOf(IOException.class);
    ClassLoader missing = loader(directory.resolve("missing.includes").toUri().toURL());
    assertThatIllegalArgumentException().isThrownBy(() -> TypeIncludes.load(Test.class, missing))
            .withMessageContaining("Unable to load includes").withCauseInstanceOf(IOException.class);
  }

  private ClassLoader loader(URL url) {
    return new ClassLoader(getClass().getClassLoader()) {
      @Override
      public Enumeration<URL> getResources(String name) {
        return Collections.enumeration(Collections.singleton(url));
      }
    };
  }

}
