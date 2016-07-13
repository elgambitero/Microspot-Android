package Paths;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 7/12/16.
 */
public class ZigZag{

    private double[][] points;

    public ZigZag(double[] center, double[] spacing, int[] shots){

        points = new double[shots[0]*shots[1]][2];

        calcPoints(center,spacing,shots);

    }

    public ZigZag(double[] center, double area, int[] shots){

        points = new double[shots[0]*shots[1]][2];

        double[] intervals = new double[2];

        intervals[0] = Math.sqrt(area)/shots[0];
        intervals[1] = Math.sqrt(area)/shots[1];

        calcPoints(center,intervals,shots);

    }

    public List<double[]> getPath(){

        return Arrays.asList(points);

    }

    private int isEven(int i){
        if(i % 2 == 0){
            return 1;
        }else{
            return -1;
        }
    }

    private void calcPoints(double[] center, double[] spacing, int[] shots){
        for(int j = 0; j < shots[1]; j++){
            for(int i = 0; i < shots[0]; i++){

                double[] coord = new double[2];

                coord[0] = center[0] + isEven(j)*(i*spacing[0] - ( spacing[0]*(shots[0] - 1)/2 ) ) ;
                coord[1] = center[1] - (j*spacing[1] - ( spacing[1]*(shots[1] - 1)/2 ) ) ;

                points[j*shots[0] + i][0] = coord[0];
                points[j*shots[0] + i][1] = coord[1];

            }
        }
    }

}