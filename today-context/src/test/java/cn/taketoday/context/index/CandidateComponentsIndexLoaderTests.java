/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.index;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import cn.taketoday.core.io.ClassPathResource;

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
    Set<String> components = index.getCandidateTypes("cn.taketoday", "foo");
    assertThat(components).contains(
            "cn.taketoday.context.index.Sample1",
            "cn.taketoday.context.index.Sample2");
  }

  @Test
  public void loadIndexSingleMatch() {
    CandidateComponentsIndex index = CandidateComponentsIndexLoader.loadIndex(
            CandidateComponentsTestClassLoader.index(getClass().getClassLoader(),
                    new ClassPathResource("today.components", getClass())));
    Set<String> components = index.getCandidateTypes("cn.taketoday", "biz");
    assertThat(components).contains(
            "cn.taketoday.context.index.Sample3");
  }

  @Test
  public void loadIndexNoMatch() {
    CandidateComponentsIndex index = CandidateComponentsIndexLoader.loadIndex(
            CandidateComponentsTestClassLoader.index(getClass().getClassLoader(),
                    new ClassPathResource("today.components", getClass())));
    Set<String> components = index.getCandidateTypes("cn.taketoday", "none");
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
  public void loadIndexNoSpringComponentsResource() {
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
