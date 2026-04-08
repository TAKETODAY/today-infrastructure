package infra.beans.support;

import org.jspecify.annotations.Nullable;

import infra.beans.BeanInstantiationException;
import infra.lang.Assert;

final class UnsafeBeanInstantiator extends BeanInstantiator {

  private final Class<?> type;

  public UnsafeBeanInstantiator(Class<?> type) {
    Assert.notNull(type, "type is required");
    this.type = type;
  }

  @Override
  protected Object doInstantiate(@Nullable Object @Nullable [] args) throws Throwable {
    try {
      return UnsafeUtils.getUnsafe().allocateInstance(this.type);
    }
    catch (InstantiationException e) {
      throw new BeanInstantiationException(type, "unsafe cannot allocate instance", e);
    }
  }

}
