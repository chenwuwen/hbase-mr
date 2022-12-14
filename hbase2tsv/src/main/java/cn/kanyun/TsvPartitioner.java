package cn.kanyun;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Partitioner;

/**
 * 返回分区数(哪个分区),需要与job.setNumReduceTasks(1)配合使用
 * 如设置ReduceTask个数为16 则可选分区为0-15
 * 如设置ReduceTask个数为1 则可选分区仅可为0
 * 因此如果有自定义分区的需求,则可以定义此类,如果ReduceTask设为1,则不需要定义此类
 * 因为只有一个分区,根本就没得选
 */
public class TsvPartitioner extends Partitioner<Text, Text> {

    @Override
    public int getPartition(Text text, Text text2, int i) {
        return 0;
    }
}
