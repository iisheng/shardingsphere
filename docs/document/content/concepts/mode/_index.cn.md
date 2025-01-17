+++
pre = "<b>3.2. </b>"
title = "运行模式"
weight = 2
chapter = true
+++

## 背景

Apache ShardingSphere 是一套完善的产品，使用场景非常广泛。
除生产环境的集群部署之外，还为工程师在开发和自动化测试等场景提供相应的运行模式。
Apache ShardingSphere 提供的 2 种运行模式分别是单机模式和集群模式。

## 单机模式

能够将数据源和规则等元数据信息持久化，但无法将元数据同步至多个 Apache ShardingSphere 实例，无法在集群环境中相互感知。
通过某一实例更新元数据之后，会导致其他实例由于获取不到最新的元数据而产生不一致的错误。
适用于工程师在本地搭建 Apache ShardingSphere 环境。

## 集群模式

提供了多个 Apache ShardingSphere 实例之间的元数据共享和分布式场景下状态协调的能力。
在真实部署上线的生产环境，必须使用集群模式。它能够提供计算能力水平扩展和高可用等分布式系统必备的能力。
集群环境需要通过独立部署的注册中心来存储元数据和协调节点状态。 

**源码：https://github.com/apache/shardingsphere/tree/master/shardingsphere-mode**
