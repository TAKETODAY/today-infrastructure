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

package infra.transaction.annotation;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

import infra.core.annotation.AnnotationUtils;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.RepeatableContainers;
import infra.transaction.interceptor.NoRollbackRuleAttribute;
import infra.transaction.interceptor.RollbackRuleAttribute;
import infra.transaction.interceptor.RuleBasedTransactionAttribute;
import infra.transaction.interceptor.TransactionAttribute;
import jakarta.transaction.Transactional;

/**
 * Strategy implementation for parsing JTA 1.2's {@link jakarta.transaction.Transactional} annotation.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class JtaTransactionAnnotationParser implements TransactionAnnotationParser, Serializable {

  @Override
  public boolean isCandidateClass(Class<?> targetClass) {
    return AnnotationUtils.isCandidateClass(targetClass, Transactional.class);
  }

  @Override
  @Nullable
  public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
    MergedAnnotation<Transactional> attributes = MergedAnnotations.from(
            element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.NONE).get(Transactional.class);
    if (attributes.isPresent()) {
      return parseTransactionAnnotation(attributes);
    }
    else {
      return null;
    }
  }

  public TransactionAttribute parseTransactionAnnotation(jakarta.transaction.Transactional ann) {
    return parseTransactionAnnotation(MergedAnnotation.from(ann));
  }

  protected TransactionAttribute parseTransactionAnnotation(MergedAnnotation<Transactional> attributes) {
    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();

    rbta.setPropagationBehaviorName(RuleBasedTransactionAttribute.PREFIX_PROPAGATION +
            attributes.getEnum("value", Transactional.TxType.class).toString());

    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();
    for (Class<?> rbRule : attributes.getClassArray("rollbackOn")) {
      rollbackRules.add(new RollbackRuleAttribute(rbRule));
    }
    for (Class<?> rbRule : attributes.getClassArray("dontRollbackOn")) {
      rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
    }
    rbta.setRollbackRules(rollbackRules);

    return rbta;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (other instanceof JtaTransactionAnnotationParser);
  }

  @Override
  public int hashCode() {
    return JtaTransactionAnnotationParser.class.hashCode();
  }

}
