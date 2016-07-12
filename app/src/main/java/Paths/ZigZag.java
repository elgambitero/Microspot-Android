package Paths;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 7/12/16.
 */
public class ZigZag{

    private ArrayList<Double[]> points;

    public ZigZag(Double[] center, Double[] spacing, Integer[] shots){

        points = new ArrayList<>();

        for(int j = 0; j < shots[1]; j++){
            for(int i = 0;j < shots[0]; i++){

                Double[] coord = new Double[0];

                coord[0] = center[0] + isEven(j)*(i*spacing[0] - (spacing[0]*shots[0]/2));
                coord[1] = center[1] + (j*spacing[1] - (spacing[1]*shots[1]/2));

                points.add(coord);

            }
        }

    }

    public List<Double[]> getPath(){

        return points;

    }

    private int isEven(int i){
        if(i % 2 == 0){
            return 1;
        }else{
            return -1;
        }
    }
}
