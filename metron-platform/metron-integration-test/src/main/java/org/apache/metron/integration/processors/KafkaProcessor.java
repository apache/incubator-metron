/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.integration.processors;

import com.google.common.base.Function;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.KafkaComponent;

import java.util.LinkedList;
import java.util.List;
public class KafkaProcessor<T> implements Processor<T> {
    private String kafkaComponentName;
    private String readTopic;
    private String errorTopic;
    private String invalidTopic;
    private List<byte[]> errors = new LinkedList<>();
    private List<byte[]> invalids = new LinkedList<>();
    public Object cookie;

    public KafkaProcessor(){}
    public KafkaProcessor withKafkaComponentName(String name){
        this.kafkaComponentName = name;
        return this;
    }
    public KafkaProcessor withReadTopic(String topicName){
        this.readTopic = topicName;
        return this;
    }
    public KafkaProcessor withErrorTopic(String topicName){
        this.errorTopic = topicName;
        return this;
    }
    public KafkaProcessor withInvalidTopic(String topicName){
        this.invalidTopic = topicName;
        return this;
    }
    public KafkaProcessor withValidateReadMessages(Function<List<byte[]>, Boolean> validate){
        this.validateReadMessages = validate;
        return this;
    }
    public KafkaProcessor withHandleReadMessages(Function<List<byte[]>, ReadinessState> handle){
        this.handleReadMessages = handle;
        return this;
    }
    public KafkaProcessor withProvideResult(Function<KafkaProcessor<T>, T> provide){
        this.provideResult = provide;
        return this;
    }


    private Function<List<byte[]>, Boolean> validateReadMessages;
    private Function<List<byte[]>, ReadinessState> handleReadMessages;
    private Function<KafkaProcessor<T>,T> provideResult;

    public Object getCookie(){
        return this.cookie;
    }

    public ReadinessState process(ComponentRunner runner){
        KafkaComponent kafkaComponent = runner.getComponent(kafkaComponentName, KafkaComponent.class);
        List<byte[]> messages = kafkaComponent.readMessages(readTopic);
        Boolean validated = validateReadMessages.apply(messages);
        if(validated == null){
            validated = false;
        }
        if(validated){
            return handleReadMessages.apply(messages);
        }else{
            if(errorTopic != null) {
                errors.addAll(kafkaComponent.readMessages(errorTopic));
            }
            if(invalidTopic != null) {
                invalids.addAll(kafkaComponent.readMessages(invalidTopic));
            }
            if(errors.size() > 0 || invalids.size() > 0) {
                messages = messages;
                return ReadinessState.READY;
            }
            return ReadinessState.NOT_READY;
        }
    }
    @SuppressWarnings("unchecked")
    public ProcessorResult<T> getResult(){
        ProcessorResult.Builder<T> builder = new ProcessorResult.Builder();
        return builder.withResult(provideResult.apply(this)).withProcessErrors(errors).withProcessInvalids(invalids).build();
    }
}
