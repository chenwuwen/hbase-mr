package cn.kanyun;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class HbaseExportTsvJob extends Configured implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(String.valueOf(HbaseExportTsvJob.class));
    private static DateTimeFormatter yyyyMMddFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static DateTimeFormatter yyyyMMddHHmmssFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static String TABLE_NAME = "";
    private static final String OUTPUT_PATH_PREFIX = "/export/tsv/";


    @Override
    public int run(String[] args) throws Exception {
        TABLE_NAME = System.getProperty("sourceTable");
        String START_TIME = System.getProperty("startTime");
        String RANGE_DAY = System.getProperty("rangeDay");
        String jobName = "HbaseExportTsvJob";
        logger.info("任务名称：[{}],当前任务参数是:[TABLE_NAME={},START_TIME={},RANGE_DAY={}]", jobName, TABLE_NAME, START_TIME, RANGE_DAY);
        logger.warn("注意此处虽然打印了日志,但是在Mapper/Reducer中的日志,并不在此处显示,位置:");
        //创建一个作业: 配置 还有作业名可随意更改~
        Job job = Job.getInstance(getConf(), jobName);
        job.setJarByClass(HbaseExportTsvJob.class);
        Scan scan = new Scan();
//        默认值为true, 分内存，缓存和磁盘，三种方式，一般数据的读取为内存->缓存->磁盘
//        (重要)setCacheBlocks不适合MapReduce工作,MR程序为非热点数据，不需要缓存
        scan.setCacheBlocks(false);
//        一次取多少列
        scan.setBatch(2);
//        一次取多少行
        scan.setCaching(1000);
        LocalDate date = LocalDate.parse(START_TIME, yyyyMMddFormatter);
//        表示开始时间 结束时间,例:START_TIME=20221124,RANGE_DAY=2, 则时间范围是:2022-11-24 00:00:00 ~ 2022-11-26 00:00:00
        LocalDateTime startTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(date.plusDays(Long.valueOf(RANGE_DAY)), LocalTime.MIN);
//        设定读取Hbase的时间范围
        scan.setTimeRange(startTime.toInstant(ZoneOffset.of("+8")).toEpochMilli(), endTime.toInstant(ZoneOffset.of("+8")).toEpochMilli());
        //设置输入(读取的操作)的类型 TextInputFormat
//        job.setInputFormatClass(TextInputFormat.class);
        //设置读取的文件,读取输入文件解析成key，value对
//        TextInputFormat.addInputPath(job, new Path("hdfs://192.168.1.110:9000/input/wordcount.txt"));
//        设置指定我们的 map阶段
//        job.setMapperClass(TsvMapper.class);
//        设置map的输出 k2 和v2的类型
//        job.setMapOutputKeyClass(Text.class);
//        job.setMapOutputValueClass(LongWritable.class);

        TableMapReduceUtil.initTableMapperJob(
                TABLE_NAME,
                scan,
                TsvMapper.class,
                Text.class,
                Text.class,
                job
        );

//        设置reducer阶段
        job.setReducerClass(TsvReducer.class);
//        设置reducer的输出 k3 和v3的类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
//        设置分区数(可用来控制输出文件数量)
        job.setPartitionerClass(TsvPartitioner.class);
//        设置reduce的个数(http://t.zoukankan.com/zimo-jing-p-8682317.html)
        job.setNumReduceTasks(1);
//        要将自定义的输出格式组件设置到job中
        job.setOutputFormatClass(TextOutputFormat.class);

//        设置tsv文件输出路径
        FileOutputFormat.setOutputPath(job, new Path(getOutPutPath(args, startTime, endTime)));
//        是否压缩输出
        FileOutputFormat.setCompressOutput(job,true);
        logger.info("准备执行任务,文件输出路径：[{}]", FileOutputFormat.getOutputPath(job));
//        执行job(参数为true表示监视和打印日志)
        boolean isDone = job.waitForCompletion(true);
        return isDone ? 0 : 1;
    }

    @Override
    public void setConf(Configuration conf) {

    }

    @Override
    public Configuration getConf() {
        try {
            Configuration conf = HBaseConfiguration.create();
            conf.addResource(new FileInputStream("/etc/hbase/conf/hbase-site.xml"));
//            conf.addResource(new FileInputStream("C:\\Users\\yingxu.zhao\\Downloads\\hbase-site.xml"));
//
            //最多有多少个Task同时跑
            conf.set("mapreduce.job.running.map.limit", "8");
            return conf;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 定义TSV文件输出路径,需要注意的是导出的路径是hdfs的路径
     * 如果要导出到本地路径
     * 则需要在路径前添加(file:)表示输出路径是在本地路径,如：file:/home/finance/
     * 否则会默认输出到HDFS路径(原因就在于，已经在classpath下面加载了hdfs-site.xml和core-site.xml的配置文件)
     * 同时要注意,这里只是声明路径,并不能明确导出的文件名称
     * 因为我们定义了导出类TextOutputFormat,而该类继承自FileOutputFormat
     * 而FileOutputFormat要输出一个_SUCCESS文件，所以，这里是指定了一个输出目录
     * 将输出结果移动到最后的输出路径,删除临时的工作空间，并且在输出目录中创建_success隐藏的标志文件.(注意只能是路径,似乎不能控制文件名)
     */
    public String getOutPutPath(String[] args, LocalDateTime startTime, LocalDateTime endTime) {
        String path = "";
        if (StringUtils.isNotEmpty(System.getenv("outputPath"))) {
//            如果命令行定义了输出路径,则以命令行输出路径为准
            path = args[3];
        } else {
            if (TABLE_NAME.contains(":")) {
//                判断表名是否存在冒号,存在冒号说明是带命名空间的表,而HDFS的路径不能出现冒号,所以路径添加命名空间
                String[] split = TABLE_NAME.split(":");
                path = OUTPUT_PATH_PREFIX + split[0] + File.separator + split[1] + File.separator + yyyyMMddFormatter.format(startTime) + "-" + yyyyMMddFormatter.format(endTime);
            } else {
                path = OUTPUT_PATH_PREFIX + TABLE_NAME + File.separator + yyyyMMddFormatter.format(startTime) + "-" + yyyyMMddFormatter.format(endTime);
            }
        }
        return path;
    }


}
