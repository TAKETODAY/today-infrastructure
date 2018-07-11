# Today Web Framework

Today Web Framework 一个类似 SpringMvc的轻量级web框架

### 通过@ActionProcessor,@RestProcessor配置请求处理器
```java
@RestProcessor
@ActionProcessor
public class IndexAction {

}
```
##配置请求
```java

@GET("index")
@POST("post")
@PUT("article/{1}")
......
@ActionMapping("/users/{id}")
@ActionMapping(value = "/users/**", method = {RequestMethod.GET})
@ActionMapping(value = "/users/*.html", method = {RequestMethod.GET})
@ActionMapping(value = {"/index.action", "/index.do", "/index"}, method = RequestMethod.GET)
@Interceptor({LoginInterceptor.class, ...})
public (String|List<?>|Set<?>|Map<?>|void|File|Image|...) \\w+ (request, request, session,servletContext, str, int, long , byte, short, boolean, @Session("loginUser"), @Header("User-Agent"), @Cookie("JSESSIONID"), @PathVariable("id"), @RequestBody("users"), @Multipart("uploadFiles") MultipartFile[]) {
    service...
    return </>;
}
```
## 配置自定义参数转换器
```java
@ParameterConverter 
public class DateConverter implements Converter<String, Date> {
    @Override
    public Date doConvert(String source) throws ConversionException {
        ...
    }
}
```


### 也可以通过xml文件配置简单视图, 静态资源 

```xml  

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Web-Configuration PUBLIC 
    "-//TODAY BLOG//Web - Configuration DTD 2.0//"
        "https://taketoday.cn/framework/web/dtd/web-configuration-2.1.0.dtd">

<Web-Configuration>

    <static-resources mapping="/assets/*" />
    
    <common prefix="/WEB-INF/error/" suffix=".jsp">
        <view res="400" name="BadRequest" />
        <view res="403" name="Forbidden" />
        <view res="404" name="NotFound" />
        <view res="500" name="ServerIsBusy" />
        <view res="405" name="MethodNotAllowed" />
    </common>

</Web-Configuration>

```
##  登录实例
```java
@ActionProcessor
public final class UserAction {

/* 简单视图可以使用xml文件定义，只支持jsp
    <common prefix="/WEB-INF/view/" suffix=".jsp">
        <view res="login" name="login" />
        <view res="register" name="register" />
    </common> */

    @ActionMapping(value = "/login" , method = RequestMethod.GET)
    public String login() {
        return "/login/login";//支持jsp,FreeMarker,Thymeleaf,自定义视图
    }

    @ResponseBody
    @ActionMapping(value = "/login" , method = RequestMethod.POST)
    public String login(@RequestParam(required = true) String userId,@RequestParam(required = true) String passwd, HttpServletRequest request) {
        if(userId.equals(passwd)) {
            return"{\"msg\":\"登录成功\"}";
        }
        return"{\"msg\":\"登录失败\"}";
    }
}
```
## 支持文件下载 ， 支持直接返回给浏览器图片

```java
@ActionMapping(value = {"/download"}, method = RequestMethod.GET)
public File download(String path) {
    return new File(path);
}
```
```java
@GET("/display")
public final BufferedImage display(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("image/jpeg");
    return ImageIO.read(new File("D:/www.yhj.com/webapps/upload/logo.png"));
}

@GET("captcha")
public final BufferedImage captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
    BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics graphics = image.getGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
    Graphics2D graphics2d = (Graphics2D) graphics;
    drawRandomNum(graphics2d, request);
    return image;
}
```

```java
@ResponseBody
@ActionMapping(value = { "/upload" }, method = RequestMethod.POST)
public String upload(@Multipart MultipartFile uploadFile)
        throws IOException {

    String upload = "D:/www.yhj.com/webapps/upload/";
    String path = upload + uploadFile.getFileName();
    File file = new File(path);
    uploadFile.save(file);

    return "/upload/" + uploadFile.getFileName();
}

@ResponseBody
@ActionMapping(value = { "/upload/multi" }, method = RequestMethod.POST)
public String multiUpload(HttpServletRequest request, HttpSession session, HttpServletResponse response,
        @Multipart MultipartFile[] files) throws IOException {

    String upload = "D:/www.yhj.com/webapps/upload/";

    for (MultipartFile multipartFile : files) {
        String path = upload + multipartFile.getFileName();
        File file = new File(path);
        System.out.println(path);
        if (!multipartFile.save(file)) {
            return "<script>alert('upload error !')</script>";
            //response.getWriter().print("<script>alert('upload error !')</script>");
        }
    }
    //response.getWriter().print("<script>alert('upload success !')</script>");
    return "<script>alert('upload success !')</script>";
}
```

## 加入 IOC
         加入ioc [today-context](https://gitee.com/TAKETODAY/today_context)

### 精确配置静态资源, 文件上传, 视图

```xml
<Web-Configuration>

    <static-resources mapping="/assets/*" />
    
    <!-- <multipart class="cn.taketoday.web.multipart.DefaultMultipartResolver"> 或者自定义-->
    <multipart class="cn.taketoday.web.multipart.CommonsMultipartResolver">
        <upload-encoding>UTF-8</upload-encoding>
        <!-- <upload-location>D:/upload</upload-location> -->
        <upload-maxFileSize>10240000</upload-maxFileSize>
        <upload-maxRequestSize>1024000000</upload-maxRequestSize>
        <upload-fileSizeThreshold>1000000000</upload-fileSizeThreshold>
    </multipart>

    <!-- 默认-> <view-resolver class="cn.taketoday.web.view.JstlViewResolver"> 可以自定义-->
    <view-resolver class="cn.taketoday.web.view.FreeMarkerViewResolver">
        <view-suffix>.ftl</view-suffix>
        <view-encoding>UTF-8</view-encoding>
        <view-prefix>/WEB-INF/view</view-prefix>
    </view-resolver>


    <common prefix="/WEB-INF/error/" suffix=".jsp">
        <view res="400" name="BadRequest" />
        <view res="403" name="Forbidden" />
        <view res="404" name="NotFound" />
        <view res="500" name="ServerIsBusy" />
        <view res="405" name="MethodNotAllowed" />
    </common>

</Web-Configuration>
```

## end.
