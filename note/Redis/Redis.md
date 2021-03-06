## Redis前置常识

磁盘，寻址速度，ms级别

内存，寻址速度，ns级别，速度是磁盘的10W倍

IO buffer，磁盘扇区512byte，如果一次读取数据过小，索引成本变高，如果一次读取数据过大，容易造成浪费，操作系统中，缓存行大小为4k。

数据库，索引都是存在于磁盘中，关系型数据库倾向于行级存储，在数据插入之时，已经给这行数据分配好空间，之后增删改只需要修改数据，不用移动其他数据了。在内存中存在B+T，查询数据时，先找出相应的4k的索引，再通过索引找到4k的数据。

数据库，表数据量很大，性能会不会下降？如果表有索引，增删改速度虽然会变慢，但是查询速度，如果是查询一条的话，速度依然不会变慢，如果并发大的情况下，会受硬盘带宽影响。

JVM一个线程的成本1MB，线程如果太多会造成内存成本过高，以及CPU调度成本过高。

## Redis的引入

由于内存的速度比磁盘快得多，纯内存数据库(SAP-NANA)价格异常昂贵，所以有了折中方案-缓存

2个基础设施的考虑

冯诺伊曼体系的硬件

以太网，TCP/IP的网络

## Memcached和Redis

同样作为key-value的缓存数据库，memcached的value没有类型的概念，以json来表示各种不同的数据结构

redis的value有数据类型的好处：

如果client需要取回v中的一个值，memcache需要返回所有的value到client，由client去解析，而在redis中，对每种数据类型有相应的方法，符合<b>计算向数据移动</b>



# Redis

## 安装

```bash
# 下载redis
yum install wget -y
mkdir soft
cd soft
wget http://download.redis.io/releases/redis-5.0.5.tar.gz
tar xf redis-5.0.5.tar.gz
cd redis-5.0.5
# 安装redis
yum install gcc -y   # 安装编译环境，make命令需要
make distclean	# 如果安装gcc之前执行了make，需要执行此命令删除一些临时文件
make	# Linux操作系统自带的编译命令，相同于javac，但是不是针对一种语言，用Makefile去做编译，Makefile由厂商提供
cd src  # 此时src目录下，已经有可执行程序了，redis-cli, redis-server，可以./redis-server启动，显然这种不是我们期望的启动方式
cd ..
make install PREFIX=/opt/qinsheng/redis5 # install执行安装，将可执行文件迁移到指定目录，和源码不用放在一起了，但是还是需要通过./redis-server启动，如果希望用  service redis start启动，需要继续如下操作
cd ~/soft/redis-5.0.5/utils # 运行install_server.sh之前，需要设置redis_home和添加path
vi /etc/profile # 在最后加上以下内容
	export REDIS_HOME=/opt/qinsheng/redis5
	export PATH=$PATH:$REDIS_HOME/bin
source /etc/profile	# 使profile文件生效， 这样在任何地方都可以执行redis-cli命令了
./install_server.sh	# 一个物理机上可以多次执行，生成多个redis进程，使用不同的端口区分
	# 可执行文件就一份在目录，但是内存中未来的多个实例需要各自的配置文件，持久化目录等资源
	# 现在在/etc/init.d/下，有redis_6379脚本，并且是绿色可执行，可以用service redis_6379 start/stop/status进行操作了
ps -fe | grep redis
```

![微信图片_20200716002846](images/20200716002846.png)

## 远程连接

环境：Mac一台，Windows10一台，IP地址为192.168.31.127，虚拟机Linux CentOS6安装在Windows10中，IP为192.168.169.15

从Mac中连接虚拟机中的Redis服务，需要进行如下操作：

1，关闭Windows及虚拟机防火墙。

2，在VMWare中，对NAT网络服务端口进行映射，redis服务端口为6379，需要增加映射：6379 - 192.168.169.15：6379

3，更改Redis.conf文件，将bind 127.0.0.1注释，并将protected-mode设置为no。这里需要注意的是，修改配置文件并不能使已经创建好的server生效，如果已经创建了redis_6379，即使重启也不会生效，需要删除server脚本，重新生成。

在完成以上步骤后，Mac中可以通过访问redis-cli -h 192.168.31.127 -p 6379进行访问。

## 概念

Redis是单进程，单线程，单实例，使用epoll来高效处理高并发的请求，同时由于redis是单线程的，可以保证每个连接内的命令是有顺序的。Redis是单进程，但可以启动多个Redis进程，每个Redis中默认有16个库，可以进行配置，每个库中存储的是key-value，Redis的value有五个数据类型，且对每种类型都有各自不同的方法，如果用非string的方法访问string，直接type判断时就会报错。

### 二进制安全

在 Redis 中，如果存一个 `中`字，由于客户端编码不同，可能占 3 个字节，也可能占两个字节。存一个 `10` 占用 2 个字节，存 `100` 要占 3 个字节。这是因为 Redis 用了二进制安全的技术，不将数据进行任何转换，以二进制的方式来什么存什么。为此，同一个 Redis 服务的客户端要统一编码格式，不然数据的读和存会有不一致的问题。

### 正反向索引

```md
 0  1  2  3  4
 h  e  l  l  o
-5 -4 -3 -2 -1
```

### 字符集

标准的叫ASCII字符集，其他的都叫做扩展字符集，其他的字符集不会再对ASCII进行重编码，0xxxxxxx。

如果第一个位是0，就不用考虑是不是其他字符集了，直接就是ASCII码

如果第一个位不是0，如果是UTF-8，不是0开头，是3个1和1个0，那么这上字符就是3个字节的，除了当前字节，还需要再读取两个字节，去掉前面的3个1和1个0，再进行显示。

## 五大数据类型

### String

对于五大数据类型中最常用的String类型，又可以分为三种，分别是字符串，数值，bitmap

#### 字符串

```bash
# 基本命令
set k1 hello
get k1
append k1 " world"
getrange k1 6 10	# 数值是k1的偏移量，取第6到第10位
setrange k1 6 redis	# 从第6位开始，后面的替换成redis
strlen k1
type k1 -> string	# 取得数据类型

mset k2 bbb k3 ccc # 同时设置2组key-value
mget k2 k3
set k1 aaa nx # 如果k1已经存在，不会更新k1的值，如果不存在，添加k1-aaa键值对
set k5 bbb xx # 如果k5已经存在，更新k5的值，如果不存在，不会添加
mset k1 aaa k2 bbb nx # 原子操作
getset k1 abc # 返回k1的值，并重新设值
```

#### 数值

```bash
# 数值基本命令
set k1 99	# k1=99
incr k1	# k1=100
incrby k1 25 # k1=125
decr k1 # k1=124
decrby k1 25 # k1=99
incrbyfloat 0.5 # k1=99.5

# 关于redis的存储，是二进制安全的
set k2 9
strlen k2 -> 1
append k2 999 # k2=9999
strlen k2 -> 4

set k3 中 # utf-8
strlen k3 -> 3
set k4 中 # gbk
strlen k4 -> 2

redis-cli --raw # utf-8
get k3 -> 中
get k4 -> 乱码	# redis的存储和读取需要统一编码
```

#### bitmap

```bash
#	  0号字节   1号字节	偏移量以位进行计算
#	   	0					1
#  00000000	00000000
setbit k1 1 1 # 01000000
get k1 # 返回@
setbit k1 7 1 # 01000001
get k1 # 返回A
strlen k1 # 1，只有一个字节
setbit k1 9 1 # 01000001 01000000
get k1 # 返回A@
strlen k1 # 2，现在有两个字节了

bitcount k1 0 -1 # 3，0到-1的字节里有3个1
bitpos k1 1 1 1 # 9，第1到第1个字节中，其实就是第1个字节，第一个1的偏移量，01000001 01000000，就是最后一个1，偏移量9

# bitop 对于二进制的运算
setbit k1 1 1 # 01000000
setbit k1 7 1 # 01000001
setbit k2 1 1 # 01000000
setbit k2 6 1 # 01000010
bitop add addkey k1 k2 # k1 k2进行与运算，结果就是01000000，
get addkey # 返回@
```

##### 应用场景 - 位图

统计用户登录天数，且窗口随机

```bash
setbit sean 1 1	# 第2天登录
setbit sean 7 1 # 第8天登录
setbit sean 364 1	# 第365登录
strlen sean
bitcount sean -2 -1 # 计算sean最后16天的登录次数
```

618做活动送礼物，计算需要备多少货

```bash
setbit 20200101 1 1	# 2020年1月1号，1号位代表的用户登录了
setbit 20200102 1 1 # 2020年1月2号，1号位代表的用户登录了
setbit 20200102 7 1 # 2020年1月2号，7号位代表的用户登录了
bitop or orkey 20190101 20190102	# 给这两天做或运算，就能计算出两天登录的人的数量，01000001
bitcount orkey 0 -1	# 计算有几人登录，返回2.
```

### List

Redis中的链表操作，分为左右，可以通过不同的排列组合，模拟出不同数据结构

```bash
# List基本命令
# 同向命令，先进后出，栈
lpush k1 a b c d e f  # 从左边插入，这在内存里应该是 f e d c b a
lpop k1	# 从左边取出，返回f
# 反向命令，先进先出，队列
lpush k2 a b c d e f 
rpop k2 # 从右边取出，返回a
# 数组操作
lrange k1 0 -1 # 取回从0到-1，即所有的元素
lindex k1 2 # 返回第3个元素
lindex k1 -2 # 返回倒数第2个元素
lset k1 2 xxx # 将k1第3个元素更新为xxx
lrem k1 2 a # 移除前2个a
lrem k1 -2 a # 移除后2个a
linsert k1 before/after c g # c代表k1中的元素， g代表插入的元素，在k1的c前面/后面插入g，这里的c是第一个c元素
llen k1 # 元素长度
# 阻塞，单播队列
blpop xxx 0 # Client1：0代表阻塞，如果是数值则是设置时间，此时窗口会一直阻塞，直到有另外的redis进程，给xxx赋值
blpop xxx 0 # Clinet2：同Client1，测试是否FIFO
rpush xxx hello # Client3，给xxx赋值，此时Client1获得xxx的值，退出，Client2继续阻塞
rpush xxx world # Client3，给xxx赋值，此时Client2获得xxx的值，退出
# 删除元素命令
ltrim k1 0 -1 # 一个不会删除
ltrim k1 2 -2 # 删除0，1，-1三个元素
```

![image-20200720001904216](images/image-20200720001904216.png)

![image-20200720002142102](images/image-20200720002142102.png)

![image-20200720002937214](images/image-20200720002937214.png)

### Hash

Redis是键值对的数据类型，这里的值是hash类型，即值也是键值对，如果需要保存一个用户的信息，用String来保存可能是以下命令，过于复杂和成本过高，从而引入了hash类型。

```bash
set sean::name 'zhangsan'
set sean::age '18'
set sean::address 'shanghai'
# 在取值时，需要先取得sean的属性集合，再通过属性集合取得属性值
keys sean*	# 通过sean*模糊匹配的成本高
mget key...	# 需要多一次查询
```

```bash
# hash基本命令
hset sean name 'zhangsan'
hset sean age '18'
hset sean address 'shanghai'
hmset sean name 'zhangsan' age '18' address 'shanghai' # 一个命令给三个属性赋值，和String的mset一样
# 取值
hkeys sean	# 取所有的键
hvals sean	# 取所有的值
hgetall sean	# 取所有的键值
# 计算	应用场景-商品详情页，微博中的点赞数，收藏数，等等
hincrbyfloat sean age 0.5	# 增加年龄0.5岁，hash没有decr方法，如需要减，加上负值即可
hincrbyfloat sean age -1
```

![image-20200720010714579](images/image-20200720010714579.png)

### Set

#### 无序&去重

```bash
# set基本命令
sadd k1 aa bb aa ee ff kk ll ii	# set会去重保存
smembers k1 # 取得k1所有元素，这命令不般不使用，会占用较大带宽，影响redis的吞吐量
```

#### 交集，并集，差集

```bash
# set的重要命令，取两个集合交集，并集，差集
sadd k2 1 2 3 4 5
sadd k3 4 5 6 7 8
sinter k2 k3 # 返回交集4，5
sinterstore destkey k2 k3 # 将k2,k3的交集结果保存在服务端，key为destkey，值是4，5
sunion k2 k3 # 返回并集1，2，3，4，5，6，7，8
sunionstore unionkey k2 k3 # 同理，将k2,k3的并集保存在服务端
sdiff k2 k3 # 返回1，2，3 差集跟顺序有关
sdiff k3 k2 # 返回6，7，8
```

![image-20200720011818480](images/image-20200720011818480.png)

#### 随机事件

##### 应用场景 - 抽奖

```bash
# set的另一个重要命令，随机事件，
# srandmember key count，
# count如果是正数，取出一个去重的结果集，不会超过已有集
# count如果是负数，取出一个带重复的结果集，一定满足要求的数量，随机补上集合里元素
# count如果是0，不返回
sadd users user1 user2 user3 user4 user5 user6 user7 # 增加七位用户
srandmember users 5 # 随机5位，不会重复
srandmember users 10 # 7位无序取出
srandmember users -5 # 随机5位，可能重复
srandmember users -10 # 随机10位，可能有某一些元素没有出现

# spop命令，随机取出一个，并从集合中移除，这个更符合年会抽奖
spop users # 随机弹出一个，集合长度变为6
scard users # 集合长度，返回6
```

![image-20200720013048825](images/image-20200720013048825.png)

### Sorted-Set

#### 有序&去重

因为Sorted-Set是有序集合，所有在插入时，都必须有一个权重，用来进行排序的一个标准。

```bash
# sorted-set基础命令
zadd k1 8 apple 2 blanana 3 orange	# 插入3种水果，分别给出权重数值
zrange k1 0 -1	# 取出k1的元素，返回blanana orange apple，按权重从小到大
zrangebyscore k1 3 8 withscores	# 取出权重在3-8之间的
zrange k1 0 1	# 取出第1到第2个元素 
zrevrange k1 0 1	# 从大到小排序，取出第1到第2个元素
zrange k1 -1 -2	# 取不到值
zrange k1 -2 -1 # 从小到大排序，取出倒数第2到倒数第1个元素
```

#### 更新权重重新排序

##### 应用场景 - 歌曲排序榜

```bash
zincrby k1 2.5 blanana # 给blanana加权重2.5，这样集合顺序也会发生变化，变成orange blanana apple
```

在歌曲排序榜中，有很多人去更新数据，由于redis的单线程，不用担心事务的处理，在更新权重过程中，速度远高于关系型数据库。

![image-20200720022604273](images/image-20200720022604273.png)

#### 交集，并集，差集

Sorted-Set也是Set，只不过是排序集合，所有同样也有交集，并集，差集，比普通Set复杂的是，在做交集过程中，权重如何计算，Redis中，多个集合都有的元素在做交集时，权重可以按比计算，同时可以取Sum，Max或Min。

```bash
# sorted-set的重要命令，取两个集合交集，并集，差集，并计算权重
# 增加两个有序集合
zadd k1 80 tom 60 sean 70 baby
zadd k2 60 tom 100 sean 40 ethan
# 取两个有序集合的交集
# 下面这条命令：取k1,k2的交集，保存在unkey中，k1的权重按1计算，k2的权重按0.5计算，最后按较大的权重进行排序
# 最后得到的结果是 
# ethan 20, 只在k2集合里有，且按0.5计算权重
# sean 60, k1,k2中都有，k1权重60，k2权重100按0.5算，取最大值是60
# baby 70, 只在k1中有，按1计算
# tom 80, k1,k2中都有，k1权重80，k2权重60按0.5算，取最大值是80
zunionstore unkey 2 k1 k2 weights 1 0.5 aggregate max 
```

![image-20200720023751888](images/image-20200720023751888.png)

#### 跳跃表 - Skiplist

在更新了权重值，或是集合的交并差运算后，如何能再次进行快速排序，如何实现的

这里引入了跳跃表的概念，

粗率解释：原本有一个集合3，7，22，45，在集合的上方增加了一层集合，3，22，45，再上一层是3然后指向了nil，如果需要插入11，则11去和3进行比较，比3大，然后再往下和3指向的元素进行比较，这里比22小，再往下和22左边的元素进行比较，比7大，然后进行指针修正，22左边的指针由7改成11，7右边的指针由22改成11，11的前后指针分别指向7，22。

在11插入后，会随机对11进行造成，可能是1也可能是3，图中所示就是造3层，会从左边元素进行查找，7只有一层，继续找到3，在2层中将3右边的指针从22改成11，同时将11的前后指针指向3和22，以此类推。

经过测试，增删改查综合下来，跳跃表这种数据结构相对最优

![image-20200720024228992](images/image-20200720024228992.png)

## Redis进阶

### 管道

Redis是一种基于客户端-服务端模型以及请求/响应协议的TCP服务。

这意味着通常情况下一个请求会遵循以下步骤：

- 客户端向服务端发送一个查询请求，并监听Socket返回，通常是以阻塞模式，等待服务端响应。
- 服务端处理命令，并将结果返回给客户端。

一次向redis服务器发送多个命令，减少连接，使通信成本降低

```bash
echo -e "set k2 99\nincr k2\nget k2" | nc localhost 6379	# 同时向服务端发送三条命令，由换行符\n分开
```

**重要说明**:  使用管道发送命令时，服务器将被迫回复一个队列答复，占用很多内存。所以，如果你需要发送大量的命令，最好是把他们按照合理数量分批次的处理，例如10K的命令，读回复，然后再发送另一个10k的命令，等等。这样速度几乎是相同的，但是在回复这10k命令队列需要非常大量的内存用来组织返回数据内容。

### 发布/订阅

用于发布/订阅消息

```bash
# Client1
publish redis hello	# 发布message到redis这个channel
# Client2
subscribe redis # 从redis这个channel中获取message，只有在订阅过后，监听状态下才能接收到C1发布的消息，监听前的消息不会获取。
```

#### 应用场景 - 聊天室

聊天室中一个客户端发送消息，其他客户端应该都能收到，这里涉及到历史消息的问题，一般聊天室中，不仅能看到实时聊天内容，还可以查看历史消息。

这里将消息分为三种，第一种实时消息，用发布/订阅就可以解决问题。

第二种是三天内的消息，这个消息可以保存在sorted-set中，以时间为score，消息为value进行保存，可以按时间进行排序，这样redis缓存可以解决数据的读请求。

第三种是三天以前的消息，全量数据保存在数据库中。

![image-20200721010832926](images/image-20200721010832926.png)

### 事务

Redis作为缓存数据库，特点是快，所以对事务的支持并不是很复杂。

```bash
# 事务基本命令
multi	# 开启事务
set k1 99
set k2 199
exec	# 提交事务

# 多客户端的事务对同一个key的操作，这里需要看谁先提交，谁后提交，由于Redis是单线程，所以事务的处理比较简单
# Client1					# Client2
multi							multi
get k1						delete k1
exec							exec

# watch 命令，在开启事务前，先watch某一个key，如果在事务提交时发现key值已经发生改变，则不执行事务里的操作
# Client1
watch k1
multi
get k1
keys *
exec	# 在Client1提交事务里的操作前，Client2更新了k1的值，此时Client1不会再执行事务里的操作
# Client2
set k2 updated

# discard 命令 当执行 DISCARD 命令时， 事务会被放弃， 事务队列会被清空， 并且客户端会从事务状态中退出
set foo 1
multi
incr foo
discard	# 放弃事务
get foo # 还是会返回1，incr foo命令没有被执行

```

![image-20200721012057122](images/image-20200721012057122.png)

### 布隆过滤器

布隆过滤器，简单说，防止用户请求数据库中不存在的数据，而造成数据库不必要的压力

#### 工作流程

1，需要将数据库里有的元素，通过某几种映射，分别对应到bitmap中，在bitmap中做标记

2，客户端向Redis请求数据，通过映射函数，通过在bitmap中是否有标记来判断数据库中是否有此元素

布隆过滤器是概率解决问题，会大量减少放行（缓存穿透），不可能百分百阻挡，但是成本低，可以大大降低数据库压力。

如果穿透了，数据库中不存在，那么Client应该增加redis中的key，value标记，同时如果数据库中存在，那么需要完成元素对bloom的添加。

#### 安装

```bash
# 访问redis.io modules，访问RedisBloom的github
wget https://github.com/RedisBloom/RedisBloom/archive/master.zip
yum install unzip	# 安装解压工具
unzip master.zip
make # 生成redisbloom.so文件
cp redisbloom.so /opt/qinsheng/redis5/	# redisbloom.so理论上放在任何地方都行，这里拷贝到redis目录下
redis-server --loadmodule /opt/qinsheng/redis5/redisbloom.so
redis-cli	# 可以使用bloom命令
```

#### 基本命令

```bash
 bf.add bf abc	# 加入元素
 bf.exist bf abc	# 返回1，此元素存在
 bf.exist bf aaa	# 返回0，此元素不存在
```



## Redis缓存

Redis作为数据库/缓存的区别，缓存的数据不是全量数据，缓存会随着访问变化更新数据，保证缓存中的都是热数据，因为内存大小是有限的。缓存的数据是可以丢弃的，为了追求极速，缺点是掉电易失，数据库数据则是绝对不能丢的。

因为内存大小是有限的，那么就需要根据业务的不同来内存大小的设置，key的过期时间及缓存的淘汰算法。

![image-20200721021406456](images/image-20200721021406456.png)

### 缓存有效期

```bash
# 有效时间的基本命令
set k1 aaa ex 20 # 20秒过期
ttl k1 # 查看剩余有效时间
get k1 # get命令不会延长有效时间
set k1 bbb # set命令会清空有效时间
expire k1 50 # 对已经存在的key设置有效时间
expireat k1 # 设置某元素的过期时间点
```

#### 过期判定原理

Redis keys过期有两种方式：被动和主动方式

被动：在用户访问时被发现过期

主动：有些过期的keys，永远不会访问他们。 无论如何，这些keys应该过期，所以定时随机测试设置keys的过期时间。所有这些过期的keys将会从密钥空间删除。

具体就是Redis每秒10次做的事情：

1. 测试随机的20个keys进行相关过期检测。
2. 删除所有已经过期的keys。
3. 如果有多于25%的keys过期，重复步奏1.

这是一个平凡的概率算法，基本上的假设是，我们的样本是这个密钥控件，并且我们不断重复过期检测，直到过期的keys的百分百低于25%,这意味着，在任何给定的时刻，最多会清除1/4的过期keys。

### 缓存淘汰算法

Redis的内存大小是由redis.conf文件中maxmemory-policy配置指令来进行配置，当内存满了的时候，会根据指定算法来进行内存回收。主要有以下几种算法

**noeviction**:  返回错误当内存限制达到并且客户端尝试执行会让更多内存被使用的命令（大部分的写入指令，但DEL和几个例外），这是不符合缓存的思想的，数据库则必须这样。

**allkeys-lru**:  尝试回收最少使用的键（LRU），使得新添加的数据有空间存放。

**volatile-lru**:  尝试回收最少使用的键（LRU），但仅限于在过期集合的键，使得新添加的数据有空间存放。

**allkeys-lfu**:  尝试回收使用次数最少的键。

**volatile-lfu**:  从已设置过期时间的数据集挑选使用频率最低的数据淘汰。

**allkeys-random**:  回收随机的键使得新添加的数据有空间存放。（过于不严谨，一般不会选用）

**volatile-random**:  回收随机的键使得新添加的数据有空间存放，但仅限于在过期集合的键。（同上，一般不会选用）

**volatile-ttl**:  回收在过期集合的键，并且优先回收存活时间（TTL）较短的键，使得新添加的数据有空间存放。（需要进行计算，成本过高）



## 持久化

由于Redis是内存级的数据库，数据不是无限存储的，所以需要有一个持久化的过程，而Redis数据持久化有两种方式：

RDB快照存储和AOF日志存储。

### RDB

RedisDB持久化是将数据写入磁盘，写入方式有两种，阻塞和非阻塞

#### 阻塞执行

阻塞执行指Redis暂停对外服务，开始往磁盘写数据，这样的数据具有时点性，保存的即为写入时的数据，不存在混乱，但是在写入过程中，Redis不能继续提供服务，这是严重的弊端，一般只是在关机维护时才会使用。

#### 非阻塞执行

非阻塞执行指Redis一边对外提供服务，一边往磁盘写数据，这样在写数据的过程中，内存中的数据又会发生更新，会造成时点混乱。这时需要利用Linux的父子进程间的数据隔离，生成一个子进程，由子进程进行数据写入，父进程继续对外提供服务，但是会引入一个新的问题，子进程需要将父进程的数据复制一份，如果父进程中是10G，那么子进程中需要再来10G，然后写入磁盘，否则父进程的修改还是会影响数据时点性，以上问题在Linux中可以用fock子进程来解决。

fock是Linux系统调用，创建子进程速度快，创建时不会复制数据，使用了Linux的内核机制写时拷贝（Copy On Write），只有父进程的数据被更新时才会拷贝一份，父进程指向新的更新过后的数据，子进程继续指向旧数据，这样既保证了时点性，不会过多消耗内存且创建子进程速度快

#### RDB相关配置

配置文件中给出bgsave的规则，是save这个标识，这里有瑕疵：

另外如果Redis是正常退出，非意外退出（断电等），那么在退出前会也会将数据写入rdb文件中。

```bash
# rdb配置文件
save 900 1	# 满足条件900秒且更新了1条，就会写入磁盘
save 300 10		# 300秒且更新了10条
save 60 10000		# 60秒且更新了10000条
dbfilename dump.rdb	# 保存在磁盘中的文件名
dir /var/lib/redis/6379	# 文件目录

# rdb存储默认即为开启模式，如果希望关闭，可以设置save ""
save ""
```

![image-20200725023443300](images/image-20200725023443300.png)



#### 基本命令

``` bash
# 保存命令，在没有满足配置文件中的save条件时，也可以手动保存
save	# 阻塞保存
bgsave # 非阻塞保存，另起线程
redis-check-rdb dump.rdb	# 查看rdb文件
```

#### 弊端

**不支持拉链**：只有一个file保存在磁盘中，如果需要保存各版本数据，需要运维手动存储

**丢失数据相对多**：如果一小时写入磁盘一次，那么在8点59分断电的话，会损失最后一小时的数据

#### 优点

类似于Java序列化，恢复数据的速度会相对快。

### AOF

AOF(Append Only File)是将redis的写操作都记录到文件中，在恢复数据时，将记录的命令再重新执行一遍，即可恢复数据，RDB和AOF是可以同时开启的，但是如果开启了AOF，在恢复数据的时候只会用AOF恢复，在4.0版本以后，AOF中包含RDB全量和新的写操作日志，恢复数据时丢失会更少一些。

#### AOF相关配置

```bash
appendonly yes	# 打开aof存储
appendfilename "appendonly.aof"	# aof存储文件名

no-appendfsync-on-rewrite yes

auto-aof-rewrite-percentage 100	 # 重写后，min-size增加100%
auto-aof-rewrite-min-size 64mb	# 在文件大小达到64m时进行重写

appendfsync always	# 每写一条数据，就向日志记录一次
appendfsync everysec	# 每一秒向日志中记录一次
appendfsync no	# 从来不主动flush
```

#### 基本命令

```bash
bgrewriteaof	# 重写
```

#### 优点

**丢失数据少**

#### 弊端

**体量无限变大，数据恢复慢**

##### 解决办法 - 重写

在4.0前，重写时会删除抵消的命令，合并重复的命令，但是最终得到的也是一个纯指令的日志文件。

在4.0后，重写时会将老的数据RDB到AOF文件中，将增量的以指令的方式append到AOF文件中，得到一个混合体AOF文件，利用了RDB的快和日志的全量。

### 混合模式

在Redis4.0后，可以集合RDB和AOF的优点，生成混合体AOF文件，包含二进制RDB文件和增量的AOF日志。

#### 相关配置

```bash
 aof-use-rdb-preamble yes # 打开混合模式
```

### 实验

```bash
# RDB	实验，关闭aof，不会生成aof文件
set k1 aaa
set k2 bbb
save # 查看log文件和rdb文件
bgsave # 提示后台正在写入磁盘
```

```bash
# AOF 实验，打开aof，但是关闭混合模式
set k1 aaa
set k1 bbb
set k2 ddd
set k3 abc
delete k3 # aof文件中会记录这些操作
bgrewriteaof # 重写，aof文件会变小
```

```bash
# 混合模式实验	打开aof，打开混合模式
set k1 aaa
set k2 bbb
set k3 abcde	# 查看rdb文件没有变化，aof文件有记录产生
save # rdb文件发生变化，aof不变
set k1 ccc
set k1 abcde
del k3	# rdb文件没有变化，aof文件追加记录了，文件继续变大
bgrewriteaof # 重写，aof文件会以rdb存储，节省很多计算和存储空间
set k3 aaaaa # aof文件会在rdb存储中追加日志记录
```

# Redis集群

在以上的内容中，介绍的是Redis全是单机单进程的应用，但是只要是单机，就会存在三个问题

单点故障，容量有限，压力

## 相关概念

### AKF

可扩展的系统架构设计有一个理念，通过加机器，一变多来解决容量和可用性问题，AKF是一个更加系统的可扩展模型，通过X，Y，Z轴按不同需求进行扩展

X：全量 - 镜像	集群，所以节点的数据应该保持一致

Y：业务，功能，不同节点间负责不同的业务功能，商品管理，订单管理，用户管理等等

Z：数据分区 关注服务和数据的优先级划分，比如按地域划分

但由机器的一变多，必然引发的问题，数据一致性问题。

### CAP

#### 一致性 

数据的一致性 Consistency，又可以分为以下几种

##### 强一致性

所有的节点数据完全一致，同一时刻访问任一节点都能得到同样的值，这会导致在数据同步时的所有节点阻塞，服务不可用。

![image-20200723112313794](images/image-20200723112313794.png)

##### 弱一致性

通过异步的方式同步数据，及时响应客户端，这样如果某个节点数据更新失败，会出现数据的不一致性。

![image-20200723112522081](images/image-20200723112522081.png)

##### 最终一致性

Redis将数据同步发送到kafka（类似技术），发送成功后，返回给client，再由kafka将数据同步到其他redis节点，最终达到一致性，在同步的过程中，不同节点可能返回不同的数据。

![image-20200723123328280](images/image-20200723123328280.png)

#### 可用性 Availability

在Redis集群中，无论是使用主从或是主备，主只有一个，还是有单点故障的问题，这里就需要对主做高可用，如果主机发生故障，需要由一个技术或程序，代替人来从备机中选出一台接替主机提供服务。

如何判断主机出现故障，这里假如有3台备机，需要几台判断主机出现故障

如果需要三台备机同时说主机出现故障，这里又是强一致性，任一台出现网络问题等，就会造成判断不及时。

如果一台备机判断主机出现故障，就认定出现故障，就会出现统计不准确，一台不够势力范围，出现网络分区的情况，脑裂。

一般需要n/2+1台判断主机出现故障，就可以认定，也就是平时所说的过半。

#### 分区容错性 Partition Tolerance



## 高可用

在Redis中，使用主从复制和哨兵的监控方式，来实现服务的高可用。

### 主从复制

Redis使用默认的异步复制，弱一致性，其特点是低延迟和高性能，slave 和 master 之间异步地确认处理的数据量。

#### 复制流程

在slave连接到master后，master会执行bgsave持久化数据，然后将数据传输给slave，slave接收到数据后（如果数据量大，传输是需要时间的），清除老数据，加载新数据。

当slave和master之间的连接断开后，slave会重新连接上master并会尝试进行部分重同步，以下slave6381有相关实验。

如果无法进行部分重同步，master会将数据进行持久化，将快照发给slave，之后在数据更改时持续发送命令给slave。

#### 相关配置

```bash
replica-serve-stale-data yes	 #允许使用过期数据集, 在主机将数据同步给从时(这个如果数据大，会需要一点时间，在这个时间里)，从的老数据能不能被查看，如果能，就是yes，如果不能，设置为no
replica-read-only yes	# slave是否只读
repl-diskless-sync no	# 默认同步数据是从磁盘传输，改成yes直接走网络传输

repl-backlog-size 1mb	# 增量复制,redis有一个小的消息队列+rdb，如果新的一个从，将rdb复制过来，然后掉线了，再上线，是不是需要再将所有的rdb全部拷过来，如果主上面的数据少，不足1mb，那么只需要把新增的同步过来就行，rdb不用变，但是如果从掉线的几秒，主上面已经超过1m，那么就必须重新同步rdb文件

min-replicas-to-write 3	# slave数量 
min-replicas-max-lag 10	# 秒数
```

#### 实验

```bash
mkdir ~/test/ # 创建一个test目录
cd /root/soft/redis-5.0.5/utils
./install_server.sh 6381 # 创建redis6381服务
cd ~/test/
cp /etc/redis/* . # 将创建的redis6379 6380 6381配置文件拷贝到当前文件夹

# 准备3台机器，aof关闭，log前台实时打印，启动
redis-server ~/test/6379.conf, 6380.conf, 6381.conf
replicaof 127.0.0.1 6379	# 6380运行此命令，作为6379的slave，查看log主节点会把数据同步给从节点，从节点会删除自己的老数据， 然后把主节点同步过来的数据加载到内存里，如下图所示log

#	在6380追随6379后，如下图log所示
# 6379：
# 启动bgsave命令，开启新线程保存rdb文件，将rdb文件传送给6380，结束
# 6380：
# 连接到master 6379， 传输数据，清除老数据，加载主机传送过来的数据到内存，结束。
# 此时6380是read-only状态了。

replicaof no one # 如果master挂掉，那么6380，6381只能读不能写，需要手动设置其中一个为master。
```

![image-20200725021937761](images/image-20200725021937761.png)

![image-20200725021948016](images/image-20200725021948016.png)

```bash
# 6381 设置为6379的slave，断开连接后，再重新启动服务
# 可以看出，master并没有将全量数据传输给6381，而是将6381断开后的增量数据传输过去，以偏移量的形式。
redis-server 6381.conf  replicaof 127.0.0.1 6379
```

![image-20200725024013183](images/image-20200725024013183.png)



### 哨兵

Redis的哨兵（Sentinel）系统用于管理多个Redis服务器（1个Master+N个Slave），该系统主要执行以下三个任务：

- **监控（Monitoring**）： Sentinel 会不断地检查你的主服务器和从服务器是否运作正常。
- **提醒（Notification）**： 当被监控的某个 Redis 服务器出现问题时， Sentinel 可以通过 API 向管理员或者其他应用程序发送通知。
- **自动故障迁移（Automatic failover）**： 当一个主服务器不能正常工作时， Sentinel 会开始一次自动故障迁移操作， 它会将失效主服务器的其中一个从服务器升级为新的主服务器， 并让失效主服务器的其他从服务器改为复制新的主服务器； 当客户端试图连接失效的主服务器时， 集群也会向客户端返回新主服务器的地址， 使得集群可以使用新主服务器代替失效服务器。

哨兵是可以自动发现slave和其他哨兵的：

Sentinel 可以通过询问主服务器来获得所有从服务器的信息，并通过发布/订阅系统发现其他Sentinel的变化。

#### 基本命令

```bash
redis-server /path/to/sentinel.conf --sentinel	# --sentinel 代表当前Redis服务是一个哨兵
```

#### 相关配置

```bash
# 将这个主服务器判断为失效至少需要 2 个 Sentinel 同意 （只要同意 Sentinel 的数量不达标，自动故障迁移就不会执行）
sentinel monitor mymaster 127.0.0.1 6379 2
# down-after-milliseconds 选项指定了 Sentinel 认为服务器已经断线所需的毫秒数。
sentinel down-after-milliseconds mymaster 60000
# 每次只有1个从服务器处于不能处理命令请求的状态。
sentinel parallel-syncs mymaster 1
```

#### 实验

```bash
# 启动一主两从三个redis服务，分别为6379，6380，6381
# 启动三个哨兵，监控6379
cd ~/test/
vi 26379.conf 26380.conf 26381.conf	# 创建三个哨兵配置文件
	# 配置文件内容
	port 26379 26380 26381	
	sentinel monitor mymaster 127.0.0.1 6379 2
redis-server ./26379.conf --sentienl # 启动三个哨兵，每启动一个哨兵，可以在log中看到消息，同时在配置文件中，可以看到记录。
```

![image-20200725204457329](images/image-20200725204457329.png)

![image-20200725204542283](images/image-20200725204542283.png)

```bash
# 关闭 6379服务，由哨兵选举了新的master 6380，将6379置为slave。
# 再重新启动6379服务，能看到replicaof 128.0.0.1:6380 enabled，然后同步数据，没有截图。
```

![image-20200725205009425](images/image-20200725205009425.png)

## 数据分治

通过主从复制，哨兵监控的方式，可以实现redis服务的高可用，但是不能解决redis的单节点容量问题，从而引入了数据拆分。

数据拆分的方式有很多种：

### 业务拆分

在数据量不大，交集不多的情况下使用，根据不同业务访问不同的Redis，Redis实例并不知道其他Redis的存在，比如

由A开头的key存在Redis1，由B开头的Redis存在Redis2，等等

### Sharding分片

#### hash槽

有N个redis服务时，在Client端，通过CRC-16算法，计算出数据在哪个槽区，从而判断该key属于哪个节点。redis cluster一共有16384个槽，所有的master节点都会有一个槽区，槽数是可以迁移的。

#### 一致性哈希

一致性hash是一个0-2^32的闭合圆，计算一致性hash使用如下步骤：

1，对节点进行hash，通常使用节点的ip或者具有唯一标示的数据进行hash，将其值分布在这个闭合圆上。

2，将存储的key进行hash，然后将值分布在这个闭合圆上。

3，从hash(key)在圆上的映射位置开始顺时针方向找到的一个节点即为存储key的节点。

优点：增加节点，不会造成全局洗牌

缺点：

1，新增节点后，会导致有部分数据不能命中，解决办法，要么击穿，访问数据库，要么取顺时针最近两个物理节点。

2，删除节点后，数据会往顺时针的下一个节点上转移，如果下一个节点支持不了骤增的数据，会继续转移到下一个节点，从而造成所有节点崩溃，就是雪崩。

3，节点太少的情况下，例如只有两个，可能会造成大量数据存放在A节点，B节点数据很少，就是数据倾斜。

##### 虚拟节点

为了解决雪崩和数据倾斜的问题，提出虚拟节点的概念，将真实节点计算多个哈希形成多个节点放置到hash环上，定位算法不变，这样在某一节点宕机后，数据不会全部分配给某一个节点，而是分配到多个节点上，同样的，节点增加，数据倾斜的概率也会降低。

但是虚拟节点同样会带来一个问题，就是客户端的连接增加的问题，对Server端的redis造成连接成本很高的问题。

##### 代理

由代理层进行逻辑实现，决定客户端连接到哪一台Redis上。如果代理不能处理这么多的请求，再给Proxy做集群，在前台增加LVS。

## 搭建Redis集群

### twemproxy

不支持哨兵。

```bash
# 具体步骤
cd ~/soft
mkdir twemproxy
cd twemproxy
git clone https://github.com/twitter/twemproxy.git # 如果出错，yum update nss，升级。
cd twemproxy
yum install automake libtool -y
autoreconf -fvi	# 这里会报错，CentOS6默认仓库中Autoconf的版本太低，需要2.64以上。

#替换阿里镜像
# developer.aliyun.com/mirror， 进到epel系统， 执行以下命令
# wget -O /etc/yum.repos.d/epel.repo http://mirrors.aliyun.com/repo/epel-6.repo
yum search autoconf # 这里可以发现autoconf268版本了。

yum install autoconf268
autoreconf268 -fvi 
./configure
make	# 在src目录下会生成nutcracker
cd scripts
cp nutcracker.init /etc/init.d/twemproxy	# 拷贝到init.d下，可以使用service启动
cd /etc/init.d	# 这时候还不是绿色，不能运行
chmod +x twemproxy

mkdir /etc/nutcracker
cd ~/soft/twemproxy/twemproxy/conf
cp ./* /etc/nutcracker/	# 将配置文件拷过去
cd /etc/nutcracker

cd ~/soft/twemproxy/twemproxy/src
cp nutcracker /usr/bin
cd /etc/nutcracker/	#修改配置文件

# 只保留aplha就行，按d和G，删除光标后面所有的内容
alpha:
  listen: 127.0.0.1:22121
  hash: fnv1a_64
  distribution: ketama
  auto_eject_hosts: true
  redis: true
  server_retry_timeout: 2000
  server_failure_limit: 1
  servers:
   - 127.0.0.1:6379:1
   - 127.0.0.1:6380:1

# 启动两台redis，6379，6380
mkdir ~/data/6379 ~/data/6380 # 创建两个目录，redis手动启动就拿当前目录作为data目录
cd ~/data/6379 & redis-server --port 6379
cd ~/data/6380 & redis-server --port 6380

# 启动twemproxy，连接22121端口进行测试，还可以跳过22121，直接连接6379，6380进行验证
service twemproxy start
redis-cli -p 22121

# 数据分治后，keys *, watch k1, multi都不能使用。
```

![image-20200726023052502](images/image-20200726023052502.png)

![image-20200726023457369](images/image-20200726023457369.png)



### predixy

```bash
# 具体步骤
cd ~/soft
mkdir predixy
cd predixy
wget https://github.com/joyieldInc/predixy/releases/download/1.0.5/predixy-1.0.5-bin-amd64-linux.tar.gz
tar xf predixy-1.0.5-bin-amd64-linux.tar.gz
cd predixy-1.0.5/conf

# 修改两个配置文件predixy.conf, sentinel.conf
# 修改 predixy.conf
Include sentinel.conf # 打开哨兵配置文件，注释try.conf # Include try.conf

# 修改sentinel.conf
SentinelServerPool {
    Databases 16
    Hash crc16
    HashTag "{}"	# 例如，{user}这样开头的key，会被放到同一个节点
    Distribution modula
    MasterReadPriority 60
    StaticSlaveReadPriority 50
    DynamicSlaveReadPriority 50
    RefreshInterval 1
    ServerTimeout 1
    ServerFailureLimit 10
    ServerRetryTimeout 1
    KeepAlive 120
    Sentinels {
        + 127.0.0.1:26379
        + 127.0.0.1:26380
        + 127.0.0.1:26381
    }
    Group shard001 {
    }
    Group shard002 {
    }
}

# 启动三台哨兵，分别监控两组redis主从，配置文件如下：
cd ~/test
vi 26379.conf # 26380.conf, 26381.conf 配置类似。
port 26379
sentinel monitor shard001 127.0.0.1 36379 2
sentinel monitor shard002 127.0.0.1 46379 2

```

![image-20200726032736288](images/image-20200726032736288.png)

![image-20200726033016975](images/image-20200726033016975.png)

### Redis cluster

Redis自带集群的实现

``` markdown
# 进到redis安装目录，查看README，/root/soft/redis-5.0.5/utils/create-cluster/README

To create a cluster, follow these steps:
1. Edit create-cluster and change the start / end port, depending on the number of instances you want to create.
2. Use "./create-cluster start" in order to run the instances.
3. Use "./create-cluster create" in order to execute redis-cli --cluster create, so that an actual Redis cluster will be created.
4. Now you are ready to play with the cluster. AOF files and logs for each instances are created in the current directory.

In order to stop a cluster:
1. Use "./create-cluster stop" to stop all the instances. After you stopped the instances you can use "./create-cluster start" to restart them if you change your mind.
2. Use "./create-cluster clean" to remove all the AOF / log files to restart with a clean environment.

Use the command "./create-cluster help" to get the full list of features.
```

```bash
cd ~/soft/redis-5.0.5/utils/create-cluster/
	#	NODES=6	 启动6个redis节点
	#	REPLICAS=1	每个redis节点有一个slave，就是启动三组，每组一主一从。
./create-cluster start	# 在当前目录启动，会在当前目录生成集群中所有节点的log，conf，rdb，aof文件。
./create-cluster create # 从log中可以看出分配槽位及主从分配的信息
./create-cluster stop 
./create-cluster clean # stop + clean 在关闭集群后，清除相关数据。
```

![image-20200726141457224](images/image-20200726141457224.png)

![image-20200726144018239](images/image-20200726144018239.png)



```bash
redis-cli -p 30001	# 连接到30001端口
set k1 abc # 如果不属性当前节点会报错

redis-cli -c -p 30001 # 连接到30001端口 -c
set k1 abc # 如果不属性当前节点会自动切换到正确节点，然后保存成功。
set k2 hello

watch k1 # 也会切换到槽位所属节点，再执行watch

# 如果执行事务，但是事务中key属性不同的节点，那么会出现error
multi
get k1 # 30003
get k2 # 30001
exec # error，没有在30001上开启事务，如图：
```

![image-20200726142206905](images/image-20200726142206905.png)

```bash
# 通过增加{}前缀，使得相关key值在同一节点上，可以使用事务
set {hello}k1 redis
set {hello}k2 netty
set {hello}k1 zookeeper
multi
get {hello}k1
get {hello}k2
get {hello}k3
exec
```

![image-20200726142525736](images/image-20200726142525736.png)

```bash
# 重新分配槽位，挪动多少个，从哪个node挪到哪个node
redis-cli --cluster reshard 127.0.0.1:30001
# 查看分配后的信息，01，02各移出了1000槽位，03增加了2000。
redis-cli --cluster info 12.7.0.0.1:30001
```

![image-20200726143303032](images/image-20200726143303032.png)

![image-20200726143609867](images/image-20200726143609867.png)































