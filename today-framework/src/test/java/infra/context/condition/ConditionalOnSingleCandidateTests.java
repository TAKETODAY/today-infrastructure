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

package infra.context.condition;

import org.junit.jupiter.api.Test;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Fallback;
import infra.context.annotation.Primary;
import infra.context.annotation.Scope;
import infra.context.annotation.ScopedProxyMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnSingleCandidate @ConditionalOnSingleCandidate}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class ConditionalOnSingleCandidateTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void singleCandidateNoCandidate() {
    this.contextRunner.withUserConfiguration(OnBeanSingleCandidateConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean("consumer"));
  }

  @Test
  void singleCandidateOneCandidate() {
    this.contextRunner.withUserConfiguration(AlphaConfiguration.class, OnBeanSingleCandidateConfiguration.class)
            .run((context) -> {
              assertThat(context).hasBean("consumer");
              assertThat(context.getBean("consumer")).isEqualTo("alpha");
            });
  }

  @Test
  void singleCandidateOneScopedProxyCandidate() {
    this.contextRunner
            .withUserConfiguration(AlphaScopedProxyConfiguration.class, OnBeanSingleCandidateConfiguration.class)
            .run((context) -> {
              assertThat(context).hasBean("consumer");
              assertThat(context.getBean("consumer")).hasToString("alpha");
            });
  }

  @Test
  void singleCandidateInAncestorsOneCandidateInCurrent() {
    this.contextRunner.run((parent) -> this.contextRunner
            .withUserConfiguration(AlphaConfiguration.class, OnBeanSingleCandidateInAncestorsConfiguration.class)
            .withParent(parent)
            .run((child) -> assertThat(child).doesNotHaveBean("consumer")));
  }

  @Test
  void singleCandidateInAncestorsOneCandidateInParent() {
    this.contextRunner.withUserConfiguration(AlphaConfiguration.class)
            .run((parent) -> this.contextRunner
                    .withUserConfiguration(OnBeanSingleCandidateInAncestorsConfiguration.class)
                    .withParent(parent)
                    .run((child) -> {
                      assertThat(child).hasBean("consumer");
                      assertThat(child.getBean("consumer")).isEqualTo("alpha");
                    }));
  }

  @Test
  void singleCandidateInAncestorsOneCandidateInGrandparent() {
    this.contextRunner.withUserConfiguration(AlphaConfiguration.class)
            .run((grandparent) -> this.contextRunner.withParent(grandparent)
                    .run((parent) -> this.contextRunner
                            .withUserConfiguration(OnBeanSingleCandidateInAncestorsConfiguration.class)
                            .withParent(parent)
                            .run((child) -> {
                              assertThat(child).hasBean("consumer");
                              assertThat(child.getBean("consumer")).isEqualTo("alpha");
                            })));
  }

  @Test
  void singleCandidateMultipleCandidates() {
    this.contextRunner
            .withUserConfiguration(AlphaConfiguration.class, BravoConfiguration.class,
                    OnBeanSingleCandidateConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean("consumer"));
  }

  @Test
  void singleCandidateMultipleCandidatesOnePrimary() {
    this.contextRunner
            .withUserConfiguration(AlphaPrimaryConfiguration.class, BravoConfiguration.class,
                    OnBeanSingleCandidateConfiguration.class)
            .run((context) -> {
              assertThat(context).hasBean("consumer");
              assertThat(context.getBean("consumer")).isEqualTo("alpha");
            });
  }

  @Test
  void singleCandidateTwoCandidatesOneNormalOneFallback() {
    this.contextRunner
            .withUserConfiguration(AlphaFallbackConfiguration.class, BravoConfiguration.class,
                    OnBeanSingleCandidateConfiguration.class)
            .run((context) -> {
              assertThat(context).hasBean("consumer");
              assertThat(context.getBean("consumer")).isEqualTo("bravo");
            });
  }

  @Test
  void singleCandidateMultipleCandidatesMultiplePrimary() {
    this.contextRunner
            .withUserConfiguration(AlphaPrimaryConfiguration.class, BravoPrimaryConfiguration.class,
                    OnBeanSingleCandidateConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean("consumer"));
  }

  @Test
  void singleCandidateMultipleCandidatesAllFallback() {
    this.contextRunner
            .withUserConfiguration(AlphaFallbackConfiguration.class, BravoFallbackConfiguration.class,
                    OnBeanSingleCandidateConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean("consumer"));
  }

  @Test
  void invalidAnnotationTwoTypes() {
    this.contextRunner.withUserConfiguration(OnBeanSingleCandidateTwoTypesConfiguration.class).run((context) -> {
      assertThat(context).hasFailed();
      assertThat(context).getFailure()
              .hasCauseInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining(OnBeanSingleCandidateTwoTypesConfiguration.class.getName());
    });
  }

  @Test
  void invalidAnnotationNoType() {
    this.contextRunner.withUserConfiguration(OnBeanSingleCandidateNoTypeConfiguration.class).run((context) -> {
      assertThat(context).hasFailed();
      assertThat(context).getFailure()
              .hasCauseInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining(OnBeanSingleCandidateNoTypeConfiguration.class.getName());
    });
  }

  @Test
  void singleCandidateMultipleCandidatesInContextHierarchy() {
    this.contextRunner.withUserConfiguration(AlphaPrimaryConfiguration.class, BravoConfiguration.class)
            .run((parent) -> this.contextRunner.withUserConfiguration(OnBeanSingleCandidateConfiguration.class)
                    .withParent(parent)
                    .run((child) -> {
                      assertThat(child).hasBean("consumer");
                      assertThat(child.getBean("consumer")).isEqualTo("alpha");
                    }));
  }

  @Test
  void singleCandidateMultipleCandidatesOneAutowireCandidate() {
    this.contextRunner
            .withUserConfiguration(AlphaConfiguration.class, BravoNonAutowireConfiguration.class,
                    OnBeanSingleCandidateConfiguration.class)
            .run((context) -> {
              assertThat(context).hasBean("consumer");
              assertThat(context.getBean("consumer")).isEqualTo("alpha");
            });
  }

  @Test
  void singleCandidateMultipleCandidatesOneDefaultCandidate() {
    this.contextRunner
            .withUserConfiguration(AlphaConfiguration.class, BravoNonDefaultConfiguration.class,
                    OnBeanSingleCandidateConfiguration.class)
            .run((context) -> {
              assertThat(context).hasBean("consumer");
              assertThat(context.getBean("consumer")).isEqualTo("alpha");
            });
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnSingleCandidate(String.class)
  static class OnBeanSingleCandidateConfiguration {

    @Bean
    CharSequence consumer(CharSequence s) {
      return s;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnSingleCandidate(value = String.class, search = SearchStrategy.ANCESTORS)
  static class OnBeanSingleCandidateInAncestorsConfiguration {

    @Bean
    CharSequence consumer(CharSequence s) {
      return s;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnSingleCandidate(value = String.class, type = "java.lang.Integer")
  static class OnBeanSingleCandidateTwoTypesConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnSingleCandidate
  static class OnBeanSingleCandidateNoTypeConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class AlphaConfiguration {

    @Bean
    String alpha() {
      return "alpha";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class AlphaPrimaryConfiguration {

    @Bean
    @Primary
    String alpha() {
      return "alpha";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class AlphaFallbackConfiguration {

    @Bean
    @Fallback
    String alpha() {
      return "alpha";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class AlphaScopedProxyConfiguration {

    @Bean
    @Scope(proxyMode = ScopedProxyMode.INTERFACES)
    String alpha() {
      return "alpha";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class BravoConfiguration {

    @Bean
    String bravo() {
      return "bravo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class BravoPrimaryConfiguration {

    @Bean
    @Primary
    String bravo() {
      return "bravo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class BravoFallbackConfiguration {

    @Bean
    @Fallback
    String bravo() {
      return "bravo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class BravoNonAutowireConfiguration {

    @Bean(autowireCandidate = false)
    String bravo() {
      return "bravo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class BravoNonDefaultConfiguration {

    @Bean(defaultCandidate = false)
    String bravo() {
      return "bravo";
    }

  }

}
