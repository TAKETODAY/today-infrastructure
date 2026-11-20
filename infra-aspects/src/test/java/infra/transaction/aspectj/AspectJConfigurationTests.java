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

package infra.transaction.aspectj;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.transaction.TransactionManager;
import infra.transaction.interceptor.TransactionAttributeSource;
import infra.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

class AspectJConfigurationTests {

  private final TransactionAttributeSource source = (method, targetClass) -> null;

  private TransactionAttributeSource annotationSource;
  private TransactionAttributeSource jtaSource;
  private TransactionManager annotationManager;
  private TransactionManager jtaManager;

  @BeforeEach
  void saveAspectConfiguration() {
    AnnotationTransactionAspect annotationAspect = AnnotationTransactionAspect.aspectOf();
    JtaAnnotationTransactionAspect jtaAspect = JtaAnnotationTransactionAspect.aspectOf();
    this.annotationSource = annotationAspect.getTransactionAttributeSource();
    this.annotationManager = annotationAspect.getTransactionManager();
    this.jtaSource = jtaAspect.getTransactionAttributeSource();
    this.jtaManager = jtaAspect.getTransactionManager();
  }

  @AfterEach
  void restoreAspectConfiguration() {
    AnnotationTransactionAspect annotationAspect = AnnotationTransactionAspect.aspectOf();
    annotationAspect.setTransactionAttributeSource(this.annotationSource);
    annotationAspect.setTransactionManager(this.annotationManager);
    JtaAnnotationTransactionAspect jtaAspect = JtaAnnotationTransactionAspect.aspectOf();
    jtaAspect.setTransactionAttributeSource(this.jtaSource);
    jtaAspect.setTransactionManager(this.jtaManager);
  }

  @Test
  void transactionAspectPreservesExistingManagerWhenNoManagerIsConfigured() {
    var configuration = new AspectJTransactionManagementConfiguration();
    var existingManager = new CallCountingTransactionManager();
    AnnotationTransactionAspect.aspectOf().setTransactionManager(existingManager);

    AnnotationTransactionAspect aspect = configuration.transactionAspect(this.source);

    assertThat(aspect).isSameAs(AnnotationTransactionAspect.aspectOf());
    assertThat(aspect.getTransactionAttributeSource()).isSameAs(this.source);
    assertThat(aspect.getTransactionManager()).isSameAs(existingManager);
  }

  @Test
  void jtaTransactionAspectPreservesExistingManagerWhenNoManagerIsConfigured() {
    var configuration = new AspectJJtaTransactionManagementConfiguration();
    var existingManager = new CallCountingTransactionManager();
    JtaAnnotationTransactionAspect.aspectOf().setTransactionManager(existingManager);

    JtaAnnotationTransactionAspect aspect = configuration.jtaTransactionAspect(this.source);

    assertThat(aspect.getTransactionAttributeSource()).isSameAs(this.source);
    assertThat(aspect.getTransactionManager()).isSameAs(existingManager);
  }

  @Test
  void transactionAspectUsesConfiguredTransactionManager() {
    CallCountingTransactionManager transactionManager = new CallCountingTransactionManager();
    TestableTransactionConfiguration configuration = new TestableTransactionConfiguration();
    configuration.setTransactionManagerForTest(transactionManager);

    AnnotationTransactionAspect aspect = configuration.transactionAspect(this.source);

    assertThat(aspect.getTransactionManager()).isSameAs(transactionManager);
  }

  @Test
  void jtaTransactionAspectUsesConfiguredTransactionManager() {
    CallCountingTransactionManager transactionManager = new CallCountingTransactionManager();
    TestableJtaTransactionConfiguration configuration = new TestableJtaTransactionConfiguration();
    configuration.setTransactionManagerForTest(transactionManager);

    JtaAnnotationTransactionAspect aspect = configuration.jtaTransactionAspect(this.source);

    assertThat(aspect).isSameAs(JtaAnnotationTransactionAspect.aspectOf());
    assertThat(aspect.getTransactionAttributeSource()).isSameAs(this.source);
    assertThat(aspect.getTransactionManager()).isSameAs(transactionManager);
  }

  private static class TestableTransactionConfiguration extends AspectJTransactionManagementConfiguration {

    void setTransactionManagerForTest(TransactionManager transactionManager) {
      this.txManager = transactionManager;
    }
  }

  private static class TestableJtaTransactionConfiguration extends AspectJJtaTransactionManagementConfiguration {

    void setTransactionManagerForTest(TransactionManager transactionManager) {
      this.txManager = transactionManager;
    }
  }
}
