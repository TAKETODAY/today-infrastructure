package cn.taketoday.context.event;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.expression.BeanNameExpressionResolver;
import cn.taketoday.expression.CompositeExpressionResolver;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionResolver;
import cn.taketoday.expression.FunctionMapper;
import cn.taketoday.expression.VariableMapper;
import cn.taketoday.expression.lang.LocalBeanNameResolver;

/**
 * @author yanghaijian 2021/11/5 14:19
 */
public class EventExpressionContext extends ExpressionContext {
  private ExpressionResolver elResolver;
  private final Map<String, Object> beans;
  private final ExpressionContext parent;

  public EventExpressionContext(ExpressionContext parent) {
    this(parent, new HashMap<>(4));
  }

  public EventExpressionContext(ExpressionContext parent, Map<String, Object> beans) {
    this.beans = beans;
    this.parent = parent;
  }

  @Override
  public ExpressionResolver getResolver() {
    if (elResolver == null) {
      this.elResolver = new CompositeExpressionResolver(
              new BeanNameExpressionResolver(new LocalBeanNameResolver(beans)), // TODO map 性能优化
              parent.getResolver()
      );
    }
    return elResolver;
  }

  /**
   * Add a bean to this context
   *
   * @param name bean name
   * @param bean bean instance
   */
  public void putBean(final String name, final Object bean) {
    beans.put(name, bean);
  }

  @Override
  public FunctionMapper getFunctionMapper() {
    return this.parent.getFunctionMapper();
  }

  @Override
  public VariableMapper getVariableMapper() {
    return this.parent.getVariableMapper();
  }

  @Override
  public void setPropertyResolved(Object base, Object property) {
    setPropertyResolved(true);
  }

}
