package cn.kanyun;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * 从HBase中读取数据，所以自定义的mapper类需要继承TableMapper，跟进源码发现，
 * 我们只需要在泛型中写入KEYOUT和VALUEOUT
 * 即TableMapper的父类Mapper类的
 * ImmutableBytesWritable(Hbase中的rowKey数据类型)，Result(Hbase对应rowKey的结果Bean的数据类型)
 * 是不需要写的
 */
public class TsvMapper extends TableMapper<Text, Text> {
    private static final Logger logger = LoggerFactory.getLogger(String.valueOf(TsvMapper.class));

    /**
     * 字段信息(这里的字段顺序,决定了导出来的tsv的列顺序)
     */
    private String[] fieldOrderInfos;


    @Override
    protected void map(ImmutableBytesWritable key, Result value, Context context)
            throws IOException, InterruptedException {
//        注意：key是字节数组类型,key.toString()并不能获取实际的rowKey,需要通过key.get()得到字节数组,然后再转换为String
        Text keyStr = new Text(key.get());
        logger.info("TsvMapper执行,ROWKEY:[{}]", keyStr);
        List<String> rowData = getRowData(value);
//        这里是循环写context,在reducer中接收到的value也是一个迭代器类型
        for (String column : rowData) {
            context.write(keyStr, new Text(column));
        }
    }

    /**
     * Result转Map
     *
     * @param row
     * @return
     */
    private List<String> getRowData(Result row) {
        LinkedList<String> result = new LinkedList<>();
        for (String fieldOrderInfo : fieldOrderInfos) {
            String[] fieldInfo = fieldOrderInfo.split(":");
//            得到列族
            String family = fieldInfo[0];
//            得到列名
            String column = fieldInfo[1];
            Cell columnLatestCell = row.getColumnLatestCell(family.getBytes(StandardCharsets.UTF_8), column.getBytes(StandardCharsets.UTF_8));
            String cellValue = Bytes.toString(columnLatestCell.getQualifierArray());
            result.add(cellValue);
        }
        return result;
    }

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        logger.info("Mapper的setUp()方法先于map()方法执行,可用于获取动态参数");
        String fieldOrder = System.getenv("fieldOrder");
        fieldOrderInfos = fieldOrder.split(",");

//        context.getConfiguration().get()
        super.setup(context);
    }
}
