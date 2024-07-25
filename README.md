# mini-rpc
一个rpc框架，支持多种功能，并可灵活扩展

### 本项目参考：

1.guide哥的rpc框架（https://github.com/Snailclimb/guide-rpc-framework）

2.DerekYRC佬的mini-spring框架（https://github.com/DerekYRC/mini-spring）

3.马哥的Retryer框架（https://github.com/houbb/sisyphus）



### 学习这些项目的同时，扩展了额外的功能，如下：

1.由于mini-spring不支持扫描指定注解，因此修改了注册rpcService到ioc、zk的时机

2.通过SPI配置生成请求ID的方式，除了原有的UUID外，增加了改良版SeataSnowflake方式（改进有点可见Seata官网）

3.增加了多种容错策略（failover、failfast），通过@Reference注解指定，并可以灵活扩展

4.整合sisyphus-Retryer框架，支持多种超时重试策略（如固定间隔、指数增加间隔等），并可以灵活扩展

5.支持客户端本地缓存，采用定时+延迟获取混合的方式更新缓存

6.增加了轮询的负载均衡方式
