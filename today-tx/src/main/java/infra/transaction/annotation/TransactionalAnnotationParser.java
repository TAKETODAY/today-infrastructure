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
import java.util.Arrays;
import java.util.List;

import infra.core.annotation.AnnotationUtils;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.core.annotation.RepeatableContainers;
import infra.lang.Assert;
import infra.transaction.interceptor.NoRollbackRuleAttribute;
import infra.transaction.interceptor.RollbackRuleAttribute;
import infra.transaction.interceptor.RuleBasedTransactionAttribute;
import infra.transaction.interceptor.TransactionAttribute;
import infra.util.StringUtils;

/**
 * Strategy implementation for parsing Framework's {@link Transactional} annotation.
 *
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class TransactionalAnnotationParser implements TransactionAnnotationParser, Serializable {

  @Override
  public boolean isCandidateClass(Class<?> targetClass) {
    return AnnotationUtils.isCandidateClass(targetClass, Transactional.class);
  }

  @Override
  @Nullable
  public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
    MergedAnnotation<Transactional> attributes = MergedAnnotations.from(
            element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.NONE).get(Transactional.class);
    if (attributes.isPresent()) {
      return parseTransactionAnnotation(attributes);
    }
    else {
      return null;
    }
  }

  protected TransactionAttribute parseTransactionAnnotation(MergedAnnotation<Transactional> attributes) {
    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();

    Propagation propagation = attributes.getEnum("propagation", Propagation.class);
    rbta.setPropagationBehavior(propagation.value());
    Isolation isolation = attributes.getEnum("isolation", Isolation.class);
    rbta.setIsolationLevel(isolation.value());

    rbta.setTimeout(attributes.getInt("timeout"));
    String timeoutString = attributes.getString("timeoutString");
    Assert.isTrue(StringUtils.isBlank(timeoutString) || rbta.getTimeout() < 0,
            "Specify 'timeout' or 'timeoutString', not both");
    rbta.setTimeoutString(timeoutString);

    rbta.setReadOnly(attributes.getBoolean("readOnly"));
    rbta.setQualifier(attributes.getString("value"));
    rbta.setLabels(Arrays.asList(attributes.getStringArray("label")));

    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();
    for (Class<?> rbRule : attributes.getClassArray("rollbackFor")) {
      rollbackRules.add(new RollbackRuleAttribute(rbRule));
    }
    for (String rbRule : attributes.getStringArray("rollbackForClassName")) {
      rollbackRules.add(new RollbackRuleAttribute(rbRule));
    }
    for (Class<?> rbRule : attributes.getClassArray("noRollbackFor")) {
      rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
    }
    for (String rbRule : attributes.getStringArray("noRollbackForClassName")) {
      rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
    }
    rbta.setRollbackRules(rollbackRules);

    return rbta;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (other instanceof TransactionalAnnotationParser);
  }

  @Override
  public int hashCode() {
    return TransactionalAnnotationParser.class.hashCode();
  }

}
