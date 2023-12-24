/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.transaction.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.RepeatableContainers;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.interceptor.NoRollbackRuleAttribute;
import cn.taketoday.transaction.interceptor.RollbackRuleAttribute;
import cn.taketoday.transaction.interceptor.RuleBasedTransactionAttribute;
import cn.taketoday.transaction.interceptor.TransactionAttribute;
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
