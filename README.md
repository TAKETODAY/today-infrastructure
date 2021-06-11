# TODAY JDBC 

[Sql2o](https://github.com/aaberg/sql2o) 改良版


![Java8](https://img.shields.io/badge/JDK-8+-success.svg)
[![GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)
[![Author](https://img.shields.io/badge/Author-TODAY-blue.svg)](https://github.com/TAKETODAY)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/b5b336564b304eaba40a39a211c2dd45)](https://www.codacy.com/gh/TAKETODAY/today-jdbc/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=TAKETODAY/today-jdbc&amp;utm_campaign=Badge_Grade)
[![GitHub CI](https://github.com/TAKETODAY/today-jdbc/workflows/GitHub%20CI/badge.svg)](https://github.com/TAKETODAY/today-jdbc/actions)


#### 使用实例

```java
  @Test
  public void testFetch() {
    createAndFillUserTable();

    try (JdbcConnection con = jdbcOperations.open()) {

      Date before = new Date();
      List<User> allUsers = con.createQuery("select * from User").fetch(User.class);

      assertNotNull(allUsers);

      Date after = new Date();
      long span = after.getTime() - before.getTime();
      System.out.println(String.format("Fetched %s user: %s ms", insertIntoUsers, span));

      // repeat this
      before = new Date();
      allUsers = con.createQuery("select * from User").fetch(User.class);
      after = new Date();
      span = after.getTime() - before.getTime();
      System.out.println(String.format("Again Fetched %s user: %s ms", insertIntoUsers, span));

      assertEquals(allUsers.size(), insertIntoUsers);
    }
    deleteUserTable();
  }@Test
  public void testBatch() {
    jdbcOperations.createQuery(
            "create table User(\n" +
            "id int identity primary key,\n" +
            "name varchar(20),\n" +
            "email varchar(255),\n" +
            "text varchar(100))").executeUpdate();

    String insQuery = "insert into User(name, email, text) values (:name, :email, :text)";

    JdbcConnection con = jdbcOperations.beginTransaction();
    int[] inserted = con.createQuery(insQuery)
            .addParameter("name", "test")
            .addParameter("email", "test@test.com")
            .addParameter("text", "something exciting")
            .addToBatch()

            .addParameter("name", "test2")
            .addParameter("email", "test2@test.com")
            .addParameter("text", "something exciting too")
            .addToBatch()

            .addParameter("name", "test3")
            .addParameter("email", "test3@test.com")
            .addParameter("text", "blablabla")
            .addToBatch()

            .executeBatch()
            .getBatchResult();
    con.commit();

    assertEquals(3, inserted.length);
    for (int i : inserted) {
      assertEquals(1, i);
    }

    deleteUserTable();
  }
  
```


#### Performance of SELECT

Execute 5000 SELECT statements against a DB and map the data returned to a POJO.
Code is available [here](/src/test/java/cn/taketoday/jdbc/performance/PojoPerformanceTest.java).

Method                                                              | Duration               |
------------------------------------------------------------------- | ---------------------- |
Hand coded <code>ResultSet</code>                                   | 49ms                   |
TODAY JDBC                                                          | 59ms (20.41% slower)   |
[Sql2o](https://github.com/aaberg/sql2o)                            | 65ms (32.65% slower)   |
[Apache DbUtils](http://commons.apache.org/proper/commons-dbutils/) | 107ms (118.37% slower) |
[MyBatis](http://mybatis.github.io/mybatis-3/)                      | 245ms (400.00% slower) |
[JDBI](http://jdbi.org/)                                            | 273ms (457.14% slower) |
[Hibernate](http://hibernate.org/)                                  | 280ms (471.43% slower) |
[Spring JdbcTemplate](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/jdbc.html) | 342ms (597.96% slower) |
[jOOQ](http://www.jooq.org)                                         | 374ms (663.27% slower) |


## 🙏 鸣谢
本项目的诞生离不开以下开源项目：
* [TODAY Context](https://github.com/TAKETODAY/today-context): A Java library for dependency injection and aspect oriented programing
* [Sql2o](https://github.com/aaberg/sql2o): sql2o is a small library, which makes it easy to convert the result of your sql-statements into objects. No resultset hacking required. Kind of like an orm, but without the sql-generation capabilities. Supports named parameters.

## 📄 开源协议
请查看 [GNU GENERAL PUBLIC LICENSE](https://github.com/TAKETODAY/today-jdbc/blob/master/LICENSE)

