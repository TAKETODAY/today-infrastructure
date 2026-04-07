package infra.beans.aot;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.beans.BeanUtils;
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

  @Test
  void registerReflectionHints_withExcludeSelf() {
    processor.registerReflectionHints(hints, ConfigWithExcludedSelf.class);

    assertThat(hints.getTypeHint(ConfigWithExcludedSelf.class)).isNull();
    assertThat(hints.getTypeHint(User.class)).isNotNull();
    assertThat(hints.getTypeHint(Order.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withExtraClasses() {
    processor.registerReflectionHints(hints, ConfigWithExtra.class);

    assertThat(hints.getTypeHint(ConfigWithExtra.class)).isNotNull();
    assertThat(hints.getTypeHint(User.class)).isNotNull();
    assertThat(hints.getTypeHint(Order.class)).isNotNull();

    assertThat(hints.getTypeHint(User.class).methods())
            .extracting(method -> method.getName())
            .contains("getName", "setName");

    assertThat(hints.getTypeHint(Order.class).methods())
            .extracting(method -> method.getName())
            .contains("getId", "setId");
  }

  @Test
  void registerReflectionHints_withExtraClassNames() {
    processor.registerReflectionHints(hints, ConfigWithExtraNames.class);

    assertThat(hints.getTypeHint(ConfigWithExtraNames.class)).isNotNull();
    assertThat(hints.getTypeHint(User.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withMixedExtraAndNames() {
    processor.registerReflectionHints(hints, ConfigWithMixed.class);

    assertThat(hints.getTypeHint(ConfigWithMixed.class)).isNotNull();
    assertThat(hints.getTypeHint(User.class)).isNotNull();
    assertThat(hints.getTypeHint(Order.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_excludeSelfWithExtra() {
    processor.registerReflectionHints(hints, ConfigExcludeSelfWithExtra.class);

    assertThat(hints.getTypeHint(ConfigExcludeSelfWithExtra.class)).isNull();
    assertThat(hints.getTypeHint(User.class)).isNotNull();
    assertThat(hints.getTypeHint(Order.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withDuplicateClasses() {
    processor.registerReflectionHints(hints, ConfigWithDuplicate.class);

    assertThat(hints.getTypeHint(ConfigWithDuplicate.class)).isNotNull();
    assertThat(hints.getTypeHint(User.class)).isNotNull();

    assertThat(hints.getTypeHint(User.class).constructors()).hasSize(1);
  }

  @Test
  void registerReflectionHints_withInvalidClassName() {
    processor.registerReflectionHints(hints, ConfigWithInvalidName.class);

    assertThat(hints.getTypeHint(ConfigWithInvalidName.class)).isNull();
    assertThat(hints.typeHints()).isNullOrEmpty();
  }

  @Test
  void registerReflectionHints_withMethodReturnType() {
    Method method = ReflectionUtils.findMethod(MethodReturnBean.class, "getUser");
    assertThat(method).isNotNull();

    processor.registerReflectionHints(hints, method);

    assertThat(hints.getTypeHint(User.class)).isNotNull();
    assertThat(hints.getTypeHint(User.class).methods())
            .extracting(m -> m.getName())
            .contains("getName", "setName");
  }

  @Test
  void registerReflectionHints_withMethodReturnTypeExcluded() {
    Method method = ReflectionUtils.findMethod(MethodReturnBean.class, "getUser");
    assertThat(method).isNotNull();

    RegisterBeanMetadata annotation = new RegisterBeanMetadata() {
      @Override
      public Class<?>[] value() { return new Class<?>[0]; }

      @Override
      public Class<?>[] extra() { return new Class<?>[] { Order.class }; }

      @Override
      public String[] extraNames() { return new String[0]; }

      @Override
      public boolean excludeSelf() { return true; }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return RegisterBeanMetadata.class;
      }
    };

    Set<Class<?>> targetClasses = new HashSet<>();
    if (!annotation.excludeSelf()) {
      Class<?> returnType = method.getReturnType();
      if (!BeanUtils.isSimpleValueType(returnType)) {
        targetClasses.add(returnType);
      }
    }
    for (Class<?> clazz : annotation.extra()) {
      if (clazz != Object.class) {
        targetClasses.add(clazz);
      }
    }

    assertThat(targetClasses).containsExactly(Order.class);
    assertThat(targetClasses).doesNotContain(User.class);
  }

  @Test
  void registerReflectionHints_withField() {
    Field field = ReflectionUtils.findField(FieldBean.class, "user");
    assertThat(field).isNotNull();

    processor.registerReflectionHints(hints, field);

    assertThat(hints.getTypeHint(User.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withSimpleValueReturnType() {
    Method method = ReflectionUtils.findMethod(MethodReturnBean.class, "getString");
    assertThat(method).isNotNull();

    processor.registerReflectionHints(hints, method);

    assertThat(hints.getTypeHint(String.class)).isNull();
  }

  @Test
  void registerReflectionHints_withNoAnnotation() {
    processor.registerReflectionHints(hints, NonAnnotatedClass.class);
    assertThat(hints.getTypeHint(NonAnnotatedClass.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withEmptyExtra() {
    processor.registerReflectionHints(hints, ConfigWithEmptyExtra.class);

    assertThat(hints.getTypeHint(ConfigWithEmptyExtra.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_preservesAllHints() {
    hints.registerType(User.class, MemberCategory.INVOKE_PUBLIC_METHODS);

    processor.registerReflectionHints(hints, ConfigWithExtra.class);

    assertThat(hints.getTypeHint(User.class)).isNotNull();
    assertThat(hints.getTypeHint(User.class).getMemberCategories())
            .contains(MemberCategory.INVOKE_PUBLIC_METHODS);
  }

  @Test
  void registerReflectionHints_withExcludeSelfAndNoExtra() {
    processor.registerReflectionHints(hints, ConfigExcludeSelfNoExtra.class);

    assertThat(hints.typeHints()).isEmpty();
  }

  @Test
  void registerReflectionHints_withOnlyExtraClassNames() {
    processor.registerReflectionHints(hints, ConfigOnlyExtraNames.class);

    assertThat(hints.getTypeHint(ConfigOnlyExtraNames.class)).isNotNull();
    assertThat(hints.getTypeHint(User.class)).isNotNull();
    assertThat(hints.getTypeHint(Order.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_excludeSelfWithOnlyClassNames() {
    processor.registerReflectionHints(hints, ConfigExcludeSelfOnlyNames.class);

    assertThat(hints.getTypeHint(ConfigExcludeSelfOnlyNames.class)).isNull();
    assertThat(hints.getTypeHint(User.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withGenericCollection() {
    processor.registerReflectionHints(hints, BeanWithGenericCollection.class);

    assertThat(hints.getTypeHint(BeanWithGenericCollection.class)).isNotNull();
    assertThat(hints.getTypeHint(List.class)).isNotNull();
    assertThat(hints.getTypeHint(User.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withMultipleLevelsNesting() {
    processor.registerReflectionHints(hints, Level1Bean.class);

    assertThat(hints.getTypeHint(Level1Bean.class)).isNotNull();
    assertThat(hints.getTypeHint(Level2Bean.class)).isNotNull();
    assertThat(hints.getTypeHint(Level3Bean.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withAbstractPropertyType() {
    processor.registerReflectionHints(hints, BeanWithAbstractType.class);

    assertThat(hints.getTypeHint(BeanWithAbstractType.class)).isNotNull();
    assertThat(hints.getTypeHint(List.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withStaticField() {
    Field field = ReflectionUtils.findField(BeanWithStaticField.class, "STATIC_VALUE");
    assertThat(field).isNotNull();

    processor.registerReflectionHints(hints, field);

    assertThat(hints.getTypeHint(String.class)).isNull();
  }

  @Test
  void registerReflectionHints_withTransientField() {
    processor.registerReflectionHints(hints, BeanWithTransientField.class);

    assertThat(hints.getTypeHint(BeanWithTransientField.class)).isNotNull();
    assertThat(hints.getTypeHint(User.class)).isNotNull();
  }

  @Test
  void registerReflectionHints_withFinalField() {
    processor.registerReflectionHints(hints, BeanWithFinalField.class);

    assertThat(hints.getTypeHint(BeanWithFinalField.class)).isNotNull();
    assertThat(hints.getTypeHint(User.class)).isNotNull();
  }

  @RegisterBeanMetadata(excludeSelf = true)
  static class ConfigExcludeSelfNoExtra {
    private String config;
  }

  @RegisterBeanMetadata(extraNames = {
          "infra.beans.aot.BeanMetadataReflectiveProcessorTests$User",
          "infra.beans.aot.BeanMetadataReflectiveProcessorTests$Order"
  })
  static class ConfigOnlyExtraNames {
    private String config;
  }

  @RegisterBeanMetadata(excludeSelf = true, extraNames = {
          "infra.beans.aot.BeanMetadataReflectiveProcessorTests$User"
  })
  static class ConfigExcludeSelfOnlyNames {
    private String config;
  }

  @RegisterBeanMetadata
  static class BeanWithNestedComplex {
    private Address address;

    public Address getAddress() {
      return address;
    }

    public void setAddress(Address address) {
      this.address = address;
    }
  }

  static class City {
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithGenericCollection {
    private List<User> users;

    public List<User> getUsers() {
      return users;
    }

    public void setUsers(List<User> users) {
      this.users = users;
    }
  }

  @RegisterBeanMetadata
  static class Level1Bean {
    private Level2Bean level2;

    public Level2Bean getLevel2() {
      return level2;
    }

    public void setLevel2(Level2Bean level2) {
      this.level2 = level2;
    }
  }

  static class Level2Bean {
    private Level3Bean level3;

    public Level3Bean getLevel3() {
      return level3;
    }

    public void setLevel3(Level3Bean level3) {
      this.level3 = level3;
    }
  }

  static class Level3Bean {
    private String value;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithAbstractType {
    private List<String> items;

    public List<String> getItems() {
      return items;
    }

    public void setItems(List<String> items) {
      this.items = items;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithStaticField {
    public static final String STATIC_VALUE = "test";
  }

  @RegisterBeanMetadata
  static class BeanWithTransientField {
    private transient User user;

    public User getUser() {
      return user;
    }

    public void setUser(User user) {
      this.user = user;
    }
  }

  @RegisterBeanMetadata
  static class BeanWithFinalField {
    private final User user = new User();

    public User getUser() {
      return user;
    }
  }

  @RegisterBeanMetadata
  static class PackagePrivateBean {
    String value;

    String getValue() {
      return value;
    }

    void setValue(String value) {
      this.value = value;
    }
  }

  @RegisterBeanMetadata(excludeSelf = true, extra = { User.class, Order.class })
  static class ConfigWithExcludedSelf {
    private String config;
  }

  @RegisterBeanMetadata(extra = { User.class, Order.class })
  static class ConfigWithExtra {
    private String config;
  }

  @RegisterBeanMetadata(extraNames = { "infra.beans.aot.BeanMetadataReflectiveProcessorTests$User" })
  static class ConfigWithExtraNames {
    private String config;
  }

  @RegisterBeanMetadata(extra = { User.class }, extraNames = { "infra.beans.aot.BeanMetadataReflectiveProcessorTests$Order" })
  static class ConfigWithMixed {
    private String config;
  }

  @RegisterBeanMetadata(excludeSelf = true, extra = { User.class, Order.class })
  static class ConfigExcludeSelfWithExtra {
    private String config;
  }

  @RegisterBeanMetadata(extra = { User.class, User.class })
  static class ConfigWithDuplicate {
    private String config;
  }

  @RegisterBeanMetadata(extraNames = { "com.nonexistent.InvalidClass" }, excludeSelf = true)
  static class ConfigWithInvalidName {
    private String config;
  }

  @RegisterBeanMetadata
  static class MethodReturnBean {

    @RegisterBeanMetadata
    public User getUser() {
      return new User();
    }

    public String getString() {
      return "test";
    }
  }

  @RegisterBeanMetadata
  static class FieldBean {
    private User user;
  }

  static class NonAnnotatedClass {
    private String value;
  }

  @RegisterBeanMetadata(extra = {})
  static class ConfigWithEmptyExtra {
    private String config;
  }

  static class User {
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  static class Order {
    private Long id;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }
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