package org.deeplearning4j.ui.parallelism;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.impl.IrisDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingModelSaver;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.saver.InMemoryModelSaver;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.IEarlyStoppingTrainer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.parallelism.trainer.EarlyStoppingParallelTrainer;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.junit.Test;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import static org.junit.Assert.*;


public class TestParallelEarlyStopping {

    @Test
    public void testParallelStatsListenerCompatibility(){
        UIServer uiServer = UIServer.getInstance();

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).iterations(1)
            .updater(Updater.SGD)
            .weightInit(WeightInit.XAVIER)
            .list()
            .layer(0,new OutputLayer.Builder().nIn(4).nOut(3).lossFunction(LossFunctions.LossFunction.MCXENT).build())
            .pretrain(false).backprop(true)
            .build();
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.setListeners(new ScoreIterationListener(1));

        // it's important that the UI can report results from parallel training
        // there's potential for StatsListener to fail if certain properties aren't set in the model
        StatsStorage statsStorage = new InMemoryStatsStorage();
        net.setListeners(new StatsListener(statsStorage));
        uiServer.attach(statsStorage);

        DataSetIterator irisIter = new IrisDataSetIterator(50,600);
        EarlyStoppingModelSaver<MultiLayerNetwork> saver = new InMemoryModelSaver<>();
        EarlyStoppingConfiguration<MultiLayerNetwork> esConf = new EarlyStoppingConfiguration.Builder<MultiLayerNetwork>()
            .epochTerminationConditions(new MaxEpochsTerminationCondition(200))
            .scoreCalculator(new DataSetLossCalculator(irisIter,true))
            .evaluateEveryNEpochs(2)
            .modelSaver(saver)
            .build();

        IEarlyStoppingTrainer<MultiLayerNetwork> trainer = new EarlyStoppingParallelTrainer<>(esConf,net,irisIter,null,2,6,1);

        EarlyStoppingResult<MultiLayerNetwork> result = trainer.fit();
        System.out.println(result);

        assertEquals(EarlyStoppingResult.TerminationReason.EpochTerminationCondition,result.getTerminationReason());
    }
}