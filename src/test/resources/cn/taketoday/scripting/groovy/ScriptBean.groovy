package cn.taketoday.scripting.groovy

import cn.taketoday.beans.testfixture.beans.TestBean
import cn.taketoday.context.ApplicationContext
import cn.taketoday.context.aware.ApplicationContextAware
import cn.taketoday.scripting.ContextScriptBean

class GroovyScriptBean implements ContextScriptBean, ApplicationContextAware {

  private int age

  @Override
  int getAge() {
    return this.age
  }

  @Override
  void setAge(int age) {
    this.age = age
  }

  def String name

  def TestBean testBean;

  def ApplicationContext applicationContext
}
