package cn.kanyun;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

public class HbaseConnectionUtil {


    public org.apache.hadoop.conf.Configuration configuration() {

        Configuration configuration = HBaseConfiguration.create();

        configuration.set("hbase.zookeeper.property.clientPort", "2181");
        configuration.set("hbase.zookeeper.quorum", "node01,node02,node03");

        return configuration;
    }

}
