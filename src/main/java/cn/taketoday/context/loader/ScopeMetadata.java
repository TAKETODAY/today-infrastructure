package cn.taketoday.context.loader;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.annotation.ScopedProxyMode;
import cn.taketoday.lang.Assert;

/**
 * Describes scope characteristics
 *
 * <p>The default scope is "singleton"
 *
 * @author TODAY 2021/10/26 15:58
 * @see ScopeMetadataResolver
 * @since 4.0
 */
public class ScopeMetadata {

  private String scopeName = BeanDefinition.SCOPE_SINGLETON;

  private ScopedProxyMode scopedProxyMode = ScopedProxyMode.NO;

  /**
   * Set the name of the scope.
   */
  public void setScopeName(String scopeName) {
    Assert.notNull(scopeName, "'scopeName' must not be null");
    this.scopeName = scopeName;
  }

  /**
   * Get the name of the scope.
   */
  public String getScopeName() {
    return this.scopeName;
  }

  /**
   * Set the proxy-mode to be applied to the scoped instance.
   */
  public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
    Assert.notNull(scopedProxyMode, "'scopedProxyMode' must not be null");
    this.scopedProxyMode = scopedProxyMode;
  }

  /**
   * Get the proxy-mode to be applied to the scoped instance.
   */
  public ScopedProxyMode getScopedProxyMode() {
    return this.scopedProxyMode;
  }

}
