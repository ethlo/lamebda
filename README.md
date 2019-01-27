# Lamebda
[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.lamebda/lamebda.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.ethlo.lamebda%22)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](LICENSE)
[![Build Status](https://travis-ci.org/ethlo/lamebda.svg?branch=master)](https://travis-ci.org/ethlo/lamebda)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/598913bc1fe9405c82be73d9a4f105c8)](https://www.codacy.com/app/ethlo/lamebda?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ethlo/lamebda&amp;utm_campaign=Badge_Grade)

Simple HTTP handler supporting dynamic loading of HTTP handler functions. Intended for running within your existing infrastructure as a gateway or integration layer embedded with your current framework like, but not limited to, Spring MVC or Spring Flux.

> It Really Whips The Lambda's Ass!

# Example use-cases

* Ad-hoc API services and integration projects - Changes in the integration code can evolve freely from the core service
* Transactional support across multiple API calls
* Batch multiple API calls to avoid chatty data exchange
* API prototyping
* A powerful support tool for extracting data or changing state (think JMX on steroids)

# Getting started

## Integrating with your project

### Usage with Spring Boot and Spring MVC

Add dependency to your `pom.xml`
```xml
<dependency>
    <groupId>com.ethlo.lamebda</groupId>
    <artifactId>lamebda-spring-web-starter</artifactId>
    <version>0.6.0</version>
</dependency>
```

Add the following properties to `application.properties`:
```properties
lamebda.enabled=true
lamebda.source.directory=/var/lib/lamebda
lamebda.request-path=/gateway
```

### Invocation/delegation from a standard HttpServlet

```xml
<dependency>
    <groupId>com.ethlo.lamebda</groupId>
    <artifactId>lamebda-servlet</artifactId>
    <version>0.6.0</version>
</dependency>
```

```java
public class MyLamebdaServlet implements HttpServlet
{
    String contextPath = "/servlet"
    String sourceDir = "/var/lib/lamebda"
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

## Extra preparations if you have an Open API Specification (OAS) file

1. Create a home directory for Lamebda. We will use `/var/lib/lamebda` as an example.

2. Create `.generator` in the root folder, so we have `/var/lib/lamebda/.generator`. 

3. Download [`openapi-generator-cli`](http://central.maven.org/maven2/org/openapitools/openapi-generator-cli/3.3.4/openapi-generator-cli-3.3.4.jar) and [`groovy-models`](https://repo1.maven.org/maven2/com/ethlo/openapi-tools/groovy-models/0.1/groovy-models-0.1.jar) and put it into the `.generator` folder.

## Setup a project folder
Create a project folder. This folder is a logical grouping for your API functions. We will go with the create `test` for now. We now have the folder `/var/lib/lamebda/test`.


## Add your first script
1. Create a folder for the functions in the test project: `/var/lib/lamebda/test/scripts`

2. Add a simple script in the scripts folder:

```groovy
class MyFunction extends SimpleServerFunction {

    @Override
    void get(HttpRequest request, HttpResponse response) {
        response.json(HttpStatus.OK, [methods: request.methods, message:'Hello world'])
    }
```

## Access your first function

Your function should be available under `/gateway/test/my-function`

