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

## 联系方式
- 邮箱 taketoday@foxmail.com

## 开源协议

请查看 [GNU GENERAL PUBLIC LICENSE](https://github.com/TAKETODAY/today-framework/blob/master/LICENSE)

