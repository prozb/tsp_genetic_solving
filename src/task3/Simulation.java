package task3;

import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * @author Pavlo Rozbytskyi
 * @version 3.0.1
 */

public class Simulation implements Callable<String>{
    public static String TAG = "Simulation";

    public static int counter;      //static simulations counter
    private int generationCount;
    private int geneLen;
    private int replicationSchema;
    private int crossOverSchema;
    private int maxGenerations;     //max generations of one simulation
    private int initRate;
    private int runsNum;            //runs to calculate average
    private int posSimul;
    private float mutationRate;
    private float recombinationRate;
    private float lastPc;
    private boolean protect;        //protect best schema
    private boolean isGraph;        //if need to export to file

    private double da; // distance approximated
    private int [] statArr;
    private double [] distArr;
    private int resPosition;
    private int distPos;
    private StringBuilder sb;
    private Point point;

    static {
        counter = 0;
    }

    Simulation(int geneLen, int generationCount, float mutationRate, float recombinationRate, int runsNum,
               int replicationSchema, int crossOverSchema, int maxGenerations, int initRate, boolean protect,
               Point point, boolean isGraph, int pos){

        this.generationCount   = generationCount;
        this.recombinationRate = recombinationRate;
        this.replicationSchema = replicationSchema;
        this.crossOverSchema   = crossOverSchema;
        this.maxGenerations    = maxGenerations;
        this.mutationRate      = mutationRate;

        this.posSimul = pos;
        this.point    = point;
        this.isGraph  = isGraph;
        this.runsNum  = runsNum;
        this.initRate = initRate;
        this.protect  = protect;
        this.geneLen  = geneLen;
        this.statArr  = new int[runsNum];
        this.distArr  = new double[Constants.GENERATIONS_NUM];
        this.sb       = new StringBuilder();
        this.da       = 0.765 * Math.sqrt( (Constants.CITY_COUNT + 1) * Constants.GRID_SIZE * Constants.GRID_SIZE);

        counter++;
    }

    public String call() {
//        try {
//            if(isGraph)
                graphSimulation();
//            else {
//                startSimulation();
//                printStatistics();
//            }
//        }catch (Exception e){
//            Main.printError("Cannot execute simulation " + e.toString(), TAG);
//        }
        Main.logger.info("Thread #" + Thread.currentThread().getId() + " finished");

        return sb.toString();
    }


    private void graphSimulation(){
        sb.setLength(0);
        //average generations count to achieve max fitness
        float genCount = 0;

        mutationRate      = point.getPm();
        recombinationRate = point.getPc();

        if(Constants.TSP_RANDOM) {
//            for(int i = 0; i < Constants.TSP_SIMULATIONS_COUNT; i++){
            startSimulation();
//                double df = distArr[distPos - 1];
            for (int i = 0; i < distArr.length; i++) {
                exportToBuffer(distArr[i], da);
            }
        }else{
            startSimulation();
            genCount = calcStatistics();
            exportToBuffer(mutationRate, recombinationRate, genCount);
        }

        Main.logger.debug("===> Thread $" + Thread.currentThread().getId() + " simulation #" + posSimul + " finished");

    }

    private void exportToBuffer(double df, double da) {
        sb.append(df);
        sb.append("\t\t");
        sb.append(da);
        sb.append("\t\n");
    }

    private void exportToBuffer(float pc, float pm, float averCount){
        if(lastPc != 0 && lastPc != pc) {
            lastPc = pc;
            sb.append("\n");
        }

        sb.append(pm);
        sb.append("\t\t");
        sb.append(pc);
        sb.append("\t\t");
        sb.append(averCount);
        sb.append("\t\n");
    }

    //process runsNum simulations to calculate average
    private void startSimulation() {
        try {
            int localRunsNum = runsNum;
            int runsCounter = 0;
            while (localRunsNum > 0) {
                runSimulation();
                Main.logger.debug("Thread $" + Thread.currentThread().getId() + " simulation #" + posSimul +
                        " run #" + runsCounter + " finished pm = " + point.getPm() + " pc = " + point.getPc());
                localRunsNum--;
                runsCounter++;

                if(Constants.TSP_RANDOM)
                    break;
            }
        }catch (Exception e){
            Main.logger.error(TAG, e);
        }
    }

    //process just one simulation
    private void runSimulation() {
        int runsCount = maxGenerations;

        DNAPool pool = new DNAPool(generationCount, geneLen, initRate, mutationRate,
                                    replicationSchema, crossOverSchema, recombinationRate, protect);
        while(!pool.isFinished() && runsCount > 0){
            pool.calculateFitness();
            pool.calcMinFitnessOfGeneration();
            pool.processCrossOver();
            pool.processMutation();
            pool.calculateFitness();
            pool.calcMinFitnessOfGeneration();
            pool.sortGeneration();
            pool.processReplication();
            pool.switchToNextGeneration();
            pool.calculateFitness();

            if(Constants.TSP_RANDOM) {
                pushDist(pool.getMinFitnessGeneral());
            }
//            if(runsCount == 100 || runsCount == 300 || runsCount == 700 || runsCount == 1500 || runsCount == 2000)
//                Main.logger.info("runs count = " + runsCount);
            runsCount--;
        }
        Main.logger.info("generation: " + (pool.getGenerationsCount() - 1) + " min fitness general: "
                + pool.getMinFitnessGeneral());
        push(pool.getGenerationsCount() - 1);
    }

    private float calcStatistics(){
        int count = Arrays.stream(statArr).sum();
        clear();
        return count / statArr.length;
    }

    public void printStatistics(){
        Main.logger.info("Average " + calcStatistics() % 1000 + " generations to achieve max fitness");
    }

    // pushes result of the generation into statistics array
    private void push(int res){
        statArr[resPosition++] = res;
    }

    private void pushDist(double dist){
        distArr[distPos++] = dist;
    }

    private void clear(){
        resPosition = 0;
        Arrays.fill(statArr, 0);
    }
}