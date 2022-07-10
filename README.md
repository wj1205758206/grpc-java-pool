# grpc-java-pool
由于业务需求，上游模块需要根据不同的key，将流量转发到下游模块，gRPC底层使用http2，而http2是支持多路复用的，按理来说不需要额外的连接池，但是经过压测单个gRPC Client连接数有限，并发量过大，会导致CPU飙升，吞吐量下降，所以需要将gRPC Client池化，使用客户端连接池来提高吞吐量。基于common-pool2实现的gRPC客户端连接池，使用GenericKeyedObjectPool类来实现带key的gRPC Client连接池。
# pool整体架构设计
![image](https://user-images.githubusercontent.com/52147760/178135372-9d353c18-bbdf-4095-b3c2-9ad33a35f238.png)
# 池化对象Grpc Client
gRPC官网《最佳性能实践》一文中有明确提到：
- 尽可能重复使用stub和channel
- 每个 gRPC channel可以有 0 个或多个 HTTP/2 连接，每个连接通常对并发流的数量有限制。当连接上的活动 RPC 数量达到此限制时，额外的 RPC 会在客户端中排队，并且必须等待活动 RPC 完成才能发送。由于这种排队，具有高负载或长期流式 RPC 的应用程序可能会遇到性能问题。有两种可能的解决方案：
  1. 为应用程序中的每个高负载区域创建一个单独的通道
  2. 使用 gRPC channel池在多个连接上分发 RPC（通道必须具有不同的通道参数以防止重复使用，因此定义特定于使用的通道参数，例如通道号）

而Grpc Client实现则是封装了channel和stub，将Grpc Client最为连接池中的对象，实现复用。
由于需要上游模块需要根据不同key进行流量转发，需要对不同key最池化处理，基于common-pool2池化包，实现不同key的连接池。Grpc Pool相当于一个大池子，大池子里面按照key划分，每一个key又对应着不同的小池子，而小池子里面就是池化对象Grpc Client。

# 连接池扩缩容设计实现

- 空闲连接数

  连接池需要保持一定数量的空闲连接，无论这些连接是否被使用都会保留在池子中，随时等待请求的连接。相当于做了预热准备，防止系统一启动，大量请求要求建立连接。这里要考虑每个key对应的最大/最小空闲连接数。

  1. 设置每个key对应的最大空闲连接数：`setMaxIdlePerKey()`
  2. 设置每个key对应的最小空闲连接数：`setMinIdlePerKey()`

- 最大活跃连接数

  连接池的容量也是 有限的，不可能无限接收连接请求，我们需要设置整个池子的最大活跃连接数，以及每个key对应的最大活跃连接数，如果客户端请求超过次数，便需要根据实现的池满处理机制，对没有得到连接的请求进行处理。

  1. 设置整个池子的最大活跃连接数`setMaxTotal()`
  2. 设置每个key对应的最大活跃连接数`setMaxTotalPerKey()`

- 扩容机制

  整个池子预热了一些空闲连接，当有请求来时，如果空闲连接已经用光了，但是还没有达到最大活跃连接数(maxTotal和MaxTotalPerKey)，那么就可以创建新的连接服务该请求，新创建的连接在用完之后尝试放回池子中。

- 缩容机制

  如果某段时间内，请求数量减少了，池子中就会有大量的空闲连接，如果空闲连接的数量超过了设置的最大空闲连接数，那么就需要关闭一部分空闲连接。

![image](https://user-images.githubusercontent.com/52147760/178142960-2a872e8c-964a-47b7-80f4-8f9559c3db9b.png)


# 空闲连接的保活/超时清除

### 超时清除

当连接服务完请求之后就会归还到连接池中，变成空闲连接，如果一段时间内这个连接没有被再次使用，服务端就可能会根据自己的超时策略关闭这个空闲连接，那么此时池子中空闲连接就失效了，如果再有请求使用这个失效连接就会导致请求失败。所以我们从池子中获取连接时，首先要判断该连接是否失效了，如果没有失效才可以继续使用。

- Grpc Client

  `channel`设置`idleTimeout()`属性， 如果空闲超时，将断开所有连接和nameResolver、LB

- Grpc pool config

  - `setTestWhileIdle(true)`：开启检查空闲连接是否超时
  - `setTimeBetweenEvictionRunsMillis()`：设置驱逐线程执行间隔时间
  - `setNumTestsPerEvictionRun()`：设置每次驱逐线程清除空闲连接的个数
  - `setMinEvictableIdleTimeMillis()`：设置空闲连接被驱逐前能够保留的时间，有可能该空闲连接恰好被请求再次使用

### 保活

除了空闲超时会导致池子中的连接失效，还有就是如果gRPC Server端服务器重启，那么也会导致池子中的所有连接都会失效，如果这时候有请求从池子中获取连接，获取到的是已经失效的连接，就会导致请求失败。为了避免这种请求，我们需要考虑连接的保活问题。

从Grpc Client客户端角度实现保活机制，`channel`设置以下属性：

- `keepAliveTime()`： 设置`channel`保活 发送`PING`帧最小时间间隔，用来确定空闲连接是否仍然有效
- `keepAliveTimeout()`：超过设置的`KeepAliveTimeout`，将关闭该连接
- `keepAliveWithoutCalls(true)`：开启  即使没有请求进行，也可以发送keepalive ping
- `enableRetry()`： 开启重试机制，有可能网络拥塞问题导致失败，开始重启并设置重试次数
- `maxRetryAttempts()`：设置最大重试次数

# 池满处理机制设计与实现

前面说了连接池实现了扩容机制，但是不等无限扩容，当池子里的连接数达到了总的最大活跃连接数时，我们需要处理那些没有获取到连接的请求。

有两种解决方案：

1. 池满阻塞：

   连接池属性设置`setBlockWhenExhausted(true)`，当连接池满了之后，会阻塞当前获取连接的线程。同时还需要设置`setMaxWaitMillis()`最大阻塞等待时间，如果将会一直阻塞。

2. 抛异常：

   当池满时，我们可以手动抛出异常，根据具体的业务逻辑去处理，比如加入重试机制等。

