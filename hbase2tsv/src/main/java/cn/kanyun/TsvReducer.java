package cn.kanyun;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.StringJoiner;

public class TsvReducer extends Reducer<Text, Text, Text, Text> {
    private static final Logger logger = LoggerFactory.getLogger(String.valueOf(TsvReducer.class));

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
//        注意:这里的value是迭代器类型,可以参考本Module的Mapper,是循环write到Context的
        Iterator<Text> iterator = values.iterator();
//        注意Linux下制表符是\t\t而非\t(这里我们导出的是TSV文件)
        StringJoiner joiner = new StringJoiner("^");
        while (iterator.hasNext()) {
            Text next = iterator.next();
            joiner.add(next.toString());
        }
        String data = joiner.toString();
        logger.info("TsvReducer执行,开始写入文件:Key是:[{}]数据是:[{}]", key, data);
        String merge = key.toString() + "^" + data;
        Text text = new Text(merge);
//       TextOutputFormat会以制表符分割KV对(注意Linux下制表符是\t\t而非\t),因此不需要在对写出的key做处理
        context.write(text, new Text(""));

    }
}
