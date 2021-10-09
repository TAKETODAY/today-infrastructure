package cn.taketoday.core.io;

import cn.taketoday.util.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yanghaijian 2021/10/9 10:12
 */
class PropertiesUtilsTests {

  @Test
  void testCheckPropertiesName() {
    assert PropertiesUtils.checkPropertiesName("info").equals("info.properties");
    assert PropertiesUtils.checkPropertiesName("info.properties").equals("info.properties");
  }

}