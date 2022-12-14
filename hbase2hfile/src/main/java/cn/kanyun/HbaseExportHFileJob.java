package cn.kanyun;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat2;
import org.apache.hadoop.hbase.tool.LoadIncrementalHFiles;
import org.apache.hadoop.hbase.util.MapReduceExtendedCell;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class HbaseExportHFileJob extends Configured implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(String.valueOf(HbaseExportHFileJob.class));
    private static DateTimeFormatter yyyyMMddFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static DateTimeFormatter yyyyMMddHHmmssFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static String TABLE_NAME = "";

    @Override
    public int run(String[] args) throws Exception {
        TABLE_NAME = System.getProperty("targetTable");
        String INPUT_PATH = System.getProperty("inputPath");
        String jobName = "HbaseExportHFileJob";
        logger.info("任务名称：[{}],当前任务参数是:[TABLE_NAME={},INPUT_PATH={},RANGE_DAY={}]", jobName, TABLE_NAME, INPUT_PATH);
        logger.warn("注意此处虽然打印了日志,但是在Mapper/Reducer中的日志,并不在此处显示,位置:hdfs:/tmp/logs/hdfs/logs");
        //创建一个作业: 配置 还有作业名可随意更改~
        Job job = Job.getInstance(getConf(), jobName);
        job.setJarByClass(HbaseExportHFileJob.class);
        Connection connection = ConnectionFactory.createConnection(getConf());
        Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
//        得到表中所有的列族
        Set<byte[]> columnFamilyNames = table.getDescriptor().getColumnFamilyNames();
        job.setMapOutputKeyClass(ImmutableBytesWritable.class);
        job.setMapOutputValueClass(MapReduceExtendedCell.class);
//        当job.setNumReduceTasks(0)时，即没有reduce阶段，此时唯一影响的就是map结果的输出方式
//        1:如果有reduce阶段,map的结果被flush到硬盘,作为reduce的输入;reduce的结果将被OutputFormat的RecordWriter写到指定的地方(setOutputPath),作为整个程序的输出.
//        2:如果没有reduce阶段，map的结果将直接被OutputFormat的RecordWriter写到指定的地方（setOutputPath），作为整个程序的输出
//        https://blog.csdn.net/xiaoshunzi111/article/details/48339391
        job.setNumReduceTasks(0);

//        数据写回到HDFS,写成HFile -> 所以指定输出格式为HFileOutputFormat2
        job.setOutputFormatClass(HFileOutputFormat2.class);

//        设置文件导出路径
        HFileOutputFormat2.setOutputPath(job, new Path(getOutPutPath(args)));
//        导出文件压缩
//        HFileOutputFormat2.setCompressOutput(job, true);
//        使MR可以向表中，增量增加数据(有多个重载方法)通过configureIncrementalLoad 方法指定我们的数据需要加载到哪个表的哪个region里面去
//        HFileOutputFormat2.configureIncrementalLoad(job, table, connection.getRegionLocator(TableName.valueOf(TABLE_NAME)));
        logger.info("准备执行任务,文件输出路径：[{}]", HFileOutputFormat2.getOutputPath(job));
//        执行job(参数为true表示监视和打印日志)
        boolean isDone = job.waitForCompletion(true);

//        也可以直接导入(测试的时候可以注释,用以观察过程)
        if (isDone) {
            try {
                logger.info("[{}]执行完毕！准备,Bulk Load HFile..", jobName);
                Admin admin = connection.getAdmin();
                LoadIncrementalHFiles loadFiles = new LoadIncrementalHFiles(job.getConfiguration());
                loadFiles.doBulkLoad(HFileOutputFormat2.getOutputPath(job), admin, table, connection.getRegionLocator(table.getName()));
                logger.info("[{}]执行完毕！Bulk Load Completed..", jobName);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

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
            conf.set("hbase.mapreduce.hfileoutputformat.table.name", "user");
            //设置Hfile压缩
            conf.set("hfile.compression", "snappy");
            conf.set("hbase.defaults.for.version.skip", "true");
            conf.set("hbase.regionserver.codecs", "snappy");

            return conf;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 定义HFile文件输出路径,需要注意的是导出的路径是hdfs的路径
     * 如果要导出到本地路径
     * 则需要在路径前添加(file:)表示输出路径是在本地路径,如：file:/home/finance/
     * 否则会默认输出到HDFS路径(原因就在于，已经在classpath下面加载了hdfs-site.xml和core-site.xml的配置文件)
     * 同时要注意,这里只是声明路径,并不能明确导出的文件名称
     * 因为我们定义了导出类TextOutputFormat,而该类继承自FileOutputFormat
     * 而FileOutputFormat要输出一个_SUCCESS文件，所以，这里是指定了一个输出目录
     * 将输出结果移动到最后的输出路径,删除临时的工作空间，并且在输出目录中创建_success隐藏的标志文件.(注意只能是路径,似乎不能控制文件名)
     */
    public String getOutPutPath(String[] args) {
        String path = "";
        if (StringUtils.isNotEmpty(System.getProperty("outputPath"))) {
//            如果命令行定义了输出路径,则以命令行输出路径为准
            path = args[3];
        } else {
            if (TABLE_NAME.contains(":")) {
//                判断表名是否存在冒号,存在冒号说明是带命名空间的表,而HDFS的路径不能出现冒号,所以路径添加命名空间
                String[] split = TABLE_NAME.split(":");
                path = "/hfile/" + split[0] + File.separator + split[1] + File.separator + yyyyMMddFormatter.format(LocalDate.now());
            } else {
                path = "/hfile/" + TABLE_NAME + File.separator + yyyyMMddFormatter.format(LocalDate.now());
            }
        }
        return path;
    }


}
