# StatsD
应用程序的监控是微服务中很重要的一环。监控主要包括四个方面的内容：指标（metrics）的采集、存储、展示以及相应的报警机制  
  
开源git地址   
github：https://github.com/statsd/statsd  
java客户端  
https://github.com/tim-group/java-statsd-client  
  
StatsD系统包括三部分：客户端（client）、服务器（server）和后端（backend）。  
  
客户端植入于应用代码中，将相应的metrics上报给StatsD server。statsd server聚合这些metrics之后，定时发送给backends。backends则负责存储这些时间序列数据，并通过适当的图表工具展示。  
简单举例，我们在代码中监听当日用户登录总数，可以设定用户登录的metrics标识位字符串login，然后调用StatsD的API持续对这个login标识进行累加（当然也支持设置具体值、减少、重置等其他操作），而StatsD服务端会对你所有的操作行为以及这些metrics标识值的变化进行记录
## 1. StatsD 简介
简单来讲，StatsD 就是一个简单的网络守护进程，基于 Node.js 平台，
通过 UDP 或者 TCP 方式侦听各种统计信息，包括计数器和定时器，并发送聚合信息到后端服务，如 Graphite。  

StatsD 最初是由 Etsy 的 Erik Kastner 写的提供 Graphite/Carbon 指标的前端代理，
初衷是为了汇总和分析应用指标。它基于两大功能：计数和计时。最开始使用 Node，
后来也实现了其他语言。通过 Statsd ，能通过特定语言的客户端检测应用程序的指标。
基于个性化需求，可以通过 Statsd 收集任何想要的数据。Statsd 通过发送 UDP 数据包来调用每个
 Statsd 服务器，下面我们来了解一下为什么选择 UDP 而不是 TCP。  
  
为什么使用 UDP?  
  
前面也说了, StatsD 是通过 UDP 传输数据的，那么有人会问为什么选 UDP 而不选 TCP 呢? 
首先，它速度很快。任何人都不想为了追踪应用的表现而减慢其速度。此外，
UDP 包遵循「fire-and-forget」机制。所以要么 StatsD 接收了这个包，要么没有。
应用不会在意 StatsD 是运行、宕机还是着火了，它单纯地相信一切运行正常。
也就是说我们不用在意后台 StatsD 服务器是不是崩了，就算崩了也不会影响前台应用。
（当然，我们可以通过图表追踪 UDP 包接收失败的情况。）  

## 2. StatsD 的一些概念
### 2.1 Buckets
当一个 Whisper 文件被创建，它会有一个不会改变的固定大小。在这个文件中可能有多个 buckets 对应于不同分辨率的数据点，每个 bucket 也有一个保留属性指明数据点应该在 bucket 中应该被保留的时间长度，Whisper 执行一些简单的数学计算来计算出多少数据点会被实际保存在每个 bucket 中。

### 2.2 Values
每个 stat 都有一个 value，该值的解释方式依赖于 modifier。通常，values 应该是整数。

### 2.3 Flush Interval
在 flush interval (冲洗间隔，通常为10秒) 超时之后，stats 会聚集起来，传送到下游的后端服务。  
  
追踪所有事件是提高效率的关键。有了 StatsD，工程师们可以轻松追踪他们需要关注的事务，而无需费时地修改配置等。

### 2.4 statsd协议
statsd采用简单的行协议，例如：  
```
<bucket>:<value>|<type>[|@sample_rate]
```
* bucket：是一个metric的标识，可以看成一个metric的变量。
* value：metrics值，通常是数字。
* type：metric的类型，通常有timer、counter、gauge和set四种。
* sample_rate：采样率，降低客户端到statsd服务器的带宽。客户端减少数据上报的频率，然后在发送的数据中加入采样频率，如0.1。statsd server收到上报的数据之后，如cnt=10，得知此数据是采样的数据，然后flush的时候，按采样频率恢复数据来发送给backend，即flush的时候，数据为cnt=10/0.1=100，而不是容易误解的10*0.1=1。

### 2.5 UDP 和 TCP：
statsd可配置相应的server为UDP和TCP（默认为UDP）。UDP和TCP各有优劣，但UDP确实是不错的方式。
* UDP不需要建立连接，速度很快，不会影响应用程序的性能。
* “fire-and-forget”机制，就算statsd server挂了，也不会造成应用程序crash。  
  
当然，UDP更适合于上报频率比较高的场景，就算丢几个包也无所谓，对于一些一天已上报的场景，任何一个丢包都影响很大。另外，对于网络环境比较差的场景，也更适合用TCP，会有相应的重发，确保数据可靠。  

### 2.6 metric类型
#### 2.6.1 counter
用来累加计数，值可以是正数或者负数。在一个flush区间，把上报的值累加，flush后会清零。
```
user.logins:10|c        // user.logins + 10
user.logins:-1|c        // user.logins - 1 
user.logins:10|c|@0.1   // user.logins + 100
                        // users.logins = 10-1+100=109
```
#### 2.6.1 timer
timers用来记录一个操作的耗时，单位ms。
1) statsd会记录平均值（mean）、最大值（upper）、最小值（lower）、累加值（sum）、平方和（sum_squares）、个数（count）以及部分百分值。
```
rpt:100|g
```
如下是在一个flush期间，发送了一个rpt的timer值100。以下是记录的值。
```java
count_80: 1,    
mean_80: 100,
upper_80: 100,
sum_80: 100,    
sum_squares_80: 10000, 
std: 0,     
upper: 100,
lower: 100,
count: 1,
count_ps: 0.1,
sum: 100,
sum_squares: 10000,
mean: 100,
median: 100
```
2) percentThreshold
对于timer数据，除了正常的统计之外还会计算一个百分比的数据（过滤掉峰值数据），默认是90%。可以通过percentThreshold修改这个值或配置多个值。例如在config.js中配置：
```shell
//分别计算50%和80%的相关值
percentThreshold: [50, 80]
```
对于百分数相关的数据需要解释一下。以90为例，statsd会把一个flush期间上报的数据，去掉10%的峰值，即按大小取cnt*90%（四舍五入）个值来计算百分值。假如10s内上报以下10个值：
```shell
1,3,5,7,13,9,11,2,4,8
```
则只取10*90%=9个值，则去掉13。百分值即按剩下的9个值来计算
```shell
$KEY.mean_90   // (1+3+5+7+9+2+11+4+8)/9
$KEY.upper_90  // 11
$KEY.lower_90  // 1
```
3）直方图
有时仅记录操作的耗时并不能让我们很好的知道当前系统的情况，通常，timing都是跟histogram一起来使用的。在config.js配置文件中设置：
```shell
histogram: [ { metric: '', bins: [10, 100, 1000, 'inf']} ]
```
这样就开启了histogram，这个histogram的bin的间隔是[0, 10ms)，[10ms - 100ms), [100ms - 1000ms), 以及[1000ms, +inf)，如果一个timing落在了某个bin里面，相应的bin的计数就加1，譬如：
```shell
foo:1|ms
foo:100|ms
foo:1|ms
foo:1000|ms
```
那么最终statsd最终flush输出时：
```shell
histogram: { bin_10: 2, bin_100: 0, bin_1000: 1, bin_inf: 1 } } },
```
注：没有p99等指标。
#### 2.6.2 gauge
gauge是任意的一维标量值（如：内存、身高等值）。gague值不会像其它类型会在flush的时候清零，而是保持原有值。statsd只会将flush区间内最后一个值发到后端。另外，如果数值前加符号，会与前一个值累加。
```shell
age:10|g    // age 为 10
age:+1|g    // age 为 10 + 1 = 11
age:-1|g    // age为 11 - 1 = 10
age:5|g     // age为5,替代前一个值
```
注：gauge通常是在client进行统计好在发给StatsD的，如capacity:100|g 这样的gauge，即使我们发送多次，
在StatsD里面，也只会保存100
#### 2.6.2 sets
记录flush期间，不重复的值。可以用来计算某个metric unique事件的个数，譬如对于一个接口，可能我们想知道有多少个user访问了，我们可以这样:
  
StatsD就会展示这个request metric只有1，2两个用户访问了。
```
request:1|s  // user 1
request:2|s  // user1 user2
request:1|s  // user1 user2
```
## 3. 安装&使用
### 3.1-1 安装-普通
在centos7上通过源码安装statsd（也可以根据github上的说明，使用docker安装）  
docker镜像：docker pull statsd/statsd  
1） 需要安装nodejs和git环境（自己百度），最新的statsd使用了ES6的语法，所以需要至少node6.0以上的版本。
2） 下载、配置、启动：
```jshelllanguage
#下载
cd /usr/local
git clone https://github.com/statsd/statsd.git
cd statsd
cp exampleConfig.js config.js
 
#修改config.js
#...
 
#启动
node /usr/local/statsd/stats.js /usr/local/statsd/config.js
# 后台启动
node /usr/local/statsd/stats.js /usr/local/statsd/config.js >/usr/local/statsd//mylogs/statsd.log &
```
启动结果：
```
[root@node1 statsd]# node /usr/local/statsd/stats.js /usr/local/statsd/config.js
8 Apr 21:55:53 - [14489] reading config file: /usr/local/statsd/config.js
8 Apr 21:55:53 - server is up INFO
```
### 3.1-2 docker
```shell
docker run -d \
    -p 8125:8125/udp \
    -p 8126:8126 \
    -v /home/statsd/config.js:/usr/src/app/config.js \
    statsd
```
### 3.2 配置
statsd提供默认的配置文件exampleConfig.js。可以参考相应的注释按需配置，接下来将简单介绍一些配置项。  
* 将控制台作为后台接收数据并输出的插件
```jshelllanguage
{
  port:8125,//statsd 服务端口
  backends:["./backends/console"],//控制台插件
  console:{
    prettyprint:true
  },
  flushInterval:30000,//statsd flush时间
  percentThreshold: [80,90]
}
```
注：在statsd目录下，backends中包含了默认的后端：graphite和console。
### 3.3 使用-控制台
配置statsd的backends问console进行调试，启动statsd。然后使用netcat发送数据进行测试：
```jshelllanguage
echo "test:9|ms" | nc -w 1 -u 127.0.0.1 8125
```
注意：netcat -w参数表示超时时间，单位是秒。  
  
根据配置的flushinterval，statsd服务端会定时的将聚合好的数据发送到backends，如果配置的后端是console，则会输出：
```jshelllanguage
Flushing stats at  Thu Apr 08 2021 22:12:03 GMT+0800 (China Standard Time)
{ counters:
   { 'statsd.bad_lines_seen': 0,
     'statsd.packets_received': 1,
     'statsd.metrics_received': 1 },
  timers: { test: [ 9 ] },
  gauges: { 'statsd.timestamp_lag': 0 },
  timer_data:
   { test:
      { count_80: 1,
        mean_80: 9,
        upper_80: 9,
        sum_80: 9,
        sum_squares_80: 81,
        count_90: 1,
        mean_90: 9,
        upper_90: 9,
        sum_90: 9,
        sum_squares_90: 81,
        std: 0,
        upper: 9,
        lower: 9,
        count: 1,
        count_ps: 0.03333333333333333,
        sum: 9,
        sum_squares: 81,
        mean: 9,
        median: 9 } },
  counter_rates:
   { 'statsd.bad_lines_seen': 0,
     'statsd.packets_received': 0.03333333333333333,
     'statsd.metrics_received': 0.03333333333333333 },
  sets: {},
  pctThreshold: [ 80, 90 ] }
```
### 3.3 使用-Graphite
* 将graphite作为后台接收数据并输出的插件
```js
{
  port: 8125,
  graphitePort: 2003,
  graphiteHost: "graphite.example.com",
  backends: [ "./backends/graphite" ]
}
```
* 部署Graphite，这里采用docker-compose快速部署
```jshelllanguage
version: '2'
services:
    grafana_graphite:
        image: nickstenning/graphite
        container_name: kamon-grafana-dashboard
        ports:
          - '8880:80'
          - '2003:2003'
          - '2004:2004'
          - '2023:2023'
          - '2024:2024'
          - '3125:8125/udp'
          - '3126:8126'
```
前端页面访问
http://127.0.0.1:8880
帐号密码都是admin
### 3.4 使用:docker-statsd-influxdb-grafana 
帖子参考：https://blog.csdn.net/shimodocs/article/details/53693847  
上面的statsd和Graphite都不用了，直接使用这个docker镜像就可以
```jshelllanguage
version: '2.2'
services:
  statsddocker:
    image: samuelebistoletti/docker-statsd-influxdb-grafana:latest
    container_name: statsddocker
    restart: always
    ports:
      - '3000:9000'
      - '8083:8083'
      - '8086:8086'
      - '3003:3003'
      - '22022:22'
      - '3004:8888'
      - '8125:8125/udp' 
```
* 登录influxDB  
浏览器打开： http://你的ip:3004。  
添加 username 和 password 都为 datasource  ，Database Name:telegraf  
在Kapacitor配置时间只用配置Username为datasource，不用配置密码  
  
注意：用户名和密码这里先填 datasource，后面会说明。此外，这个 docker 镜像自动为我们在 InfluxDB 创建了名为 datasource 的 db。  
* 登录grafana  
览器打开： http://127.0.0.1:3003  
默认帐号密码：root/root  
点击左上角图标 -> Data Source -> Add data source，进入配置数据源页面，如下填写，点击 Save：  
url 中替换成你分配的 ip。  

### 3.5 JAVA API
注意：服务器要给开通udp端口号，特别是云服务器，一定要主要是UDP端口，开TCP没用
```jshelllanguage
    <dependencies>
        <dependency>
            <groupId>com.timgroup</groupId>
            <artifactId>java-statsd-client</artifactId>
            <version>3.0.1</version>
        </dependency>
    </dependencies>
```
```jshelllanguage
package com.owp;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

public class Foo {
    private static final StatsDClient statsd = new NonBlockingStatsDClient("demo3","47.106.135.182", 8125);

    public static final void main(String[] args) {
    while (true){
        statsd.recordGaugeValue("baz2", 100);
        statsd.recordExecutionTime("bag2", 25);
        statsd.recordSetEvent("qux2", "one");
    }
    }
}
```




