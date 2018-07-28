
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.lang.Math;
import java.net.*;

public class P2{

    private Socket socket= null;

    public P2(String WindowSize,String host, int portNo) {

        //Establish connection with server              
        try{
            socket = new Socket(host, portNo);
        }
        catch(IOException i){
            System.out.println("connection refused");
            System.exit(0);
        }

        StringBuilder sb = new StringBuilder();
        try{
                //Initialize variables
                String s;
                int windowsize=Integer.parseInt(WindowSize);
                int dim;
                int count=0;
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Map<String, List<Integer>> map = new HashMap<String, List<Integer>>();
                String key;

                //Read input data as a stream
                while((s=input.readLine())!=null)
                {
                    String[] getdim=s.split(",");
                    dim=getdim.length-1;

                    //Calculate threshold and no of grids
                    double power=( Math.log(windowsize) / (dim+1) );
                    double p=Math.pow(Math.E, power);
                    int noOfGrids=(int) Math.ceil(p);
                    int range=(int) (2*(Math.pow(2, 15)));
                    int intervalSize=(int)range/noOfGrids;
                    int threshold=(int) Math.ceil(Math.log(p));

                    //For first w data points
                    if(count<windowsize){
                                        count=count+1;
                                        sb.append(s+"\n");

                                        String[] str=s.split(",");
                                        int t=Integer.parseInt(str[0]);

                                        StringBuilder sb1 = new StringBuilder();
                                        int c[]= new int[dim+1];
                                        for(int i=1;i<=dim;i++) {
                                                int a=Integer.parseInt(str[i]);
                                                if (a>32767 || a<-32768) {
                                                        System.out.println("Error: data points are out of range");
                                                        System.exit(0);
                                                }

                                                c[i]=(int) Math.floor(((double)a/range)*(double)noOfGrids);
                                                sb1.append(c[i]);
                                        }

                                        key = sb1.toString();

                                        if (map.get(key) == null) {
                                            map.put(key, new ArrayList<Integer>());
                                        }
                                        map.get(key).add(t);

                    }
                    //For w+1 data points
                    else{
                                                sb.append(s+"\n");

                                                String[] str=s.split(",");
                                                int t=Integer.parseInt(str[0]);
                                                ArrayList<Integer> xdata=new ArrayList<Integer>();
                                                for (int j=1;j<=dim;j++) {
                                                        xdata.add(Integer.parseInt(getdim[j]));
                                                }

                                                //Determine which grid it belongs to                            
                                                StringBuilder sb1 = new StringBuilder();
                                                int c[]= new int[dim+1];
                                                for(int i=1;i<=dim;i++) {
                                                        int a=Integer.parseInt(str[i]);
                                                        if (a>32767 || a<-32768) {
                                                                System.out.println("Error: data points are out of range");
                                                                System.exit(0);
                                                        }

                                                        c[i]=(int) Math.floor(((double)a/range)*(double)noOfGrids);

                                                        sb1.append(c[i]);
                                                }

                                                key = sb1.toString();
                                                boolean output;

                                                //If grid is empty
                                                if (map.get(key) == null) {
                                                                        //Check neighbor grid cells density
                                                                        output=checkNeighbor(key,map,threshold,t);

                                                                        //Output data if it is an outlier
                                                                        if(output) {

                                                                                System.out.print("Outlier: timestamp:"+t+", data: ");
                                                                                for (int j=1;j<dim;j++) {
                                                                                        System.out.print(xdata.get(j-1)+",");
                                                                                }
                                                                                System.out.println(xdata.get(dim-1));
                                                                        }
                                                 }
                                                 //If grid already exists
                                                 else {
                                                                int density=map.get(key).size();

                                                                //Density greater than threshold, update sliding window         
                                                                if(density>=threshold) {
                                                                        updateWindow(map,key,t);
                                                                }
                                                                //Density less than threshold, check neighbor grid cells density
                                                                else {
                                                                        output=checkNeighbor(key,map,threshold,t);
                                                                        //Output data if it is an outlier
                                                                        if(output) {
                                                                                System.out.print("Outlier: timestamp:"+t+", data: ");
                                                                                for (int j=1;j<dim;j++) {
                                                                                        System.out.print(xdata.get(j-1)+",");
                                                                                }
                                                                                System.out.println(xdata.get(dim-1));
                                                                        }
                                                                }
                                                 }
                           }
                }
                input.close();

        }
        catch(IOException e){
                System.out.println(e);
        }

    }

    //Get all possible neighbors of the grid cell
    public ArrayList<String> getAllNeighbors(ArrayList<Integer> point, ArrayList<String> out) {

        //For 1 dimentional data        
        if (point.size() == 1){

                        int oned=point.get(0) - 1;
                        out.add(Integer.toString(oned));
                        oned=point.get(0);
                        out.add(Integer.toString(oned));
                        oned=point.get(0) + 1;
                        out.add(Integer.toString(oned));
                        return out;
        }
        //For n dimentional data
        else {

                        int first =point.get(0);
                        point.remove(0);

                        ArrayList<String> pt=getAllNeighbors(point,out);
                        ArrayList<String> out2=new ArrayList<String>();

                        for(int i=0;i<pt.size();i++) {

                                        String sb5;

                                        int old=first-1;
                                        sb5=Integer.toString(old)+pt.get(i);
                                        out2.add(sb5);

                                        old=first;
                                        sb5=Integer.toString(old)+pt.get(i);
                                        out2.add(sb5);

                                        old=first+1;
                                        sb5=Integer.toString(old)+pt.get(i);
                                        out2.add(sb5);

                        }
                        return out2;
                }
    }

    //Check neighbor grid cells density
    public boolean checkNeighbor(String key, Map<String, List<Integer>> map, int threshold, int t) {

                        ArrayList<Integer> alldim=new ArrayList<Integer>();

                        //Store current grid cell index in a ArrayList
                        int z=0;
                        while(z<key.length()) {
                                if(key.charAt(z)!='-') {
                                        int p1=key.charAt(z)-'0';
                                        alldim.add(p1);
                                        z=z+1;
                                }
                                else {
                                        StringBuilder f=new StringBuilder();
                                        f.append(key.charAt(z));
                                        z=z+1;
                                        f.append(key.charAt(z));
                                        alldim.add(Integer.parseInt(f.toString()));
                                        z=z+1;
                                }
                        }

                        ArrayList<String> plt=new ArrayList<String>();
                        ArrayList<String> out=new ArrayList<String>();

                        //Get all possible neighbors of the grid cell
                        plt=getAllNeighbors(alldim,out);

                        boolean outlier=true;
                        
                        //Check density of neighbor grid cells
                        for (int i=0;i<plt.size();i++) {
                                if (map.get(plt.get(i)) != null && map.get(plt.get(i)).size()>= threshold) {
                                                outlier=false;
                                                updateWindow(map,key,t);
                                                break;
                                }
                        }
                        return outlier;

    }


    //Update sliding window
    public void updateWindow(Map<String, List<Integer>> map, String key, int t) {

                //Add new data to the sliding window
                if (map.get(key) == null) {
                        map.put(key, new ArrayList<Integer>());
                }
                map.get(key).add(t);
                String key1 = null;
                List<Integer> values = null ;
                ArrayList<Integer> minarray=new ArrayList<Integer>();

                //Get timestamp which was inserted into the sliding window first
                for (Entry<String, List<Integer>> ee : map.entrySet()) {
                    key1 = ee.getKey();
                    values = ee.getValue();

                    int min=Integer.MAX_VALUE;
                    for(int i1 : values) {
                                if (i1<min) {
                                        min=i1;
                                }
                    }

                    minarray.add(min);

                }
                int mini=Integer.MAX_VALUE;

                for(int a:minarray) {
                        mini=Math.min(mini,a);
                }
                
                //Remove that timestamp from the sliding window
                for (Entry<String, List<Integer>> ee : map.entrySet()) {
                        key1 = ee.getKey();
                        if (! map.get(key1).isEmpty()){
                            values = ee.getValue();
                            if(values.contains(mini)) {
                                    map.get(key1).remove(values.indexOf(mini));
                                    break;
                            }
                        }
                }
    }

    //Main
    public static void main(String args[])throws IOException{

                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

                //Error Handling for reading data from a test file
                String windowSize=null;
                String server=null;

                if((windowSize=in.readLine())==null || (server=in.readLine())==null){
                        System.out.println("invalid input: Test file data should be 2 lines");
                        System.exit(0);
                }

                try{
                    int num = Integer.parseInt(windowSize);
                }catch (NumberFormatException ex) {
                    System.out.println("invalid input: window size should be an integer");
                    System.exit(0);
                }

                String p=null;
                String ip=null;
                try{
                        String[] s=server.split(":");
                        p=s[1];
                        ip=s[0];
                }
                catch(Exception e){
                        System.out.println(e);
                        System.exit(0);
                }

                int port=0;
                try {
                        port=Integer.parseInt(p);
                }
                catch(NumberFormatException ex) {
                    System.out.println("invalid input: port number should be an integer");
                    System.exit(0);
                }

                //If test file data is valid, use the data to establish connection with the server
                if (port>=1024 && port<65536){
                        //in.close();
                        P2 client = new P2(windowSize,ip,port);
                }
                else{
                        System.out.println("invalid port");
                        System.exit(0);
                }
    }
}

                    
                  
