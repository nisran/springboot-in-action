# Spring Batch

## 简介

一个轻量级，全面的批处理框架。

Features

- Transaction management
- Chunk based processing
- Declarative I/O
- Start/Stop/Restart
- Retry/Skip

**业务场景**

- 大量数据的自动化，负责处理。如：数据迁移并且进行处理

- 定期处理数据：各种报表下载
- 对对内部或外部产生的数据进行个格式化，校验，逻辑处理等。

spring batch本身不是一个调度系统，它旨在跟一些调度系统一起工作，如：Quartz。

## 核心概念

diagram of simple architecture。

<img src="https://static01.imgkr.com/temp/0868e725dedf4308a79ef235b5b8a43c.png" alt="Figure 2.1: Batch Stereotypes" style="zoom: 80%;" />



### Job

Job是一个处理任务的封装，通常一个Job的逻辑和配置会写在一起。一个Job是几个Step的简单容器，他会通过一定的逻辑组合最后的步骤，并配置相关的参数。

参数:

- Job name
- Step的定义以及循序
- Job时候可以重新启动。

Spring提供了一个 基于Java配置的`SimpleJob`实现类。

- JobInstance：JobInstance是每次逻辑运行的一个概念。按道理来说每次跑的都是新的一个JobInstance
- JobParameters：可以区分连个Job instance。

- JobExecution：一个execution可能成功或者失败，但是一个JobInstance不会认为陈工，除非完全成功。它还会又很多参数。

### Step

step是一个Job中单独且连续的阶段，所以每个Job可以分解成多个Step。一个Step包含完成业务逻辑及相应的参数。

- StepExecution：表示每此Step的尝试，
- ExecutionContext：ExecutionContext表示一组键/值对，这些键/值对由框架持久化和控制，以允许开发人员在一个位置存储持久状态，该持久状态的作用域是StepExecution对象或JobExecution对象

### JobRepository

JobReository是上面所说的所有的概念的持久化机制，提供CRUD操作。

### JobLauncher

JobLauncher represents a simple interface for launching a Job with a given set of JobParameters

### ItemReader

ItemReader是一个Step的入口，可以为Step持续的提供数据。它可以通过返回null来表达输入itemreader返回null，表示输入耗尽了。

ItemWriter

Itemreader是一个Step的输出抽象，表示批量或一块输出。

ItemProcessor

reader和writer之间的业务处理逻辑。

## 配置与启动

### Configuraon a Job

- Restartability：
- Intercepting Job Execution：
- JobParametersValidator

### Config a Step

- 面向块的处理：指ItemReader开始读取数据就开启一个chunk并开启事务，当当前chunk的数据处理完后一次性写入并关闭事务结束一个chunk。
    - 如：read A1，read A1，processA1，processA2，write [A1,A2]。<img src="https://static01.imgkr.com/temp/3feb208334394cce8dfad7c29be90ab4.png" alt="Chunk Oriented Processing With Item Processor" style="zoom: 80%;" />

- Commit Interval：提交隔离，事务的隔离数量就是chunk定义的数量
- Skip Logic：执行可能会抛出的异常，以及最大的异常tolerant，如果超过这个系统会报错。
- Retry Logic：如果同一个地方出现指定的异常，那可以进行重试，如果重试失败超过指定的N次，会直接抛异常或者进入skip逻辑。
- 控制多个Step的执行流程，默认是按序执行，可以添加一些条件。

