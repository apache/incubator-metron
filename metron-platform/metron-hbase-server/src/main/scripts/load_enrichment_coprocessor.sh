#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

METRON_VERSION=${project.version}
COPROCESSOR_JAR=metron-hbase-server-$METRON_VERSION-uber.jar

if [ "$#" -ne 5 ]; then
    echo "Script requires 5 params: ENRICHMENT_TABLE, HDFS_URL, HDFS_PATH, COPROCESSOR_IMPL, ZOOKEEPER_URL; Only received $#"
    echo "Exiting"
    exit -1
fi

ENRICHMENT_TABLE=$1
HDFS_URL=$2
HDFS_PATH=$3
COPROCESSOR_IMPL=$4
ZOOKEEPER_URL=$5

echo "Altering ${ENRICHMENT_TABLE} to add coprocessor."
echo "Executing: alter '${ENRICHMENT_TABLE}', METHOD => 'table_att', 'Coprocessor'=>'${HDFS_URL}${HDFS_PATH}/${COPROCESSOR_JAR}|${COPROCESSOR_IMPL}||zookeeperUrl=${ZOOKEEPER_URL}'"
echo "alter '${ENRICHMENT_TABLE}', METHOD => 'table_att', 'Coprocessor'=>'${HDFS_URL}${HDFS_PATH}/${COPROCESSOR_JAR}|${COPROCESSOR_IMPL}||zookeeperUrl=${ZOOKEEPER_URL}'" | hbase shell
echo "Done"
