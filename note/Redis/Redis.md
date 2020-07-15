## Redis前置常识

磁盘，寻址速度，ms级别

内存，寻址速度，ns级别，速度是磁盘的10W倍

IO buffer，磁盘扇区512byte，如果一次读取数据过小，索引成本变高，如果一次读取数据过大，容易造成浪费，操作系统中，缓存行大小为4k。

数据库，索引都是存在于磁盘中，关系型数据库倾向于行级存储，在数据插入之时，已经给这行数据分配好空间，之后增删改只需要修改数据，不用移动其他数据了。在内存中存在B+T，查询数据时，先找出相应的4k的索引，再通过索引找到4k的数据。

数据库，表数据量很大，性能会不会下降？如果表有索引，增删改速度虽然会变慢，但是查询速度，如果是查询一条的话，速度依然不会变慢，如果并发大的情况下，会受硬盘带宽影响。

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



























