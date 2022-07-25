package cn.taketoday.scripting.groovy;

import cn.taketoday.scripting.CallCounter;

class GroovyCallCounter implements CallCounter {

  int count = -100;

  void init() {
    count = 0;
  }

  @Override
  void before() {
    count++;
  }

  @Override
  int getCalls() {
    return count;
  }

  void destroy() {
    count = -200;
  }

}
