package cn.taketoday.beans;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/1 14:54
 */
class PropertyValuesTests {

  /**
   * Must contain: forname=Tony surname=Blair age=50
   */
  protected void doTestTony(PropertyValues pvs) {
    assertThat(pvs.toArray().length == 3).as("Contains 3").isTrue();
    assertThat(pvs.contains("forname")).as("Contains forname").isTrue();
    assertThat(pvs.contains("surname")).as("Contains surname").isTrue();
    assertThat(pvs.contains("age")).as("Contains age").isTrue();
    boolean condition1 = !pvs.contains("tory");
    assertThat(condition1).as("Doesn't contain tory").isTrue();

    PropertyValue[] ps = pvs.toArray();
    Map<String, String> m = new HashMap<>();
    m.put("forname", "Tony");
    m.put("surname", "Blair");
    m.put("age", "50");
    for (PropertyValue p : ps) {
      Object val = m.get(p.getName());
      assertThat(val != null).as("Can't have unexpected value").isTrue();
      boolean condition = val instanceof String;
      assertThat(condition).as("Val i string").isTrue();
      assertThat(val.equals(p.getValue())).as("val matches expected").isTrue();
      m.remove(p.getName());
    }
    assertThat(m.size() == 0).as("Map size is 0").isTrue();
  }

  @Test
  public void testValid() {
    PropertyValues pvs = new PropertyValues();
    pvs.add(new PropertyValue("forname", "Tony"));
    pvs.add(new PropertyValue("surname", "Blair"));
    pvs.add(new PropertyValue("age", "50"));
    doTestTony(pvs);

    PropertyValues deepCopy = new PropertyValues(pvs);
    doTestTony(deepCopy);
    deepCopy.add(new PropertyValue("name", "Gordon"));
    doTestTony(pvs);
    assertThat(deepCopy.getPropertyValue("name")).isEqualTo("Gordon");
  }

  @Test
  public void testAddOrOverride() {
    PropertyValues pvs = new PropertyValues();
    pvs.add(new PropertyValue("forname", "Tony"));
    pvs.add(new PropertyValue("surname", "Blair"));
    pvs.add(new PropertyValue("age", "50"));
    doTestTony(pvs);
    PropertyValue addedPv = new PropertyValue("rod", "Rod");
    pvs.add(addedPv);
    assertThat(pvs.getPropertyValue("rod").equals(addedPv.getValue())).isTrue();
    PropertyValue changedPv = new PropertyValue("forname", "Greg");
    pvs.add(changedPv);
    assertThat(pvs.getPropertyValue("forname").equals(changedPv.getValue())).isTrue();
  }

  @Test
  public void testChangesOnEquals() {
    PropertyValues pvs = new PropertyValues();
    pvs.add(new PropertyValue("forname", "Tony"));
    pvs.add(new PropertyValue("surname", "Blair"));
    pvs.add(new PropertyValue("age", "50"));
    PropertyValues pvs2 = pvs;
    PropertyValues changes = pvs2.changesSince(pvs);
    assertThat(changes.toArray().length == 0).as("changes are empty").isTrue();
  }

  @Test
  public void testChangeOfOneField() {
    PropertyValues pvs = new PropertyValues();
    pvs.add(new PropertyValue("forname", "Tony"));
    pvs.add(new PropertyValue("surname", "Blair"));
    pvs.add(new PropertyValue("age", "50"));

    PropertyValues pvs2 = new PropertyValues(pvs);
    PropertyValues changes = pvs2.changesSince(pvs);
    assertThat(changes.toArray().length == 0).as("changes are empty, not of length " + changes.toArray().length).isTrue();

    pvs2.add(new PropertyValue("forname", "Gordon"));
    changes = pvs2.changesSince(pvs);
    assertThat(changes.toArray().length).as("1 change").isEqualTo(1);
    Object fn = changes.getPropertyValue("forname");
    assertThat(fn != null).as("change is forname").isTrue();
    assertThat(fn.equals("Gordon")).as("new value is gordon").isTrue();

    PropertyValues pvs3 = new PropertyValues(pvs);
    changes = pvs3.changesSince(pvs);
    assertThat(changes.toArray().length == 0).as("changes are empty, not of length " + changes.toArray().length).isTrue();

    // add new
    pvs3.add(new PropertyValue("foo", "bar"));
    pvs3.add(new PropertyValue("fi", "fum"));
    changes = pvs3.changesSince(pvs);
    assertThat(changes.toArray().length == 2).as("2 change").isTrue();
    fn = changes.getPropertyValue("foo");
    assertThat(fn != null).as("change in foo").isTrue();
    assertThat(fn.equals("bar")).as("new value is bar").isTrue();
  }

  @Test
  public void iteratorContainsPropertyValue() {
    PropertyValues pvs = new PropertyValues();
    pvs.add("foo", "bar");

    Iterator<PropertyValue> it = pvs.iterator();
    assertThat(it.hasNext()).isTrue();
    PropertyValue pv = it.next();
    assertThat(pv.getName()).isEqualTo("foo");
    assertThat(pv.getValue()).isEqualTo("bar");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  public void iteratorIsEmptyForEmptyValues() {
    PropertyValues pvs = new PropertyValues();
    Iterator<PropertyValue> it = pvs.iterator();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  public void streamContainsPropertyValue() {
    PropertyValues pvs = new PropertyValues();
    pvs.add("foo", "bar");

    assertThat(pvs.stream()).isNotNull();
    assertThat(pvs.stream().count()).isEqualTo(1L);
    assertThat(pvs.stream().anyMatch(pv -> "foo".equals(pv.getName()) && "bar".equals(pv.getValue()))).isTrue();
    assertThat(pvs.stream().anyMatch(pv -> "bar".equals(pv.getName()) && "foo".equals(pv.getValue()))).isFalse();
  }

  @Test
  public void streamIsEmptyForEmptyValues() {
    PropertyValues pvs = new PropertyValues();
    assertThat(pvs.stream()).isNotNull();
    assertThat(pvs.stream().count()).isEqualTo(0L);
  }

}
