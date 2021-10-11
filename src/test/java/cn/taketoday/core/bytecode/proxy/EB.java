package cn.taketoday.core.bytecode.proxy;

@SuppressWarnings("rawtypes")
public class EB extends EA implements Comparable {
  private int count;

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public final void finalTest() { }
}
