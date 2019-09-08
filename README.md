# TODAY framework

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3fc111bcdf694f96bbf1a063058eea36)](https://app.codacy.com/app/TAKETODAY/today-framework?utm_source=github.com&utm_medium=referral&utm_content=TAKETODAY/today-framework&utm_campaign=Badge_Grade_Settings)


## 开始

```java

@RestController
@PropertiesSource("classpath:info.properties")
public class TestApplication implements WebMvcConfiguration {

    public static void main(String[] args) {
        WebApplication.run(TestApplication.class, args);
    }

    @GET("index/{q}")
    public String index(@PathVariable String q, HttpSession httpSession) {
        return q;
    }

    @Override
    public void configureResourceMappings(ResourceMappingRegistry registry) {

        registry.addResourceMapping("/assets/**")//
                .addLocations("classpath:assets/");

    }

}
```

## 🙏 鸣谢
本项目的诞生离不开以下开源项目：
* [Slf4j](https://github.com/qos-ch/slf4j): Simple Logging Facade for Java
* [EL](https://github.com/TAKETODAY/today-expression): Java Unified Expression Language
* [Lombok](https://github.com/rzwitserloot/lombok): Very spicy additions to the Java programming language
* [FastJSON](https://github.com/alibaba/fastjson): A fast JSON parser/generator for Java
* [Freemarker](https://github.com/apache/freemarker): Apache Freemarker
* [Apache Commons FileUpload](https://github.com/apache/commons-fileupload): Apache Commons FileUpload
* [Jetty](https://github.com/eclipse/jetty.project): Eclipse Jetty® - Web Container 
* [Tomcat](https://github.com/apache/tomcat): Apache Tomcat
* [Undertow](https://github.com/undertow-io/undertow): High performance non-blocking webserver
* [Today Web](https://github.com/TAKETODAY/today-web): Servlet based high-performance lightweight web framework
* [Today Context](https://github.com/TAKETODAY/today-context): Lightweight dependency injection framework



## 📄 开源协议
请查看 [GNU GENERAL PUBLIC LICENSE](https://github.com/TAKETODAY/today-framework/blob/master/LICENSE)

