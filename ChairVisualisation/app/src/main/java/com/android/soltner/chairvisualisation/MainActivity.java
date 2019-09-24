package com.android.soltner.chairvisualisation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class MainActivity extends AppCompatActivity {

    String webserverURL="http://192.168.1.65/data";
    //GyroXYZ,XYZ2,AccelXYZ, AccelXYZ2
    List<List<Double>> allData=new ArrayList<List<Double>>();
    static ArrayList<String> movementTypes=new ArrayList<>();
    Instances instancesData;
    Classifier tree;
    //Colour
    Paint mPaint;
    //Canvas drawing surface
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private ImageView mImageView;
    private Rect mRect;
    private static final int OFFSET=120;
    //colors
    private int mColorBackground, mColorLine, mColorAccent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        DrawView drawView=new DrawView(this);
        drawView.setBackgroundColor(Color.WHITE);
        setContentView(R.layout.activity_main);
        //setContentView(drawView);
        Log.d("Server","Hallo");

        final Handler handler= new Handler();
        Timer timer= new Timer();
        TimerTask doAsynchronousTask= new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        GetStringFromUrl getStringFromUrl= new GetStringFromUrl();
                        getStringFromUrl.execute(webserverURL);
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 10000);
        //new GetStringFromUrl().execute(webserverURL);


    }


    private void init(){
        String[] temp={"FrontBack", "FrontBack-unlocked", "LeftRight", "DoNothing", "Spin", "Roll_around", "SitDown", "StandUp"};
        for(String a: temp)
        movementTypes.add(a);
       for(int i=0;i<12;i++){
           allData.add(new ArrayList<Double>());
       }

        File dir= Environment.getDataDirectory();
       File file=new File(dir,"weka.arff");
        try {
            BufferedReader reader=new BufferedReader(
                    new InputStreamReader(getResources().openRawResource(R.raw.movementvalues2)));



            Instances train= new Instances(reader);
            train.setClassIndex(train.numAttributes()-1);
            Log.d("testcl",String.valueOf(train.classIndex()));
            Log.d("textfile",train.toSummaryString());
            String[] options= new String[1];
            options[0]= "-U";
            tree= new J48();
            //tree.setOptions(options);
            tree.buildClassifier(train);

            Evaluation eval= new Evaluation(train);
            eval.evaluateModel(tree,train);
            eval.crossValidateModel(tree,train,10, new Random(1));
            Log.d("textfile",eval.toSummaryString());
            Log.d("textfile", eval.getRevision()+eval.toMatrixString()+eval.toClassDetailsString());


            InputStream is=openFileInput("output.arff");
            if(is!=null){
                BufferedReader reader1=new BufferedReader(new InputStreamReader(is));
                instancesData=new Instances(reader1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    /*
    public void draw(View view){
        int vWidth= view.getWidth();
        int vHeight=view.getHeight();
        //argb alpha red blue green in 8 bits
        mBitmap=Bitmap.createBitmap(vWidth,vHeight, Bitmap.Config.ARGB_8888);
        mImageView.setImageBitmap(mBitmap);
        mCanvas= new Canvas(mBitmap);
        //60 times per second newly drawen
        view.invalidate();
        mPaint.setColor(mColorLine);
        mRect.set(vWidth/2,vWidth/2,vWidth,vHeight);
        mCanvas.drawLine(vWidth/2-100,vHeight/2-100,vHeight/2+100,vHeight/2+100, mPaint);
    }
*/
    public class GetStringFromUrl extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String data="";
            try {
                URL url= new URL(strings[0]);
                HttpURLConnection con= (HttpURLConnection) url.openConnection();
                InputStream inputStream=new BufferedInputStream(con.getInputStream());
                InputStreamReader isr=new InputStreamReader(inputStream);
                BufferedReader reader=new BufferedReader(isr);

                data=parseData(reader);

                   Log.d("Serverdata", data);
                   System.out.print(data);

                } catch (MalformedURLException e1) {
                e1.printStackTrace();
                Log.e("ERROR", e1.getLocalizedMessage());
            } catch (IOException e1) {
                Log.e("ERROR",e1.getMessage());
                e1.printStackTrace();
            }

            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("Method","onPostExecute");

            /**
             * TODO:
             * bestimme mithilfe der Machine Learning Files die Bewegungen aus Werten
             */


            /**MinGyroX,MaxGyroX, MeanGyroX,Stddev GyroX, MinGyroY.... ->1 window
             *  MinGyroX,MaxGyroX, MeanGyroX,Stddev GyroX, MinGyroY....
             *
             */
            List<List<Double>> features=calculateFeatures(10,5);
            Instances instances=convertFeatures(features);
            ArrayList<String>lastMovements=new ArrayList<>();

            try {
                for(int j=0;j<instances.numInstances();j++) {
                    double label = tree.classifyInstance(instances.instance(j));
                    Log.d("test", instances.classAttribute().value((int) label));
                    lastMovements.add(instances.classAttribute().value((int) label));
                    Log.d("Debug",(instancesData.numInstances())+" " +(instancesData.numAttributes())+" "+instances.numInstances()+" "+instances.numAttributes());
                    instancesData.instance(instancesData.numInstances()-1-instances.numInstances()+j).setClassValue(label);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }



        try {
            FileOutputStream file=openFileOutput("output.arff",MODE_PRIVATE);
            Log.d("Path",getFileStreamPath("output.arff").getAbsolutePath());
            OutputStreamWriter writer1=new OutputStreamWriter(file);
            // writer = new BufferedWriter(new FileWriter("output.arff"));
            //writer.write(newData.toString());
            writer1.write(instancesData.toString());
            writer1.close();
            //writer.flush();
            //writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("Textfile1", "end of data");

            //Log.d("Features",features.toString());
            for(List l: allData)
                l.clear();

            TextView data= findViewById(R.id.Data);
            if(!lastMovements.isEmpty())
            data.setText("Last Movement: "+findMajority(lastMovements));
            data.setTextAppearance(MainActivity.this, android.R.style.TextAppearance_Large);
            //data.setText(Html.fromHtml(result).toString());
        }
        /*
        handle the data with identifiers and parse them into usable lists with gyro and Accel data
         */
        String parseData(BufferedReader reader){
            String data="";
            String line;
            int lin=0;
                try {
                    //represents 10s
                    while (((line=reader.readLine())!=null)){
                        lin++;
                        Log.d("readLines", String.valueOf(lin));
                        if(line.startsWith("GyroX")) {
                            int i = 0;
                            while (line != null && i < 12) {

                                try {
                                    Log.d("parseData", String.valueOf(allData.get(i).size()));
                                    Log.d("parseData",Arrays.toString(line.split(" ")));
                                    allData.get(i).add(Double.parseDouble(line.split(" ")[1]));
                                } catch (Exception e) {
                                    try {
                                        for (int j = 0; i < 10; i++)
                                            Log.d("parseDataCatch", reader.readLine());
                                    } catch (Exception e1) {
                                    }


                                }

                                data += line.substring(line.indexOf(" "));

                                // allData.get(i).add(Double.parseDouble(line.substring(6)));

                                if (i != 11) {
                                    line = reader.readLine();
                                    lin++;
                                }
                                i++;
                            }
                        }
                        else Log.d("Linestarts",line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


            return data;
        }
        /**
         * calculates min, max, mean, std from double Data lists
         * @param windowSize the size of the window to create a seqment
         * @param stepSize the amount of lines the window is shifted for shifting window approach
         */
        List<List<Double>> calculateFeatures(int windowSize,int stepSize){
            double max, min, mean, sd=0;
            int currentStep=0;
            List<List<Double>> output1=new ArrayList<>();
            //loop several times through whole calculation shifted by step
            while(currentStep<allData.get(0).size()) {
                Log.d("AllDataSize",String.valueOf(allData.get(0).size()));
                //one line of 4*6 features
                List<Double> output2 = new ArrayList<Double>();
                //loop through all lists
                for (List<Double> list : allData) {
                    max = list.get(0);
                    min = list.get(0);
                    double sum = 0;
                    int count = 0;

                    //loop through one window, one list

                        while (count < windowSize) {
                            if (currentStep + count == list.size())
                            break;

                                double element = list.get(count + currentStep);
                                if (element > max)
                                    max = element;
                                else if (element < min)
                                    min = element;
                                sum += element;
                                count++;

                        }
                        if(count<windowSize){
                            Log.d("ArffList",output1.toString());
                            Log.d("Size",String.valueOf(output1.size()));
                            //for(int i=0;i<12;i++)

                                //Log.d("Features",String.valueOf(output1.get(0).get(4*i+3)));
                            return output1;
                        }
                        mean = sum / windowSize;
                        double sum2 = 0;
                        int j = 0;
                        Iterator<Double> it = list.iterator();
                        while (it.hasNext() && j < windowSize) {

                            sum2 += Math.pow((it.next() - mean), 2);
                            j++;
                        }
                        sd = Math.sqrt(sum2 / windowSize);
                        output2.add(min);
                        output2.add(max);
                        output2.add(mean);
                        output2.add(sd);

                }
                output1.add(output2);

                Log.d("List4features6sensors", (String.valueOf(currentStep)));
                currentStep += stepSize;
            }
            Log.d("ArffList",output1.toString());
            Log.d("Size",String.valueOf(output1.size()));

           // for(int i=0;i<12;i++)

            //Log.d("Features",String.valueOf(output1.get(0).get(4*i+3)));
            return output1;
        }


    }

    //converts raw ListData into raw Instances Object usable for weka
    public Instances convertFeatures(List<List<Double>> features){
        String[]a1={"MIN_","MAX_","Standard_deviation_","Median_value_"};
        String[]a2={"Gyro","Accel"};
        String[]a3={"X","Y","Z"};
        ArrayList<Attribute>atts = new ArrayList<>();


        for(int k=0;k<a2.length;k++) {
            for (int i = 0; i < a3.length; i++) {
                for (int j = 0; j < a1.length; j++) {
                    atts.add(new Attribute(a1[j] + a2[k]+a3[i]));
                }

            }
            for (int i = 0; i < a3.length; i++) {
                for (int j = 0; j < a1.length; j++) {
                    atts.add(new Attribute(a1[j] + a2[k]+a3[i]+2));
                }

            }
        }
        atts.add(new Attribute("class",movementTypes));


        if(instancesData==null)
        instancesData= new Instances("ChairMovements",atts, 0 );
        Instances currentInstanceData=new Instances("ChairMovements",atts,0);
        /**
        double[]t={1,2,3,412,23123,1,2,23,1,23,1,2};
        double[]t2={12,23,23,12,4,1234,4,131,123,124,1};
        newData.add(new DenseInstance(1.0,t));
        Log.d("Test",newData.toString());
        newData.add(new DenseInstance(1.0,t2));
        Log.d("Test", newData.toString());
        */

        for(int j=0;j<features.size();j++) {
            double[] values = new double[instancesData.numAttributes()];

            for (int i = 0; i < values.length - 1; i++) {
                values[i] = features.get(j).get(i);
                if (i % 4 == 0)
                    Log.d("Min", String.valueOf(features.get(j).get(i)));
            }


            Log.d("Textfile1", Arrays.toString(values));
            DenseInstance i = new DenseInstance(1.0, values);
            /*
            Instances unlabeled=new Instances("TestInstances",atts,0);
            unlabeled.setClassIndex(unlabeled.numAttributes()-1);
            unlabeled.add(i);


            try {
                tree.classifyInstance(unlabeled.firstInstance());
                Log.d("abc", unlabeled.classAttribute().value((int) tree.classifyInstance(unlabeled.firstInstance())));
            } catch (Exception e) {
                e.printStackTrace();
            }
            */

            //DenseInstance i=new DenseInstance(newData.numAttributes());

            currentInstanceData.add(i);
            currentInstanceData.setClassIndex(currentInstanceData.numAttributes()-1);
            instancesData.add(i);
            instancesData.setClassIndex(instancesData.numAttributes() - 1);
            //i.setValue(newData.numAttributes()-1,"?");
            Log.d("convertFeatures", "added one Line");
            Log.d("ConvertFeatures", "Number of lists: " + features.size());
            Log.d("ConvertFeatures", "Number of attributes:" + instancesData.numAttributes() + "FeatureSize" + features.get(j).size() + "Iteration" + j);
            Log.d("ConvertFeatures", instancesData.toString());

        }



        //Log.d("Textfile1",newData.toString());

        return currentInstanceData;

    }
    String findMajority(ArrayList<String> list){
        int maxCount=0;
        int index=0;
        for(int i=0;i<list.size();i++) {
            int count = 0;
            for (int j = 0; j < list.size(); j++) {
                if (list.get(i) == list.get(j))
                    count++;
            }
            if(count>maxCount){
                maxCount=count;
                index=i;
            }
        }
            return list.get(index);
    }
}
