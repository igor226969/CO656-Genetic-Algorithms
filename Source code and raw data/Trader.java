import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;
import java.util.*;
import java.text.*;

public class Trader
{

    private double[][] data = new double[0][0];
    private static final int MAXGEN =1000;
    private static final int POPSIZE = 100;
    private static final int TOURNYSIZE = 20;
    private Random random = new Random();
    private double[][] population = new double[POPSIZE][4];
    private double[][] tempPopulation = new double[POPSIZE][4];
    private ArrayList<Double> fitness = new ArrayList<>();
    private double budget = 3000;
    private int portfolio = 0;
    private int generation = 1;
    private double[] best = new double[4];
    private DecimalFormat df = new DecimalFormat("#.##");
    private int sameCounter = 0;
    
    public void run(String filename) throws IOException {
        load(filename);
        initialise();
        evaluate();
        // Calculates the max fitness value of the first population and gets its index 
        double max = Collections.max(fitness);
        int maxPos = fitness.indexOf(max);
        //copies the weightings to a global variable
        for(int i = 0; i < 4; i++){
            best[i] = population[maxPos][i];    
        }
        System.out.println("Generation 0: " + max);
        for (int g = 0; g < MAXGEN; g++) {
            double[][] crossover = new double[2][4];
            double[] mutate = new double[4];
            double[][] NEWGEN = new double[POPSIZE][4];
            boolean mutation;
            int x = 0;
            // ga starts
            for(int i=1; i<POPSIZE; i+= x) {
                //checks if max fitness hasnt changed in 10 generations and forces mutation
                if(sameCounter == 10){
                    mutation = true;
                    sameCounter = 0;
                }
                else{
                    mutation = random.nextBoolean();
                }
                //50/50 on mutation and crossover otherwise unless 2 offspring would cause a popualtion oiverflow
                if(mutation == true){
                    mutate = mutation(select());
                    NEWGEN[i] = mutate;
                    x= 1;
                }
                else if(mutation == false && i < POPSIZE - 2){
                    crossover = crossover(select(),select());
                    for(int j=0;j<2;j++){
                        NEWGEN[i+j] = crossover[j];
                        x = 2;
                    }
                }
                else{
                    mutate = mutation(select());
                    NEWGEN[i]=mutate;
                    x = 1;
                }
            }
            //elitism
            //checks if a new max fitness has been achieved
            population = NEWGEN;
            evaluate();
            double max1 = Collections.max(fitness);
            if(max1 > max){
                max = max1;
                maxPos = fitness.indexOf(max1);
                for(int i = 0; i < 4; i++){
                    best[i] = population[maxPos][i];    
                }
            }
            else if (max1 == max){
                max = max1;
                maxPos = fitness.indexOf(max1);
                for(int i = 0; i < 4; i++){
                    best[i] = population[maxPos][i];    
                }
                sameCounter += 1;
            }
            for(int z = 0; z < 4; z++){
                population[0][z] = best[z];    
            }
            //prints the highest fitness of the generation and the gen number
            evaluate();
            DecimalFormat df = new DecimalFormat("#.##");
            
            System.out.println("Generation "+ generation + ": " + df.format(max));
            System.out.println(Arrays.toString(best));
            System.out.println("-------------------------------------");
            
            generation +=1;
        }
    }   
    
    public void load(String filename) throws IOException {
        //reads the csv file
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        ArrayList<double[]> lines = new ArrayList<>();
        String line = null;
        // skip the first 28 lines as not all signals generated
        for(int i=0; i<28;i++)
        {
                reader.readLine();
        }
    
        while ((line = reader.readLine()) != null) {
            String[] split = line.split(",");
            
            double[] values = new double[split.length];
            
            for (int i = 0; i < values.length; i++) {
                values[i] = "N/A".equals(split[i])
                                ? Double.NaN
                                : Double.valueOf(split[i]);
            }
            
            lines.add(values);
        }
        
        data = lines.toArray(data);
        
    }
    
    public void initialise(){
        //initialises random population to 2 dp
        double value2dp;
        for(int i=0; i < POPSIZE; i++){
            for(int j=0; j < 4; j++){
                double value = random.nextDouble();
                value2dp = Math.floor(value * 100) / 100;
                population[i][j] = value2dp;    
            }
        }
    }
   
    public void evaluate(){
        //calcualtes fitness of the whole population
        fitness.clear();
        for(int j = 0; j < POPSIZE; j ++){
            budget = 3000;
            portfolio = 0;
            for(int i = 0; i < data.length; i++) {
                double sell = 0;
                double buy = 0;
                double hold = 0;
                double price = data[i][0];
                for(int x = 0; x < 4; x ++){
                    if(data[i][x+6] == 2.0){
                        sell += population[j][x];
                    }
                    else if(data[i][x+6] == 1.0){
                        buy += population[j][x];
                    }
                    else if(data[i][x+6] == 0){
                        hold += population[j][x];
                    }
                }
                //theses are changed to buy 1 and sell 1 etc. for testing. Currently set to sell 1 and buy all as this gave me the highest value.
                if(sell > buy && sell > hold){
                    if(portfolio > 0){
                        budget += price;
                        portfolio -=1;
                    }
                }
                else if(buy > sell && buy > hold){
                    if(budget > price){
                        double nStock = Math.floor(budget/price);
                        budget -= price * nStock;
                        portfolio += nStock;
                    }
                }
            }
            fitness.add(budget + (portfolio * data[data.length - 1][0]));
        }
    }
    
    private int select(){
        //select operator (tournament select)
        ArrayList<Integer> selected = new ArrayList<>();
        ArrayList<Double> tFitness = new ArrayList<>();
        double winner = 0;
        int winnerPosition = 0;
        for(int i = 0; i < TOURNYSIZE; i++){
            int pos = random.nextInt(POPSIZE-1);
            while(selected.contains(pos)){
                pos = random.nextInt(POPSIZE);
            }
            selected.add(pos);
            tFitness.add(fitness.get(pos));    
            
        }
        winner = Collections.max(tFitness);
        winnerPosition = fitness.indexOf(winner);
        return winnerPosition;
    }
    
    private double[][] crossover(int first, int second){
        //crossover at random point
        double[][] offspring = new double[2][4];
        for(int i=0; i < 2; i++){
            int crossoverPoint = random.nextInt(4);
            for(int j=0; j < 4; j++){
                if(i == 0){
                    if(j < crossoverPoint){
                        offspring[i][j] = population[first][j];
                    }
                    else{
                        offspring[i][j] = population[second][j];
                    }
                }
                else if(i == 1){
                    if(j < crossoverPoint){
                        offspring[i][j] = population[second][j];
                    }
                    else{
                        offspring[i][j] = population[first][j];
                    }
                }
            }
        }
        return offspring;
    }
    
    public double[] mutation(int parent){
        //mutation at random point
        double[] offspring = new double[4];
        offspring = population[parent];
        int mutationPoint = random.nextInt(4);
        double value = random.nextDouble();
        double value2dp = Math.floor(value * 100) / 100;
        offspring[mutationPoint] = value2dp;
        return offspring;
    }
}