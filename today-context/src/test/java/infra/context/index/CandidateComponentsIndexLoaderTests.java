/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.index;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import infra.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link CandidateComponentsIndexLoader}.
 *
 * @author Stephane Nicoll
 */
public class CandidateComponentsIndexLoaderTests {

  @Test
  public void validateIndexIsDisabledByDefault() {
    CandidateComponentsIndex index = CandidateComponentsIndexLoader.loadIndex(null);
    assertThat(index).as("No today.components should be available at the default location").isNull();
  }

  @Test
  public void loadIndexSeveralMatches() {
    CandidateComponentsIndex index = CandidateComponentsIndexLoader.loadIndex(
            CandidateComponentsTestClassLoader.index(getClass().getClassLoader(),
                    new ClassPathResource("today.components", getClass())));
    Set<String> components = index.getCandidateTypes("infra", "foo");
    assertThat(components).contains(
            "infra.context.index.Sample1",
            "infra.context.index.Sample2");
  }

  @Test
  public void loadIndexSingleMatch() {
    CandidateComponentsIndex index = CandidateComponentsIndexLoader.loadIndex(
            CandidateComponentsTestClassLoader.index(getClass().getClassLoader(),
                    new ClassPathResource("today.components", getClass())));
    Set<String> components = index.getCandidateTypes("infra", "biz");
    assertThat(components).contains(
            "infra.context.index.Sample3");
  }

  @Test
  public void loadIndexNoMatch() {
    CandidateComponentsIndex index = CandidateComponentsIndexLoader.loadIndex(
            CandidateComponentsTestClassLoader.index(getClass().getClassLoader(),
                    new ClassPathResource("today.components", getClass())));
    Set<String> components = index.getCandidateTypes("infra", "none");
    assertThat(components).isEmpty();
  }

  @Test
  public void loadIndexNoPackage() {
    CandidateComponentsIndex index = CandidateComponentsIndexLoader.loadIndex(
            CandidateComponentsTestClassLoader.index(getClass().getClassLoader(),
                    new ClassPathResource("today.components", getClass())));
    Set<String> components = index.getCandidateTypes("com.example", "foo");
    assertThat(components).isEmpty();
  }

  @Test
  public void loadIndexNoInfraComponentsResource() {
    CandidateComponentsIndex index = CandidateComponentsIndexLoader.loadIndex(
            CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader()));
    assertThat(index).isNull();
  }

  @Test
  public void loadIndexNoEntry() {
    CandidateComponentsIndex index = CandidateComponentsIndexLoader.loadIndex(
            CandidateComponentsTestClassLoader.index(getClass().getClassLoader(),
                    new ClassPathResource("empty-today.components", getClass())));
    assertThat(index).isNull();
  }

  @Test
  public void loadIndexWithException() {
    final IOException cause = new IOException("test exception");
    assertThatIllegalStateException().isThrownBy(() -> {
      CandidateComponentsTestClassLoader classLoader = new CandidateComponentsTestClassLoader(getClass().getClassLoader(), cause);
      CandidateComponentsIndexLoader.loadIndex(classLoader);
    }).withMessageContaining("Unable to load indexes").withCause(cause);
  }

}
