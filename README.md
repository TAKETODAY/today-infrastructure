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
Hand coded <code>ResultSet</code>                                   | 45ms                   |
TODAY JDBC                                                          | 53ms (17.78% slower)   |
[Sql2o](https://github.com/aaberg/sql2o)                           | 70ms (55.56% slower)   |
[Apache DbUtils](http://commons.apache.org/proper/commons-dbutils/) | 98ms (117.78% slower)  |
[MyBatis](http://mybatis.github.io/mybatis-3/)                      | 240ms (433.33% slower) |
[JDBI](http://jdbi.org/)                                            | 319ms (608.89% slower) |
[Hibernate](http://hibernate.org/)                                  | 494ms (723% slower)    |
[Spring JdbcTemplate](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/jdbc.html) | 364ms (708.89% slower) |
[jOOQ](http://www.jooq.org)                                         | 367ms (715.56% slower) |


