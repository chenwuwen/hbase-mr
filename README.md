# 这是一个关于使用Hbase MapReduce的代码实例

工程分为3个模块:
> > hbase2tsv: HBase数据转化为文本文件

> > hbase2hfile: 文本文件转化为HFile

> > base: Hbase基础API使用

<u>hbase2tsv/hbase2hfile 这两个模块可以用来做Hbase数据迁移。</u>

我这里使用的是Cloudera Manager 6.3.2的版本,因此对应的 Hbase版本是：2.1.0-cdh6.3.2 Hadoop版本是：3.0.0-cdh6.3.2

如果你做Hbase数据迁移,还有如下方法可以使用：

* HBase自带的导出功能

> hbase org.apache.hadoop.hbase.mapreduce.Export

* HBase自带的导入功能

> hbase org.apache.hadoop.hbase.mapreduce.Import

* HBase自带的CopyTable功能

> hbase  org.apache.hadoop.hbase.mapreduce.CopyTable

* HBase自带的快照功能

> hbase org.apache.hadoop.hbase.snapshot.ExportSnapshot


如果需要在本地调试MR代码,需要添加额外的Jar包到ClassPath(尽量不要尝试使用Maven来加载依赖,因为依赖会很多), 依赖的Jar包可以从你的Hbase服务器进行下载(HADOOP_HOME是环境变量)：
> HADOOP_HOME/share/hadoop/common目录下的hadoop-common-2.7.1.jar和haoop-nfs-2.7.1.jar  
HADOOP_HOME/share/hadoop/common/lib目录下的所有JAR包 HADOOP_HOME/share/hadoop/hdfs目录下的haoop-hdfs-2.7.1.jar和haoop-hdfs-nfs-2.7.1.jar HADOOP_HOME/share/hadoop/hdfs/lib目录下的所有JAR包    
HADOOP_HOME/share/hadoop/mapreduce除hadoop-mapreduce-examples-2.7.1.jar之外的jar包 HADOOP_HOME/share/hadoop/mapreduce/lib/所有jar包

如果你是windows系统,还需要安装插件[winutils](https://blog.csdn.net/ssxueyi/article/details/79716902)

如果你需要使用hbase2hfile、hbase2tsv 来迁移数据, 先在源表所在的HBase集群使用hbase2tsv模块导出tsv文件,然后再将tsv文件传输到目标表所在的HDFS集群,
再在目标表所在的HDFS集群使用hbase2hfile模块将源表数据导入到目标表中,这两个模块的具体用法,请查看模块内的readme


[修复Hbase元数据](https://zhuanlan.zhihu.com/p/267268541)
[HBase2.1.0的坑](https://www.zhihu.com/question/350838118/answer/858933007)
[HBase2.1.0手动修复元数据](https://github.com/DarkPhoenixs/hbase-meta-repair)

HBase rowKey前缀查询：

```shell
# 指定rowKey前缀,并指定列
scan 'scores', {COLUMNS=>['info:name'],FILTER => org.apache.hadoop.hbase.filter.PrefixFilter.new(org.apache.hadoop.hbase.util.Bytes.toBytes('20222'))}
```


```shell
# 范围查找 指定startRowKey/endRowKey
scan 'scores', {STARTROW => 'row2',ENDROW => 'row2'}
```