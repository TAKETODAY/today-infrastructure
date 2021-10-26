package cn.taketoday.context.loader;


import cn.taketoday.beans.factory.Scope;
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

  private String scopeName = Scope.SINGLETON;

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

}
