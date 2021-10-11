package cn.taketoday.core.bytecode.proxy;

class D5 implements DI5 {
  public int vararg(String... strs) {
    return strs.length;
  }
}
