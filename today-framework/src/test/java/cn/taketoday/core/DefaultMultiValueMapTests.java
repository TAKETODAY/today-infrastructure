package cn.taketoday.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Harry Yang 2021/11/9 15:21
 */
class DefaultMultiValueMapTests {

  @Test
  void addAll() {

    ArrayList<String> list = new ArrayList<>();

    list.add("value1");
    list.add("value2");

    Enumeration<String> enumeration = Collections.enumeration(list);
    DefaultMultiValueMap<Object, Object> multiValueMap = new DefaultMultiValueMap<>();

    multiValueMap.addAll("key", enumeration);

    assertThat(multiValueMap).hasSize(1);

    List<Object> objectList = multiValueMap.get("key");

    assertThat(objectList).isEqualTo(list);
  }

}
