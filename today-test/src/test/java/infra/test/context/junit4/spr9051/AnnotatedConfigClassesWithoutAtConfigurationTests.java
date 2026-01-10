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

package infra.test.context.junit4.spr9051;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.JUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This set of tests refutes the claims made in
 * .
 *
 * <p><b>The Claims</b>:
 *
 * <blockquote>
 * When a {@code @ContextConfiguration} test class references a config class
 * missing an {@code @Configuration} annotation, {@code @Bean} dependencies are
 * wired successfully but the bean lifecycle is not applied (no init methods are
 * invoked, for example). Adding the missing {@code @Configuration} annotation
 * solves the problem, however the problem and solution isn't obvious since
 * wiring/injection appeared to work.
 * </blockquote>
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration(classes = AnnotatedConfigClassesWithoutAtConfigurationTests.AnnotatedFactoryBeans.class)
public class AnnotatedConfigClassesWithoutAtConfigurationTests {

  /**
   * This is intentionally <b>not</b> annotated with {@code @Configuration}.
   * Consequently, this class contains what we call <i>annotated factory bean
   * methods</i> instead of standard bean definition methods.
   */
  static class AnnotatedFactoryBeans {

    static final AtomicInteger enigmaCallCount = new AtomicInteger();

    @Bean
    public String enigma() {
      return "enigma #" + enigmaCallCount.incrementAndGet();
    }

    @Bean
    public LifecycleBean lifecycleBean() {
      // The following call to enigma() literally invokes the local
      // enigma() method, not a CGLIB proxied version, since these methods
      // are essentially factory bean methods.
      LifecycleBean bean = new LifecycleBean(enigma());
      assertThat(bean.isInitialized()).isFalse();
      return bean;
    }
  }

  @Autowired
  private String enigma;

  @Autowired
  private LifecycleBean lifecycleBean;

  @Test
  public void testSPR_9051() throws Exception {
    assertThat(enigma).isNotNull();
    assertThat(lifecycleBean).isNotNull();
    assertThat(lifecycleBean.isInitialized()).isTrue();
    Set<String> names = new HashSet<>();
    names.add(enigma.toString());
    names.add(lifecycleBean.getName());
    assertThat(new HashSet<>(Arrays.asList("enigma #1", "enigma #2"))).isEqualTo(names);
  }
}
