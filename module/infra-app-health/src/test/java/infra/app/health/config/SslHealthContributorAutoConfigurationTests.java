/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.health.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import infra.app.config.ssl.SslAutoConfiguration;
import infra.app.health.SslHealthIndicator;
import infra.app.health.config.SslHealthContributorAutoConfigurationTests.CustomSslInfoConfiguration.CustomSslHealthIndicator;
import infra.app.health.config.registry.HealthContributorRegistryAutoConfiguration;
import infra.app.health.contributor.Health;
import infra.app.health.contributor.HealthIndicator;
import infra.app.health.contributor.Status;
import infra.app.info.SslInfo;
import infra.app.info.SslInfo.CertificateChainInfo;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.core.ssl.SslBundles;
import infra.test.classpath.resources.WithPackageResources;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslHealthContributorAutoConfiguration}.
 *
 * @author Jonatan Ivanov
 */
@WithPackageResources("test.jks")
class SslHealthContributorAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(SslHealthContributorAutoConfiguration.class,
                  HealthContributorRegistryAutoConfiguration.class, SslAutoConfiguration.class))
          .withPropertyValues("server.ssl.bundle=ssltest",
                  "ssl.bundle.jks.ssltest.keystore.location=classpath:test.jks");

  @Test
  void beansShouldNotBeConfigured() {
    this.contextRunner.withPropertyValues("app.health.ssl.enabled=false")
            .run((context) -> assertThat(context).doesNotHaveBean(HealthIndicator.class)
                    .doesNotHaveBean(SslInfo.class));
  }

  @Test
  void beansShouldBeConfigured() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(SslHealthIndicator.class);
      assertThat(context).hasSingleBean(SslInfo.class);
      Health health = context.getBean(SslHealthIndicator.class).health();
      assertThat(health.getStatus()).isSameAs(Status.OUT_OF_SERVICE);
      assertDetailsKeys(health);
      List<CertificateChainInfo> invalidChains = getInvalidChains(health);
      assertThat(invalidChains).hasSize(1);
      assertThat(invalidChains).first().isInstanceOf(CertificateChainInfo.class);

    });
  }

  @Test
  void beansShouldBeConfiguredWithWarningThreshold() {
    this.contextRunner.withPropertyValues("app.health.ssl.certificate-validity-warning-threshold=1d")
            .run((context) -> {
              assertThat(context).hasSingleBean(SslHealthIndicator.class);
              assertThat(context).hasSingleBean(SslInfo.class);
              assertThat(context).hasSingleBean(SslHealthIndicatorProperties.class);
              assertThat(context.getBean(SslHealthIndicatorProperties.class).certificateValidityWarningThreshold)
                      .isEqualTo(Duration.ofDays(1));
              Health health = context.getBean(SslHealthIndicator.class).health();
              assertThat(health.getStatus()).isSameAs(Status.OUT_OF_SERVICE);
              assertDetailsKeys(health);
              List<CertificateChainInfo> invalidChains = getInvalidChains(health);
              assertThat(invalidChains).hasSize(1);
              assertThat(invalidChains).first().isInstanceOf(CertificateChainInfo.class);
            });
  }

  @Test
  void customBeansShouldBeConfigured() {
    this.contextRunner.withUserConfiguration(CustomSslInfoConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(SslHealthIndicator.class);
      assertThat(context.getBean(SslHealthIndicator.class))
              .isSameAs(context.getBean(CustomSslHealthIndicator.class));
      assertThat(context).hasSingleBean(SslInfo.class);
      assertThat(context.getBean(SslInfo.class)).isSameAs(context.getBean("customSslInfo"));
      Health health = context.getBean(SslHealthIndicator.class).health();
      assertThat(health.getStatus()).isSameAs(Status.OUT_OF_SERVICE);
      assertDetailsKeys(health);
      List<CertificateChainInfo> invalidChains = getInvalidChains(health);
      assertThat(invalidChains).hasSize(1);
      assertThat(invalidChains).first().isInstanceOf(CertificateChainInfo.class);
    });
  }

  private static void assertDetailsKeys(Health health) {
    assertThat(health.getDetails()).containsOnlyKeys("expiringChains", "validChains", "invalidChains");
  }

  @SuppressWarnings("unchecked")
  private static List<CertificateChainInfo> getInvalidChains(Health health) {
    List<CertificateChainInfo> invalidChains = (List<CertificateChainInfo>) health.getDetails()
            .get("invalidChains");
    assertThat(invalidChains).isNotNull();
    return invalidChains;
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomSslInfoConfiguration {

    @Bean
    SslHealthIndicator sslHealthIndicator(SslInfo sslInfo) {
      return new CustomSslHealthIndicator(sslInfo);
    }

    @Bean
    SslInfo customSslInfo(SslBundles sslBundles) {
      return new SslInfo(sslBundles);
    }

    static class CustomSslHealthIndicator extends SslHealthIndicator {

      CustomSslHealthIndicator(SslInfo sslInfo) {
        super(sslInfo, Duration.ofDays(7));
      }

    }

  }

}
