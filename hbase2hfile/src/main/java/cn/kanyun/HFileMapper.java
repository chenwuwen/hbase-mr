package cn.kanyun;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.MapReduceExtendedCell;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 导出HFile文件
 */
public class HFileMapper extends Mapper<LongWritable, Text, ImmutableBytesWritable, MapReduceExtendedCell> {
    private static final Logger logger = LoggerFactory.getLogger(String.valueOf(HFileMapper.class));

    /**
     * 定义TSV文件分隔符
     */
    private String separator = "";

    /**
     * 字段信息(这里的字段顺序,决定了导出来的tsv的列顺序)
     */
    private String[] fieldOrderInfos;

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
//       注意：key是字节数组类型,key.toString()并不能获取实际的rowKey,需要通过key.get()得到字节数组,然后再转换为String
        logger.info("HFileMapper执行,LongWritable:[{}]", key.get());
//        value为tsv文件的一行内容,按照参数进行分割
        String[] values = value.toString().split(separator);
//        取第一列做为rowKey
        byte[] rowKey = values[0].getBytes(StandardCharsets.UTF_8);
//        默认第一列是主键
        ImmutableBytesWritable immutableBytesWritable = new ImmutableBytesWritable(rowKey);
//        从TSV文件的第二列开始
        for (int i = 0; i < fieldOrderInfos.length; i++) {
            String[] fieldInfo = fieldOrderInfos[i].split(":");
            String family = fieldInfo[0];
            String field = fieldInfo[1];
            KeyValue keyValue = new KeyValue(rowKey, family.getBytes(StandardCharsets.UTF_8), field.getBytes(StandardCharsets.UTF_8), values[i + 1].getBytes(StandardCharsets.UTF_8));
            context.write(immutableBytesWritable, new MapReduceExtendedCell(keyValue));
        }


    }

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        logger.info("Mapper的setUp()方法先于map()方法执行,可用于获取动态参数");
        separator = System.getenv("separator");
        String fieldOrder = System.getenv("fieldOrder");
        fieldOrderInfos = fieldOrder.split(",");
//        context.getConfiguration().get()
        super.setup(context);
    }
}
