/usr/local/bin/yaf --in eth0 --live pcap --idle-timeout 0 | /usr/local/bin/yafscii --tabular | /usr/hdp/current/kafka-broker/bin/kafka-console-producer.sh --broker-list $1 --topic ipfix
