package cn.taketoday.core.bytecode.transform.impl;

/**
 * @author baliuka
 */
public interface PersistenceCapable {

  void setPersistenceManager(Object manager);

  Object getPersistenceManager();
}
