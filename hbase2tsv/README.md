# 该模块主要用来查询HBase数据,并导出Tsv文件

*当然具体导出Tsv还是Csv都可以在代码中进行更改,这取决与你的数据是否与分隔符冲突*

**需要的注意的是：要确保生成文件的列的顺序是一定的,
例:(第一行数据字段顺序为A字段,B字段 ,第二行字段顺序也要为A字段,B字段) 这种一定要做检查,避免在导入的时候,出现偏差**


将模块打好包后,就可以上传到Hbase集群了：
```shell

yarn -jar <上传的jar包名称> -DsourceTable=<源表>  \
       -DstartTime=<开始时间(yyyyMMdd)>  \
       -DrangeDay=<导出天数(整形)>  \
       -DoutputPath=<HFile导出路径(可选)>  \
       -DfieldOrder=<导出tsv字段顺序> 

```
* sourceTable: 你要导出的表
* startTime：从什么时间开始导出,例：20221011
* rangeDay：导出几天的数据,如导出2天的数据,则导出时间范围为 2022-10-11 00:00:00 ~ 2022-10-13 00:00:00
* outputPath: 导出tsv文件存放路径(可选参数,未配置默认导入到hdfs下 /export/tsv/...),可能会导出多个文件,这个可以通过控制分区来实现导出文件数量
* fieldOrder：定义tsv文件字段顺序,例：列族名:字段名 注意,tsv文件默认第一列是rowKey,例((f:name,f:age))


注意：当执行完上面任务之后,目标表并没有插入数据,此时还需要执行命令：
`hbase org.apache.hadoop.hbase.tool.LoadIncrementalHFiles <HFile导出路径> <目标表>`

此时查看目标数据表,数据已新增