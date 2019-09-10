# Lamebda
[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.lamebda/lamebda.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.ethlo.lamebda%22)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](LICENSE)
[![Build Status](https://travis-ci.org/ethlo/lamebda.svg?branch=master)](https://travis-ci.org/ethlo/lamebda)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/598913bc1fe9405c82be73d9a4f105c8)](https://www.codacy.com/app/ethlo/lamebda?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ethlo/lamebda&amp;utm_campaign=Badge_Grade)
[![Coverage Status](https://coveralls.io/repos/github/ethlo/lamebda/badge.svg?branch=master&kill_cache=1)](https://coveralls.io/github/ethlo/lamebda?branch=master)

A simple, powerful, plugin system for adding new API endpoints or custom functionality to you Spring project. Oftentimes there are conflicting requirements from different clients utilizing a common main application. The required custom functionality needed is often easy to do with a few lines of code, but is not acceptable to add to your main code-base. With Lamebda you can load either precompiled or source files (Groovy/Java) for adding custom functionality.

> It Really Whips The Lambda's Ass!

## Example use-cases

* Ad-hoc API services and integration projects - Changes in the integration code can evolve freely from the core service
* Transactional support across multiple API calls
* Batch multiple API calls to avoid chatty data exchange
* API prototyping
* A powerful support tool for extracting data or changing state (think JMX on steroids)

## Getting started

### Integrating with your project

NOTE: If you want to try the bleeding edge, please add this snapshot repository
```xml
<repository>
  <id>sonatype-snapshots</id>     
  <snapshots><enabled>true</enabled></snapshots>
  <url>https://oss.sonatype.org/content/repositories/snapshots</url>
</repository>
```

#### Usage with Spring Boot and Spring MVC

Add dependency to your `pom.xml`
```xml
<dependency>
    <groupId>com.ethlo.lamebda</groupId>
    <artifactId>lamebda-spring-web-starter</artifactId>
    <version>0.9.4</version>
</dependency>
```

Add the following properties to `application.properties`:
```properties
lamebda.enabled=true
lamebda.root-directory=/var/lib/lamebda
lamebda.request-path=/gateway
```

### Project configuration
`project.properties`
* `project.name` - Human-readable name of the project. Optional.
* `project.base-packages` - The base packages that Spring IOC container is scanning

### Setup a project folder
Create a project folder. This folder is a logical grouping for your API functions. We will go with the create `test` for now. We now have the folder `/var/lib/lamebda/test`.

### Add your first script
1. Create a folder for the functions in the test project: `/var/lib/lamebda/test/src/main/groovy/com/acme`

2. Add a simple script in the scripts folder:

```groovy
@RestController
class MyController {
    @GetMapping("/my/{id}")
    def get(@PathVariable("id") int id) {
        return [requested: id]
    }
}
```

Please note that you can also write code in good ol' Java! Make sure you then put it in `src/main/java/com/acme`.

Add `com.acme` as base-package in your `project.properties`:
`project.base-packages=com.acme`

### Access your first function

Your function should be available under `/gateway/test/my/123`

### Built in functions

* /&lt;servlet&gt;/gateway/test/status/ - Simple status page showing loaded controllers
