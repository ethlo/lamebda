# Lamebda
[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.lamebda/lamebda.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.ethlo.lamebda%22%20AND%20a:%22lamebda%22)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](LICENSE)
[![Build Status](https://travis-ci.org/ethlo/lamebda.svg?branch=master)](https://travis-ci.org/ethlo/lamebda)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/598913bc1fe9405c82be73d9a4f105c8)](https://www.codacy.com/app/ethlo/lamebda?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ethlo/lamebda&amp;utm_campaign=Badge_Grade)

Simple HTTP handler supporting dynamic loading of HTTP handler functions. Intended for running within your existing infrastructure as a gateway or integration layer embedded with your current framework like, but not limited to, Spring MVC or Spring Flux.

> It Really Whips The Lambda's Ass!

### Example use-cases

* Ad-hoc API services and integration projects - Changes in the integration code can evolve freely from the core service
* Transactional support across multiple API calls
* Batch multiple API calls to avoid chatty data exchange
* API prototyping
* A powerful support tool for extracting data or changing state (think JMX on steroids)

### A simple example script
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
      def id = service.register(json.mypayload)
      response.json(HttpStatus.CREATED, [id: id])
    }
}
```

This scripts responds to `GET` and `POST`, and the url mapping is based on the class name hyphenated, i.e. `/my-function`.

### Usage with Spring Boot and Spring MVC

```xml
<dependency>
    <groupId>com.ethlo.lamebda</groupId>
    <artifactId>lamebda-spring-web-starter</artifactId>
    <version>0.5.0</version>
</dependency>
```

```properties
lamebda.enabled=true
lamebda.source.directory=/my/scripts
lamebda.request-path=/mypath
```

Your function should be available under `/servlet/mypath/my-function`

### Invocation/delegation from a standard HttpServlet

```xml
<dependency>
    <groupId>com.ethlo.lamebda</groupId>
    <artifactId>lamebda-servlet</artifactId>
    <version>0.5.0</version>
</dependency>
```

```java
public class MyLamebdaServlet implements HttpServlet
{
    String contextPath = "/servlet"
    String sourceDir = "/my/scripts"
    final ClassResourceLoader classResourceLoader = new FileSystemClassResourceLoader(f->f, sourceDir);
    return new FunctionManager(classResourceLoader);

    @Override
    public void service(HttpServletrequest req, HttpServletResponse res)
    {
        final HttpRequest request = new ServletHttpRequest(contextPath, request);
        final HttpResponse ressponse = new ServletHttpResponse(response);
        functionManager.handle(request, response);
    }
}
```

Add the example script below to the `sourcDir` folder.

Your function should be available under `/servlet/my-function`
