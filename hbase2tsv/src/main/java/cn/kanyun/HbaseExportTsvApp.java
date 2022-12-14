package cn.kanyun;

import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HbaseExportTsvApp {
    private static final Logger logger = LoggerFactory.getLogger(String.valueOf(HbaseExportTsvApp.class));

    public static void main(String[] args) throws Exception {
        logger.warn("至少需要使用4个参数: -DsourceTable=<源表>  -DstartTime=<开始时间(yyyyMMdd)> " +
                "-DrangeDay=<导出天数(整形)> -DoutputPath=<tsv导出路径(可选)> -DfieldOrder=<导出tsv字段顺序>");
//        开发环境配置(hadoop主要基于linux编写，winutil.exe主要用于模拟linux下的目录环境),需要确保开发环境的权限可以运行winutil.exe
//        System.setProperty("hadoop.home.dir", "E:\\hadoop\\");
//        ToolRunner可以运行MR
        int exitCode = ToolRunner.run(new HbaseExportTsvJob(), args);
        System.exit(exitCode);
    }
}
