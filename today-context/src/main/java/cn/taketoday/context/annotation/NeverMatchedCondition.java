package cn.taketoday.context.annotation;

import cn.taketoday.core.type.AnnotatedTypeMetadata;

/**
 * Never match Condition
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/13 17:12
 */
public class NeverMatchedCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return false;
  }

}
