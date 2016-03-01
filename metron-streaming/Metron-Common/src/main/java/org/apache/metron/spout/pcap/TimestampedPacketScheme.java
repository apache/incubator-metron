package org.apache.metron.spout.pcap;

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.google.common.collect.ImmutableList;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import storm.kafka.KeyValueScheme;

import java.util.List;

public class TimestampedPacketScheme implements KeyValueScheme {
    private static final String KV_FIELD = "kv";

    @Override
    public List<Object> deserializeKeyAndValue(byte[] key, byte[] value) {
        Long ts = Bytes.toLong(key);
        System.out.println("Scheme: " + ts);
        return new Values(ImmutableList.of(new LongWritable(ts), new BytesWritable(value)));
    }

    @Override
    public List<Object> deserialize(byte[] ser) {
        throw new UnsupportedOperationException("Really only interested in deserializing a key and a value");
    }

    @Override
    public Fields getOutputFields() {
        return new Fields(KV_FIELD);
    }
}
