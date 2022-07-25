package cn.taketoday.scripting.groovy;

import FactoryBean

class TestFactoryBean implements FactoryBean {

  @Override
  public boolean isSingleton() {
    true
  }

  @Override
  public Class getObjectType() {
    String.class
  }

  @Override
  public Object getObject() {
    "test"
  }
}
