package cn.taketoday.scripting.groovy;

import cn.taketoday.scripting.Calculator

class DelegatingCalculator implements Calculator {

  def Calculator delegate;

  @Override
  int add(int x, int y) {
    //println "hello"
    //println this.metaClass.getClass()
    //println delegate.metaClass.getClass()
    //delegate.metaClass.invokeMethod("add", [x,y])

    delegate.callMissingMethod()

    return delegate.add(x, y)
  }

}
