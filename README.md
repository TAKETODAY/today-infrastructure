# TODAY JDBC 

[Sql2o](https://github.com/aaberg/sql2o) 改良版


![Java8](https://img.shields.io/badge/JDK-8+-success.svg)
[![GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)
[![Author](https://img.shields.io/badge/Author-TODAY-blue.svg)](https://github.com/TAKETODAY)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/b5b336564b304eaba40a39a211c2dd45)](https://www.codacy.com/gh/TAKETODAY/today-jdbc/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=TAKETODAY/today-jdbc&amp;utm_campaign=Badge_Grade)
[![GitHub CI](https://github.com/TAKETODAY/today-jdbc/workflows/GitHub%20CI/badge.svg)](https://github.com/TAKETODAY/today-jdbc/actions)


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


