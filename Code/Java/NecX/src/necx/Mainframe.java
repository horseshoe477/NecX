package necx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import necx.plotter.PlotEMG2D;
import weka.classifiers.Classifier;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class Mainframe extends javax.swing.JFrame
{
    // Plotting
    PlotEMG2D plotEMG;
    
    // Serial port
    Serial serial;
    
    // EMG data
    double dEmgCh1;
    double dEmgCh2;
    int iPlotBufferSize = 1023;
    int iCurMagIdx = 0;
    ArrayList<Double> emgDataCh1;
    ArrayList<Double> emgDataCh2;
    int iSamplingInterval = 4;
    boolean bIsStopped = false;
    
    // Weka
    int iNumOfFeatures = 7;         // Number of features
    FastVector fvWekaAttributes;    // Attributes of the feature vector
    Instances myDataSet;            // Data set that stores new feature vectors
    Instance myInstance;            // New feature vector
    Classifier clsSVM;              // Classifier1: SVM
    Classifier clsRFB;              // Classifier2: RFB Network
    Classifier clsNNge;             // Classifier3: NNge
    Classifier clsLMT;              // Classifier4: LMT
    Classifier clsBayesNet;         // Classifier5: BayesNet
    
    // Socket communication
    Socket sock;
    BufferedReader reader;
    BufferedWriter writer;
    DataInputStream dis;
    DataOutputStream dos;
    ServerSocket serverSock = null;
    
    
    // =======================================================================================
    // InnerClass - thread to update magnetometer data and plots
    // =======================================================================================
    public class KickstartThread implements Runnable 
    {
        public KickstartThread() {}

        double dPreEmgCh1=0.0, dPreEmgCh2=0.0, dPreEmgCh3=0.0;
        
        public void run() 
        {    
            System.out.println("NecX Running");
            
            // Initialization
            for (int i = 0; i < iPlotBufferSize; i++) {
                emgDataCh1.add(new Double(0.0));
                emgDataCh2.add(new Double(0.0));
            }
            iCurMagIdx = 0;

            // Plot the powerspectrum & FFT
            bIsStopped = false;
            while ( !bIsStopped) 
            {
                try{
                    // Update the array
                    emgDataCh1.set(iCurMagIdx, new Double(dEmgCh1));
                    emgDataCh2.set(iCurMagIdx, new Double(dEmgCh2));
                        
                    // -------------------------------------
                    // Plot the curve
                    if(jcb_ShowEMG.isSelected()){
                        int iIdx = (iCurMagIdx+1) % iPlotBufferSize;
                        for(int i=0 ; i<iPlotBufferSize ; i++){
                            plotEMG.updatePlot(i, emgDataCh1.get(iIdx), 0);
                            plotEMG.updatePlot(i, emgDataCh2.get(iIdx), 1);
                            
                            iIdx = (++iIdx) % iPlotBufferSize;
                        }
                    }
                   
                    iCurMagIdx = (iCurMagIdx+1) % iPlotBufferSize;

                    Thread.sleep(iSamplingInterval);  // Update the chart every 4ms
                }
                catch(Exception e){
                    //System.out.println("[KickstartThread] " + e.getMessage());
                }
            }
        }
    }
    
    // =======================================================================================
    // InnerClass - Socket server for accepting a new worker
    // =======================================================================================
    public class StartWorkerServerThread implements Runnable
    {
        public StartWorkerServerThread(){}

        public void run(){
            startWorkerServer();
        }
    }
    
    // =======================================================================================
    // InnerClass - thread to update text
    // =======================================================================================
    public class ShowTextThread implements Runnable
    {
        String strText = "";
        public ShowTextThread(String strText){
            this.strText = strText;
        }
        
        public void run(){
            try{
                jlb_Gesture.setText(strText);
                Thread.sleep(2000);
                jlb_Gesture.setText("------");
            }
            catch(Exception e){ e.printStackTrace(); }
        }
    }
    
    // =======================================================================================
    // InnerClass - Worker thread
    // =======================================================================================
    public class WorkerThread implements Runnable
    {
        public WorkerThread(Socket clientSocket){
            try{
                sock = clientSocket;
                InputStream is = sock.getInputStream();
                dis = new DataInputStream(is);
                reader = new BufferedReader(new InputStreamReader(dis));
                OutputStream os = sock.getOutputStream();
                dos = new DataOutputStream(os);
                writer = new BufferedWriter(new OutputStreamWriter(dos));

                System.out.println("Receive request from Workers...");
            }
            catch(Exception e){
                //System.out.println("[WorkerThread] " + e.getMessage());
            }
        }

        public void run()
        {
            // General variables
            boolean bIsContinue = true;
            
            while(bIsContinue)
            {
                try{
                    Thread.sleep(iSamplingInterval);  // Check the queue every 5ms
                    
                    /*
                    // Send data
                    String strRawData = dEmgCh1 + "==" + dEmgCh2 + "\n";
                    writer.write(strRawData);
                    writer.flush();
                    */
                    
                    // Command
                    double dCmd = -1;
                   
                    // Read the results
                    String strMsg = reader.readLine();
                    //
                    if(strMsg.equals("NoHit")){
                        continue;
                        //jlb_Gesture.setText("------");
                    }
                    else if( strMsg.equals("Fire")){
                        dCmd = 100;
                        if(jcb_Debug.isSelected())
                            System.out.println("Fire");
                    }
                    else if( !strMsg.isEmpty())
                    {
                        String[] strFeatures = strMsg.split(",");
                        if(strFeatures.length != iNumOfFeatures) { // Check number of features
                            System.out.println("[WorkerThread]: incorrect feature vector length = " + strFeatures.length);
                            continue;
                        }
                        
                        // Fill instance value
                        myInstance = new Instance(iNumOfFeatures + 1);  // +1=Add the class attribute
                        myInstance.setDataset(myDataSet);
                        for(int i=0 ; i<iNumOfFeatures ; i++)
                            myInstance.setValue(
                                (Attribute)fvWekaAttributes.elementAt(i), 
                                Math.floor(Double.valueOf(strFeatures[i])));
                        //myInstance.setValue((Attribute)fvWekaAttributes.elementAt(iNumOfFeatures), "UTilt");
                        
                        // Perform the prediction
                        double valSVM       = clsSVM.classifyInstance(myInstance);
                        double valRFB       = clsRFB.classifyInstance(myInstance);
                        double valNNge      = clsNNge.classifyInstance(myInstance);
                        double valLMT       = clsLMT.classifyInstance(myInstance);
                        double valBayesNet  = clsBayesNet.classifyInstance(myInstance);
                        // Get the gesture name
                        if(jcb_Debug.isSelected()){
                            System.out.println("[SVM]  " + myInstance.classAttribute().value((int)valSVM));
                            System.out.println("[RFB]  " + myInstance.classAttribute().value((int)valRFB));
                            System.out.println("[NNge] " + myInstance.classAttribute().value((int)valNNge));
                            System.out.println("[LMT]  " + myInstance.classAttribute().value((int)valLMT));
                            System.out.println("[FT]   " + myInstance.classAttribute().value((int)valBayesNet));
                            System.out.println("--------------------------");
                        }
                      
                        dCmd = valSVM + valRFB + valNNge + valLMT + valBayesNet;
                    }
                    
                    // Send Command
                    if(dCmd == -1)
                        continue;
                    else if(dCmd == 100){               // Fire
                        if(jcb_SendKey.isSelected())
                            //sendKey("{Z 50}");
                            sendKey("{UP}"); //sendKey("{RIG}");
                        //jlb_Gesture.setText("Fire!");
                        myShowText("Shaking");
                    }
                    else if(dCmd >= 0 && dCmd < 3){     // Left Rotate
                        if(jcb_SendKey.isSelected())
                            //sendKey("{DOW 200}");
                            sendKey("{LEF}");
                        //jlb_Gesture.setText(myInstance.classAttribute().value(0));
                        myShowText("Left Rotate");
                    }
                    else if(dCmd >= 3 && dCmd <=5){     // Move up
                        if(jcb_SendKey.isSelected())
                            //sendKey("{UP 200}");
                            //sendKey("{UP}");
                            sendKey("{RIG}");
                        //jlb_Gesture.setText(myInstance.classAttribute().value(1));
                        myShowText("Up Tilt");
                    }
                }
                catch(Exception e){
                    System.out.println("[WorkerThread] " + e.getMessage());
                }
            }
        }
    }
    
    // =======================================================================================
    // Constructor
    // =======================================================================================
    public Mainframe() 
    {
        // ------------------------------------------------
        // EMG data
        dEmgCh1 = 0.0;
        dEmgCh2 = 0.0;
        emgDataCh1 = new ArrayList<Double>();
        emgDataCh2 = new ArrayList<Double>();
        // ------------------------------------------------
        // Initialize the plots
        plotEMG = new PlotEMG2D(this, "Sensor 1", "time", "mg (milligauss)", iPlotBufferSize);
        plotEMG.display(true);
        // ------------------------------------------------
        // Initialize and open the serial port
        serial = new Serial(this);
        serial.initialize();
	System.out.println("Serial started");
        // ------------------------------------------------
        // Load the weka model
        loadWeka();
        // ------------------------------------------------
        // Start the thread
        Thread tKickstart = new Thread(new KickstartThread());         // Magnetometer data
        tKickstart.start();
        Thread tWorker    = new Thread(new StartWorkerServerThread()); // Worker
        tWorker.start();
        
        initComponents();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jcb_ShowEMG = new javax.swing.JCheckBox();
        jlb_Gesture = new javax.swing.JLabel();
        jcb_SendKey = new javax.swing.JCheckBox();
        jcb_Debug = new javax.swing.JCheckBox();
        jcb_Ch1 = new javax.swing.JCheckBox();
        jcb_Ch2 = new javax.swing.JCheckBox();
        jcb_ShowHeartRate = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jcb_ShowEMG.setSelected(true);
        jcb_ShowEMG.setText("Show EMG data");
        jcb_ShowEMG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcb_ShowEMGActionPerformed(evt);
            }
        });

        jlb_Gesture.setFont(new java.awt.Font("Lucida Grande", 1, 48)); // NOI18N
        jlb_Gesture.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jlb_Gesture.setText("------");

        jcb_SendKey.setText("Send Key");

        jcb_Debug.setText("Debug");

        jcb_Ch1.setSelected(true);
        jcb_Ch1.setText("Ch1");

        jcb_Ch2.setSelected(true);
        jcb_Ch2.setText("Ch2");

        jcb_ShowHeartRate.setText("HeartRate");
        jcb_ShowHeartRate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcb_ShowHeartRateActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jlb_Gesture, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(jcb_ShowEMG)
                        .add(18, 18, 18)
                        .add(jcb_SendKey)
                        .add(18, 18, 18)
                        .add(jcb_Debug)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 77, Short.MAX_VALUE)
                        .add(jcb_ShowHeartRate)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jcb_Ch1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jcb_Ch2)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jcb_ShowEMG)
                    .add(jcb_SendKey)
                    .add(jcb_Debug)
                    .add(jcb_Ch1)
                    .add(jcb_Ch2)
                    .add(jcb_ShowHeartRate))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jlb_Gesture, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // -----------------------------------------
    // Load Weka model
    // -----------------------------------------
    public void loadWeka()
    {
        try
        {
            // Feature vector
            fvWekaAttributes = new FastVector(iNumOfFeatures + 1);  // +1 = Add the class attribute

            // Declare numeric attributes
            for(int i=0 ; i<iNumOfFeatures ; i++){
                String strFeature = "f" + i;
                fvWekaAttributes.addElement(new Attribute(strFeature));
            }
            // Declare the class attribute along with its values
            FastVector fvClassVal = new FastVector(2);
            fvClassVal.addElement("LRotate");
            //fvClassVal.addElement("Tilt");
            fvClassVal.addElement("UTilt");
            //fvClassVal.addElement("mouth");
            fvWekaAttributes.addElement(new Attribute("class", fvClassVal));

            // Create an empty testing set
            myDataSet = new Instances("TestingSet", fvWekaAttributes, 1);
            myDataSet.setClassIndex(iNumOfFeatures);   // Set class index

            // Create the instance
            myInstance = new Instance(iNumOfFeatures + 1);  // +1=Add the class attribute
            myInstance.setDataset(myDataSet);

            // Load model
            clsSVM      = (Classifier) weka.core.SerializationHelper.read("./wekaModel/mySVM.model");
            clsRFB      = (Classifier) weka.core.SerializationHelper.read("./wekaModel/myRFB.model");
            clsNNge     = (Classifier) weka.core.SerializationHelper.read("./wekaModel/myNNge.model");
            clsLMT      = (Classifier) weka.core.SerializationHelper.read("./wekaModel/myLMT.model");
            clsBayesNet = (Classifier) weka.core.SerializationHelper.read("./wekaModel/myBayesNet.model");
        }
        catch(Exception e){
            System.out.println("[loadWeka] " + e.getMessage());
        }
    }
    
    public void loadWeka2()
    {
        try{
            // Feature vector
            int iNumOfFeatures = 469;
            FastVector fvWekaAttributes = new FastVector(iNumOfFeatures);

            // Declare numeric attributes
            for(int i=0 ; i<iNumOfFeatures-1 ; i++){
                String strFeature = "f" + i;
                fvWekaAttributes.addElement(new Attribute(strFeature));
            }
            // Declare the class attribute along with its values
            FastVector fvClassVal = new FastVector(5);
            fvClassVal.addElement("LRotate");
            fvClassVal.addElement("LTilt");
            fvClassVal.addElement("RRotate");
            fvClassVal.addElement("RTilt");
            fvClassVal.addElement("UTilt");
            fvWekaAttributes.addElement(new Attribute("class", fvClassVal));


            //int[] testSample = {296,295,296,295,295,295,294,295,295,296,296,297,297,297,298,298,299,298,299,299,300,300,299,300,299,300,299,300,299,299,299,299,299,298,299,298,299,298,298,298,297,298,297,298,297,298,297,298,298,297,298,298,299,298,299,298,298,298,297,297,295,296,294,295,294,294,294,293,293,292,293,293,294,293,294,294,295,295,295,296,295,297,297,298,298,299,300,299,301,300,302,302,303,303,304,305,305,306,306,308,307,309,308,309,310,309,311,310,312,311,313,312,313,314,313,314,313,315,314,315,315,315,315,314,315,314,315,314,315,314,315,314,314,314,313,314,312,313,312,312,312,311,311,310,310,309,310,309,309,308,307,308,306,307,306,306,305,306,305,304,304,303,304,303,303,303,303,302,302,302,301,301,300,301,300,300,300,299,299,298,299,298,299,298,299,299,298,298,297,298,297,298,298,298,298,297,298,297,298,297,298,298,298,298,298,298,297,298,297,298,298,298,298,298,298,298,299,298,298,298,298,298,298,298,298,298,298,298,298,298,298,298,298,298,380,385,393,399,407,413,420,427,432,440,444,452,456,462,466,468,473,475,480,481,482,484,487,490,491,493,493,495,495,497,497,497,497,496,497,495,494,492,490,487,484,481,476,474,469,464,457,451,444,437,430,421,414,404,395,386,376,368,358,350,340,331,321,312,301,294,283,274,264,256,249,240,232,222,215,206,198,191,183,176,168,162,154,148,140,134,128,122,117,111,107,100,96,91,87,83,77,75,69,67,62,60,56,54,52,49,48,44,45,42,43,42,42,44,45,49,50,55,58,62,65,69,74,77,82,85,91,95,101,106,111,118,122,129,134,141,146,153,158,165,171,176,184,188,196,200,207,212,218,224,229,236,241,248,252,259,263,269,274,279,284,288,294,297,303,306,311,315,318,322,324,329,330,335,337,340,342,344,347,347,351,351,354,354,355,357,357,358,357,359,358,360,359,360,360,359,360,359,360,359,360,359,359,359,358,359,358,359,357,358,357,357,357,356,357,355,356,354,355,354,354,354,353,353,352,353,352,353,352,352,352,350,351,349};
            int[] testSample = {305,306,306,307,307,308,308,308,309,309,311,312,314,315,318,320,322,326,328,331,332,334,335,336,337,338,339,339,341,341,342,343,344,345,345,346,347,348,348,350,350,351,351,352,352,352,353,352,353,352,352,352,351,350,349,349,347,347,345,344,342,340,337,334,331,327,324,319,315,311,308,305,302,299,296,294,291,288,286,283,281,277,275,272,269,267,264,261,258,256,252,250,247,245,243,240,238,236,234,232,231,230,229,227,226,226,225,225,225,225,225,226,226,226,227,228,229,230,231,232,233,235,236,238,239,241,242,245,246,248,250,252,254,255,258,259,262,263,265,267,269,272,273,275,277,279,281,283,285,287,288,290,292,293,295,296,297,298,299,301,301,302,303,304,304,305,306,306,307,307,308,308,308,308,309,309,309,309,309,310,310,311,311,311,311,311,312,312,312,312,312,312,313,313,313,313,313,313,313,313,313,313,313,313,313,312,313,312,313,312,312,312,312,312,312,312,312,312,312,312,312,312,313,312,313,313,313,313,313,313,313,313,312,313,298,299,298,299,298,298,298,297,297,296,297,296,297,296,297,297,297,298,298,300,301,304,306,308,311,314,318,320,325,328,333,337,341,345,349,355,358,364,367,373,376,380,384,387,391,393,397,399,402,403,404,405,405,406,405,405,403,402,401,399,397,394,393,389,387,383,380,375,371,367,361,356,349,344,336,330,323,316,309,301,295,287,281,272,265,258,251,244,237,231,224,219,213,208,203,198,195,190,188,184,183,180,180,178,178,178,178,180,180,183,184,186,188,190,193,195,200,202,206,209,213,216,219,223,226,231,234,238,241,244,247,250,254,256,260,262,266,267,270,272,274,276,277,280,281,283,284,286,287,288,290,290,292,291,293,293,294,294,295,296,295,297,296,297,297,298,298,297,298,297,298,297,299,298,298,298,298,298,297,299,298,299,299,299,299,299,300,299,301,300,302,302,302,303,303,304,304,305,305,306,306,307,307,307,308,308,309,309,310,310,310,311,310,312,311,312,312,313,313,313,314,313,314,313,315,314,315,315,315,316,315,316,315,316};

            
            // Create an empty training set
            Instances myTestingSet = new Instances("TestingSet", fvWekaAttributes, 1);
            myTestingSet.setClassIndex(iNumOfFeatures-1);   // Set class index
            // Create the instance
            Instance myInstance = new Instance(iNumOfFeatures);
            myInstance.setDataset(myTestingSet);
            // Fill instance value
            for(int i=0 ; i<iNumOfFeatures-1 ; i++){
                myInstance.setValue((Attribute)fvWekaAttributes.elementAt(i), testSample[i]);
            }
            //myInstance.setValue((Attribute)fvWekaAttributes.elementAt(iNumOfFeatures-1), "UTilt");

            //
            // Load model
            //
            Classifier cls = (Classifier) weka.core.SerializationHelper.read("./wekaModel/mySVM.model");

            //perform your prediction
            double value = cls.classifyInstance(myInstance);

            //get the name of the class value
            String prediction = myInstance.classAttribute().value((int)value); 

            System.out.println(prediction);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    // -----------------------------------------
    // Start the server socket (w/ Python)
    // -----------------------------------------
    public void startWorkerServer()
    {
        // Start socket
        try{
            int iPort = 9999;
            //serverSock = new ServerSocket(iPort);
            serverSock = new ServerSocket();
            serverSock.bind(new InetSocketAddress("127.0.0.1", iPort));
            System.out.println("Worker server is running at port " + iPort);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        // Waiting for clients
        while(true)
        {
            try
            {
                Socket clientSocket = serverSock.accept();
                Thread threadWorker = new Thread(new WorkerThread(clientSocket));
                threadWorker.start();
                
                System.out.println("Client (Worker) is connected!!");
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    
    // =======================================================================================
    public void myShowText(String strText){
        Thread tUpdate = new Thread(new ShowTextThread(strText));
        tUpdate.start();
    }
    
    // =======================================================================================
    // Events
    // =======================================================================================
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        
        bIsStopped = true;
        //serial.close();
        try{
            serverSock.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }//GEN-LAST:event_formWindowClosing

    private void jcb_ShowEMGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcb_ShowEMGActionPerformed
        
        plotEMG.display(jcb_ShowEMG.isSelected());
    }//GEN-LAST:event_jcb_ShowEMGActionPerformed

    private void jcb_ShowHeartRateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcb_ShowHeartRateActionPerformed
        
        boolean bIsShowHeartRate = this.jcb_ShowHeartRate.isSelected();
        if(bIsShowHeartRate)
            plotEMG.setValueRange(plotEMG.chart, 0, iPlotBufferSize, 300, 700);
        else
            plotEMG.setValueRange(plotEMG.chart, 0, iPlotBufferSize, 0, 1023);
        
    }//GEN-LAST:event_jcb_ShowHeartRateActionPerformed

    // =======================================================================================
    // User-defined
    // =======================================================================================
    public void setEMGData_From_SerialPort(String strCh1, String strCh2)
    {
        try{
            if(jcb_Ch1.isSelected())
                dEmgCh1 = Double.valueOf(strCh1);
            else
                dEmgCh1 = 0;

            if(jcb_Ch2.isSelected())
                dEmgCh2 = Double.valueOf(strCh2);
            else
                dEmgCh2 = 0;

            // Send data to python side for processing
            String strRawData = dEmgCh1 + "==" + dEmgCh2 + "\n";
            writer.write(strRawData);
            writer.flush();
        }
        catch(Exception e){
            
        }
    }
    
    // =======================================================================================
    // User-defined
    // =======================================================================================
    private void sendKey(String strKey)
    {
        try{
            RobotExt robot = new RobotExt();
        
            //robot.delay(200);
            robot.sendKeys(strKey);
            //robot.mySendKey(strKey);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    // =======================================================================================
    // Entrance
    // =======================================================================================
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Mainframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Mainframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Mainframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Mainframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Mainframe().setVisible(true);
            }
        });
    }
    
    // <editor-fold defaultstate="collapsed" desc="Form component declaration">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jcb_Ch1;
    private javax.swing.JCheckBox jcb_Ch2;
    private javax.swing.JCheckBox jcb_Debug;
    private javax.swing.JCheckBox jcb_SendKey;
    private javax.swing.JCheckBox jcb_ShowEMG;
    private javax.swing.JCheckBox jcb_ShowHeartRate;
    private javax.swing.JLabel jlb_Gesture;
    // End of variables declaration//GEN-END:variables
    // </editor-fold>
}
