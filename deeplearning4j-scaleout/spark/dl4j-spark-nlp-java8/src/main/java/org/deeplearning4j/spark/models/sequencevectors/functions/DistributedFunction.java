package org.deeplearning4j.spark.models.sequencevectors.functions;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;
import org.deeplearning4j.models.embeddings.loader.VectorsConfiguration;
import org.deeplearning4j.models.sequencevectors.sequence.SequenceElement;
import org.deeplearning4j.models.sequencevectors.sequence.ShallowSequenceElement;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.spark.models.sequencevectors.export.ExportContainer;
import org.nd4j.parameterserver.distributed.VoidParameterServer;
import org.nd4j.parameterserver.distributed.conf.VoidConfiguration;

/**
 *
 *
 * @author raver119@gmail.coms
 */
@Slf4j
public class DistributedFunction<T extends SequenceElement> implements Function<T, ExportContainer<T>> {

    protected Broadcast<VoidConfiguration> configurationBroadcast;
    protected Broadcast<VectorsConfiguration> vectorsConfigurationBroadcast;
    protected Broadcast<VocabCache<ShallowSequenceElement>> shallowVocabBroadcast;

    protected transient VocabCache<ShallowSequenceElement> shallowVocabCache;

    public DistributedFunction(@NonNull Broadcast<VoidConfiguration> configurationBroadcast, @NonNull Broadcast<VectorsConfiguration> vectorsConfigurationBroadcast, @NonNull Broadcast<VocabCache<ShallowSequenceElement>> shallowVocabBroadcast) {
        this.configurationBroadcast = configurationBroadcast;
        this.vectorsConfigurationBroadcast = vectorsConfigurationBroadcast;
        this.shallowVocabBroadcast = shallowVocabBroadcast;
    }

    @Override
    public ExportContainer<T> call(T word) throws Exception {
        if (shallowVocabCache == null)
            shallowVocabCache = shallowVocabBroadcast.getValue();

        ExportContainer<T> container = new ExportContainer<>();

        ShallowSequenceElement reduced = shallowVocabCache.tokenFor(word.getStorageId());
        word.setIndex(reduced.getIndex());

        container.setElement(word);
        container.setArray(VoidParameterServer.getInstance().getVector(reduced.getIndex()));

        return container;
    }
}
