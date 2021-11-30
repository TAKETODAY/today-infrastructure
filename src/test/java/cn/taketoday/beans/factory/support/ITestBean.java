package cn.taketoday.beans.factory.support;

import java.io.IOException;

public interface ITestBean extends AgeHolder {

  String getName();

  void setName(String name);

  ITestBean getSpouse();

  void setSpouse(ITestBean spouse);

  ITestBean[] getSpouses();

  String[] getStringArray();

  void setStringArray(String[] stringArray);

  Integer[][] getNestedIntegerArray();

  Integer[] getSomeIntegerArray();

  void setSomeIntegerArray(Integer[] someIntegerArray);

  void setNestedIntegerArray(Integer[][] nestedIntegerArray);

  int[] getSomeIntArray();

  void setSomeIntArray(int[] someIntArray);

  int[][] getNestedIntArray();

  void setNestedIntArray(int[][] someNestedArray);

  /**
   * Throws a given (non-null) exception.
   */
  void exceptional(Throwable t) throws Throwable;

  Object returnsThis();

  INestedTestBean getDoctor();

  INestedTestBean getLawyer();

  IndexedTestBean getNestedIndexedBean();

  /**
   * Increment the age by one.
   *
   * @return the previous age
   */
  int haveBirthday();

  void unreliableFileOperation() throws IOException;

}
