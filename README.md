# grpc-java-pool
由于业务需求，上游模块需要根据不同的key，将流量转发到下游模块，gRPC底层使用http2，而http2是支持多路复用的，按理来说不需要额外的连接池，但是经过压测单个gRPC Client连接数有限，并发量过大，会导致CPU飙升，吞吐量下降，所以需要将gRPC Client池化，使用客户端连接池来提高吞吐量。基于common-pool2实现的gRPC客户端连接池，使用GenericKeyedObjectPool类来实现带key的gRPC Client连接池。
# pool整体架构设计
![image](https://user-images.githubusercontent.com/52147760/178135372-9d353c18-bbdf-4095-b3c2-9ad33a35f238.png)
# 池化对象Grpc Client
gRPC官网《最佳性能实践》一文中有明确提到：
- 尽可能重复使用stub和channel
-每个 gRPCchannel可以有 0 个或多个 HTTP/2 连接，每个连接通常对并发流的数量有限制。当连接上的活动 RPC 数量达到此限制时，额外的 RPC 会在客户端中排队，并且必须等待活动 RPC 完成才能发送。由于这种排队，具有高负载或长期流式 RPC 的应用程序可能会遇到性能问题。有两种可能的解决方案：
  1. 为应用程序中的每个高负载区域创建一个单独的通道
  2. 使用 gRPC channel池在多个连接上分发 RPC（通道必须具有不同的通道参数以防止重复使用，因此定义特定于使用的通道参数，例如通道号）

而Grpc Client实现则是封装了channel和stub，将Grpc Client最为连接池中的对象，实现复用。
由于需要上游模块需要根据不同key进行流量转发，需要对不同key最池化处理，基于common-pool2池化包，使用不同key的连接池。Grpc Pool相当于一个大池子，大池子里面按照key划分，每一个key又对应着不同的小池子，而小池子里面就是池化对象Grpc Client。
