package infra.beans.aot;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/4/7 13:29
 */
class BeanMetadataReflectiveProcessorTests {

  private final BeanMetadataReflectiveProcessor processor = new BeanMetadataReflectiveProcessor();

  private final ReflectionHints hints = new ReflectionHints();

  @Test
  void registerReflectionHints_withSimpleBean() {
    processor.registerReflectionHints(hints, SimpleBean.class);

    assertThat(hints.getTypeHint(SimpleBean.class)).isNotNull();
    assertThat(hints.getTypeHint(SimpleBean.class).constructors()).isNotEmpty();
    assertThat(hints.getTypeHint(SimpleBean.class).methods())
            .extracting(method -> method.getName())
            .contains("getName", "setName", "getAge", "setAge");
    assertThat(hints.getTypeHint(SimpleBean.class).fields())
            .extracting(field -> field.getName())
            .contains("name", "age");
  }

  @Test
  void registerReflectionHints_withNonClassElement_logsWarning() {
    Method method = ReflectionUtils.findMethod(SimpleBean.class, "getName");
    assertThat(method).isNotNull();

    processor.registerReflectionHints(hints, method);

    assertThat(hints.getTypeHint(SimpleBean.class)).isNull();
  }

  @Test
  void registerReflectionHints_withComplexPropertyType() {
    processor.registerReflectionHints(hints, BeanWithAddress.class);

    assertThat(hints.getTypeHint(BeanWithAddress.class)).isNotNull();
    assertThat(hints.getTypeHint(Address.class)).isNotNull();
    assertThat(hints.getTypeHint(Address.class).constructors()).isNotEmpty();
  }

  @Test
  void registerReflectionHints_withNestedBean() {
    processor.registerReflectionHints(hints, OuterBean.class);

    assertThat(hints.getTypeHint(OuterBean.class)).isNotNull();
    assertThat(hints.getTypeHint(InnerBean.class)).isNotNull();
    assertThat(hints.getTypeHint(InnerBean.class).methods())
            .extracting(method -> method.getName())
            .contains("getValue", "setValue");
  }

  @Test
  void registerReflectionHints_withPrimitiveTypes() {
    processor.registerReflectionHints(hints, BeanWithPrimitives.class);

    assertThat(hints.getTypeHint(BeanWithPrimitives.class)).isNotNull();
    assertThat(hints.getTypeHint(BeanWithPrimitives.class).methods())
            .extracting(method -> method.getName())
            .contains("getIntValue", "setIntValue", "isFlag", "setFlag");
  }

  @Test
  void registerReflectionHints_withListProperty() {
    processor.registerReflectionHints(hints, BeanWithList.class);

    assertThat(hints.getTypeHint(BeanWithList.class)).isNotNull();
    assertThat(hints.getTypeHint(String.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withFieldOnlyProperty() {
    processor.registerReflectionHints(hints, BeanWithFieldOnly.class);

    assertThat(hints.getTypeHint(BeanWithFieldOnly.class)).isNotNull();
    assertThat(hints.getTypeHint(BeanWithFieldOnly.class).fields())
            .extracting(field -> field.getName())
            .contains("fieldOnly");
  }

  @Test
  void registerReflectionHints_withReadOnlyProperty() {
    processor.registerReflectionHints(hints, BeanWithReadOnly.class);

    assertThat(hints.getTypeHint(BeanWithReadOnly.class)).isNotNull();
    assertThat(hints.getTypeHint(BeanWithReadOnly.class).methods())
            .extracting(method -> method.getName())
            .contains("getReadOnly");
  }

  @Test
  void registerReflectionHints_withWriteOnlyProperty() {
    processor.registerReflectionHints(hints, BeanWithWriteOnly.class);

    assertThat(hints.getTypeHint(BeanWithWriteOnly.class)).isNotNull();
    assertThat(hints.getTypeHint(BeanWithWriteOnly.class).methods())
            .extracting(method -> method.getName())
            .contains("setWriteOnly");
  }

  @Test
  void registerReflectionHints_withMetaAnnotatedBean() {
    processor.registerReflectionHints(hints, MetaAnnotatedBean.class);

    assertThat(hints.getTypeHint(MetaAnnotatedBean.class)).isNotNull();
    assertThat(hints.getTypeHint(MetaAnnotatedBean.class).methods())
            .extracting(method -> method.getName())
            .contains("getData", "setData");
  }

  @Test
  void registerReflectionHints_withBeanHavingConstructorArgs() {
    processor.registerReflectionHints(hints, BeanWithConstructor.class);

    assertThat(hints.getTypeHint(BeanWithConstructor.class)).isNotNull();
    assertThat(hints.getTypeHint(BeanWithConstructor.class).constructors()).isNotEmpty();
  }

  @Test
  void registerReflectionHints_withInterfaceProperty() {
    processor.registerReflectionHints(hints, BeanWithInterface.class);

    assertThat(hints.getTypeHint(BeanWithInterface.class)).isNotNull();
    assertThat(hints.getTypeHint(List.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withEnumProperty() {
    processor.registerReflectionHints(hints, BeanWithEnum.class);

    assertThat(hints.getTypeHint(BeanWithEnum.class)).isNotNull();
    assertThat(hints.getTypeHint(TestEnum.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withArrayProperty() {
    processor.registerReflectionHints(hints, BeanWithArray.class);

    assertThat(hints.getTypeHint(BeanWithArray.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withMapProperty() {
    processor.registerReflectionHints(hints, BeanWithMap.class);

    assertThat(hints.getTypeHint(BeanWithMap.class)).isNotNull();
    assertThat(hints.getTypeHint(Map.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withOptionalProperty() {
    processor.registerReflectionHints(hints, BeanWithOptional.class);

    assertThat(hints.getTypeHint(BeanWithOptional.class)).isNotNull();
    assertThat(hints.getTypeHint(Optional.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withDateProperty() {
    processor.registerReflectionHints(hints, BeanWithDate.class);

    assertThat(hints.getTypeHint(BeanWithDate.class)).isNotNull();
    assertThat(hints.getTypeHint(Date.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withLocalDateTimeProperty() {
    processor.registerReflectionHints(hints, BeanWithLocalDateTime.class);

    assertThat(hints.getTypeHint(BeanWithLocalDateTime.class)).isNotNull();
    assertThat(hints.getTypeHint(LocalDateTime.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_preservesExistingHints() {
    hints.registerType(SimpleBean.class, MemberCategory.INVOKE_PUBLIC_METHODS);

    processor.registerReflectionHints(hints, SimpleBean.class);

    assertThat(hints.getTypeHint(SimpleBean.class)).isNotNull();
    assertThat(hints.getTypeHint(SimpleBean.class).getMemberCategories())
            .contains(MemberCategory.INVOKE_PUBLIC_METHODS);
  }

  @RegisterBeanMetadata
  static class SimpleBean {
    private String name;
    private int age;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithAddress {
    private Address address;

    public Address getAddress() {
      return address;
    }

    public void setAddress(Address address) {
      this.address = address;
    }
  }

  static class Address {
    private String street;
    private String city;

    public String getStreet() {
      return street;
    }

    public void setStreet(String street) {
      this.street = street;
    }

    public String getCity() {
      return city;
    }

    public void setCity(String city) {
      this.city = city;
    }
  }

  @RegisterBeanMetadata
  static class OuterBean {
    private InnerBean inner;

    public InnerBean getInner() {
      return inner;
    }

    public void setInner(InnerBean inner) {
      this.inner = inner;
    }
  }

  static class InnerBean {
    private String value;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithPrimitives {
    private int intValue;
    private boolean flag;

    public int getIntValue() {
      return intValue;
    }

    public void setIntValue(int intValue) {
      this.intValue = intValue;
    }

    public boolean isFlag() {
      return flag;
    }

    public void setFlag(boolean flag) {
      this.flag = flag;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithList {
    private List<String> items;

    public List<String> getItems() {
      return items;
    }

    public void setItems(List<String> items) {
      this.items = items;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithFieldOnly {
    private String fieldOnly;
  }

  @RegisterBeanMetadata
  static class BeanWithReadOnly {
    private String readOnly = "constant";

    public String getReadOnly() {
      return readOnly;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithWriteOnly {
    private String writeOnly;

    public void setWriteOnly(String writeOnly) {
      this.writeOnly = writeOnly;
    }
  }

  @CustomBeanMetadata
  static class MetaAnnotatedBean {
    private String data;

    public String getData() {
      return data;
    }

    public void setData(String data) {
      this.data = data;
    }
  }

  @RegisterBeanMetadata
  @interface CustomBeanMetadata {
  }

  @RegisterBeanMetadata
  static class BeanWithConstructor {
    private String value;

    public BeanWithConstructor() {
    }

    public BeanWithConstructor(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithInterface {
    private List<String> list;

    public List<String> getList() {
      return list;
    }

    public void setList(List<String> list) {
      this.list = list;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithEnum {
    private TestEnum status;

    public TestEnum getStatus() {
      return status;
    }

    public void setStatus(TestEnum status) {
      this.status = status;
    }
  }

  enum TestEnum {
    ACTIVE, INACTIVE
  }

  @RegisterBeanMetadata
  static class BeanWithArray {
    private String[] items;

    public String[] getItems() {
      return items;
    }

    public void setItems(String[] items) {
      this.items = items;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithMap {
    private Map<String, Object> data;

    public Map<String, Object> getData() {
      return data;
    }

    public void setData(Map<String, Object> data) {
      this.data = data;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithOptional {
    private Optional<String> value;

    public Optional<String> getValue() {
      return value;
    }

    public void setValue(Optional<String> value) {
      this.value = value;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithDate {
    private Date date;

    public Date getDate() {
      return date;
    }

    public void setDate(Date date) {
      this.date = date;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithLocalDateTime {
    private LocalDateTime dateTime;

    public LocalDateTime getDateTime() {
      return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
      this.dateTime = dateTime;
    }
  }

}