# Lamebda
Simple HTTP processing handler supporting dynamically loading of HTTP handler functions. Intended for running within your existing infrastructure as a gateway or integration layer embedded with your current framework like, but not limited to,  Spring MVC or Spring Flux.

> It Really Whips The Lambda's Ass!

# Usage with Spring Boot and Spring MVC

```xml
<dependency>
    <groupId>com.ethlo.lamebda</groupId>
    <artifactId>lamebda-spring-web-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```properties
lamebda.enabled=true
lamebda.source.directory=/my/groovy/scripts
lamebda.request-path=/mypath
```

Voila!

### Invocation/delegation from a standard HttpServlet

```xml
<dependency>
    <groupId>com.ethlo.lamebda</groupId>
    <artifactId>lamebda-servlet</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```java
public class MyLamebdaServlet implements HttpServlet
{
    String contextPath = "/servlet"
    String sourceDir = "/var/lib/lamebda/scripts"
    final ClassResourceLoader loader = new FileSystemClassResourceLoader(f->f, sourceDir);
    return new FunctionManager(loader);

    @Override
    public void service(HttpServletrequest req, HttpServletResponse res)
    {
        final HttpRequest request = new ServletHttpRequest(PATH, request);
        final HttpResponse ressponse = new ServletHttpResponse(response);
        functionManager.handle(request, response);
    }
}
```

### An example script
```groovy
class MyFunction extends SimpleServerFunction {

    @Autowired
    private MyService service;

    @Override
    void get(HttpRequest request, HttpResponse response) {
        response.json(HttpStatus.OK, [method: request.method, message:'Hello world'])
    }

    @Override
    void post(HttpRequest request, HttpResponse response) {
      def json = request.json()
      def prop = json.myprop
      def id = service.register(prop)
      response.json(HttpStatus.201, [id: id])
    }
}
```
