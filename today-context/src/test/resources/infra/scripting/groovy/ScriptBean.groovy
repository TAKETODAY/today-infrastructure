package infra.scripting.groovy

import ApplicationContext
import ApplicationContextAware
import TestBean
import infra.scripting.ContextScriptBean

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
