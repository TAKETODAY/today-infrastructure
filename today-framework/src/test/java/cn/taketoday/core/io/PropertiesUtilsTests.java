package cn.taketoday.core.io;

import org.junit.jupiter.api.Test;

/**
 * @author Harry Yang 2021/10/9 10:12
 */
class PropertiesUtilsTests {

  @Test
  void testCheckPropertiesName() {
    assert PropertiesUtils.checkPropertiesName("info").equals("info.properties");
    assert PropertiesUtils.checkPropertiesName("info.properties").equals("info.properties");
  }

  @Test
  void loadProperties() {

  }

}
