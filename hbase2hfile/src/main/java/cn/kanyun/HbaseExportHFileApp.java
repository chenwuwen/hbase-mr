package cn.kanyun;

import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HbaseExportHFileApp {
    private static final Logger logger = LoggerFactory.getLogger(String.valueOf(HbaseExportHFileApp.class));

    public static void main(String[] args) throws Exception {
        logger.warn("至少需要使用4个参数: -DtargetTable=<目标表>  " +
                "-DinputPath=<tsv文件路径> -DoutputPath=<HFile文件输出路径(可选)> " +
                "-Dseparator=<tsv文件分隔符> -DfieldOrder=<tsv文件对应字段名,默认第一列是rowKey,从第二列开始> ");
//        开发环境配置(hadoop主要基于linux编写，winutil.exe主要用于模拟linux下的目录环境),需要确保开发环境的权限可以运行winutil.exe
//        System.setProperty("hadoop.home.dir", "E:\\hadoop\\");
//        ToolRunner可以运行MR
        int exitCode = ToolRunner.run(new HbaseExportHFileJob(), args);
        System.exit(exitCode);
    }
}
