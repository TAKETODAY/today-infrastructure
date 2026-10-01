# TODAY Infrastructure 文档

本目录包含 TODAY Infrastructure 的参考文档及文档站点构建配置。

## 配置示例

使用 `[configprops,yaml]` 编写配置示例，构建时会自动生成 YAML 和 Properties 两个选项卡：

```asciidoc
[configprops,yaml]
----
infra:
  profiles:
    validate: false
----
```

Properties 选项卡将嵌套对象展开为点分属性名，将列表展开为 `[0]`、`[1]` 等索引，
并将多个 YAML 文档转换为使用 `#---` 分隔的 Properties 文档。
YAML 原文保留；生成的 Properties 不保留 YAML 注释，空对象和空列表不生成属性。
扩展目前只接受 YAML 输入，不支持自定义 YAML 标签或循环别名。

扩展测试：在本目录运行 `node --test extensions/configprops.test.js`。

## 文档来源与版权

本目录中的部分文档及示例翻译、改编或移植自 Spring Framework 和 Spring Boot
的参考文档，并根据 TODAY Infrastructure 的实现进行了调整。
此来源说明适用于本目录中新旧文档中的相关衍生内容，不表示所有内容均来自上游。

上游项目：

- [Spring Framework](https://github.com/spring-projects/spring-framework)
- [Spring Boot](https://github.com/spring-projects/spring-boot)

原始内容的版权归原作者所有，相关版权、许可证及 NOTICE 声明予以保留。
上游相关内容采用 Apache License 2.0；许可证文本见仓库根目录的
[LICENSE](../LICENSE)。本说明不替代文件内已有的原始版权或许可证声明。

修改部分：Copyright 2017 - 2026 the TODAY authors.

文档来源与修改版权在此统一说明，各页面无需重复添加 `Derived from` 和
`Modifications Copyright` 注释。文件或引用示例中已有的原始版权及许可证头仍应保留。
