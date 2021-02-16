# <img src="src/docs/asciidoc/images/spring-framework.png" width="80" height="80"> Spring Framework [![Build Status](https://build.spring.io/plugins/servlet/wittified/build-status/SPR-PUBM)](https://build.spring.io/browse/SPR)

This is the home of the Spring Framework: the foundation for all [Spring projects](https://spring.io/projects). Collectively the Spring Framework and the family of Spring projects are often referred to simply as "Spring". 

Spring provides everything required beyond the Java programming language for creating enterprise applications for a wide range of scenarios and architectures. Please read the [Overview](https://docs.spring.io/spring/docs/current/spring-framework-reference/overview.html#spring-introduction) section as reference for a more complete introduction.

## Code of Conduct

This project is governed by the [Spring Code of Conduct](CODE_OF_CONDUCT.adoc). By participating, you are expected to uphold this code of conduct. Please report unacceptable behavior to spring-code-of-conduct@pivotal.io.

## Access to Binaries

For access to artifacts or a distribution zip, see the [Spring Framework Artifacts](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-Artifacts) wiki page.

## Documentation

The Spring Framework maintains reference documentation ([published](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/) and [source](src/docs/asciidoc)), Github [wiki pages](https://github.com/spring-projects/spring-framework/wiki), and an
[API reference](https://docs.spring.io/spring-framework/docs/current/javadoc-api/). There are also [guides and tutorials](https://spring.io/guides) across Spring projects.

## Build from Source

See the [Build from Source](https://github.com/spring-projects/spring-framework/wiki/Build-from-Source) Wiki page and the [CONTRIBUTING.md](CONTRIBUTING.md) file.

## Stay in Touch

Follow [@SpringCentral](https://twitter.com/springcentral), [@SpringFramework](https://twitter.com/springframework), and its [team members](https://twitter.com/springframework/lists/team/members) on Twitter. In-depth articles can be found at [The Spring Blog](https://spring.io/blog/), and releases are announced via our [news feed](https://spring.io/blog/category/news).

## License

The Spring Framework is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).

## spring解决循环引用

### 三级缓存解决循环依赖问题的关键是什么？为什么通过提前暴露对象能解决？

实例化和初始化分开操作，在中间过程中给其他对象赋值的时候，并不是一个完整的对象，而是把半成品对象赋值给了其他对象

> 半成品：完成实例化但未完成初始化的bean对象

### 如果只使用一级缓存，能不能解决问题？
不能，因为我们在整个处理过程中，缓存中放的是半成品和成品对象，如果只有一级缓存，那么成品半成品都会放在到一级缓存中，有可能在获取过程中获取到半成品对象，
此时半成品的对象是无法使用的，不能直接进行相关的处理，因此要把半成品和成品的存放空间分割开来。
### 只使用二级缓存行不行？为什么需要三级缓存？
在使用中，三级缓存比二级缓存多了一个() ->getEarlyBwanReference(beanName)的处理过程。
如果能保证所有的bean都不去调用getEarlyBwanReference此方法，使用二级缓存可以吗？
是的，如果保证所有的bean对象都不调用此方法，可以只是用二级缓存！

使用三级缓存的本质在于解决AOP代理问题！！！

### 如果某个bean需要代理对象，那么会不会创建普通的bean对象。
会，需要创建普通对象。

### 为什么使用了三级缓存，就能解决AOP代理问题？
当一个bean对象需要代理时，在整个创建过程中会创建两个对象。一个是普通对象，一个是代理生成的对象。
bean默认对象和代理对象都是单例的，那么在整个bean生命周期的处理环节中，因为一个beanName只能对应一个对象，所以必须保证在使用这些对象的时候加一层判断，
判断是否需要进行代理的处理 

### 怎么知道什么时候使用普通bean对象还是代理对象？
所以需要通过一个匿名内部类的方式，在使用的时候直接对普通对象进行覆盖操作，保证全局唯一！
也就是通过一个匿名内部类，保证什么时候用就什么时候调代理，把普通对象覆盖

### 总结

- 一级缓存放成品对象
- 二级缓存放半成品对象
- 三级缓存放lamda表达式，来完成代理对象的覆盖过程
