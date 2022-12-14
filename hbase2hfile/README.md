# 该模块主要读取Tsv文件,并生成HFile


**需要的注意的是：要确保生成文件的列的顺序是一定的,
例:(第一行数据字段顺序为A字段,B字段 ,第二行字段顺序也要为A字段,B字段) 这种一定要做检查,避免在导入的时候,出现偏差**


将模块打好包后,就可以上传到Hbase集群了：
```shell

yarn -jar <上传的jar包名称> -DtargetTable=<目标表>  \
       -DinputPath=<tsv文件路径>  \
       -DoutputPath=<HFile文件输出路径(可选)>  \
       -Dseparator=<tsv文件分隔符>  \
       -DfieldOrder=<tsv文件对应字段名,默认第一列是rowKey,从第二列开始> 

```
* sourceTable: 你要导如的目标表
* inputPath：tsv文件的路径,HDFS路径
* outputPath: 导出的HFile文件路径
* separator：tsv文件字段分隔符
* fieldOrder：定义tsv文件字段顺序,例：列族名:字段名 注意,tsv文件默认第一列是rowKey(f:name,f:age)


