package necx;

import java.util.ArrayList;
import necx.plotter.PlotEMG2D;

public class Mainframe extends javax.swing.JFrame
{
    // Plotting
    PlotEMG2D plotEMG;
    
    // Serial port
    Serial serial;
    
    // EMG data
    double dEmgCh1;
    double dEmgCh2;
    double dEmgCh3;
    int iPlotBufferSize = 1000;
    int iCurMagIdx = 0;
    ArrayList<Double> emgDataCh1;
    ArrayList<Double> emgDataCh2;
    ArrayList<Double> emgDataCh3;
    
    boolean bIsStopped = false;
    
    // =======================================================================================
    // InnerClass - thread to update magnetometer data and plots
    // =======================================================================================
    public class KickstartThread implements Runnable 
    {
        public KickstartThread() {}

        double dPreEmgCh1=0.0, dPreEmgCh2=0.0, dPreEmgCh3=0.0;
        
        public void run() 
        {    
            System.out.println("RubMe Running");
            
            // Initialization
            for (int i = 0; i < iPlotBufferSize; i++) {
                emgDataCh1.add(new Double(0.0));
                emgDataCh2.add(new Double(0.0));
                emgDataCh3.add(new Double(0.0));
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
                    emgDataCh3.set(iCurMagIdx, new Double(dEmgCh3));
                        
                    // -------------------------------------
                    // Plot the curve
                    if(jcb_ShowEMG.isSelected()){
                        int iIdx = (iCurMagIdx+1) % iPlotBufferSize;
                        for(int i=0 ; i<iPlotBufferSize ; i++){
                            plotEMG.updatePlot(i, emgDataCh1.get(iIdx), 0);
                            plotEMG.updatePlot(i, emgDataCh2.get(iIdx), 1);
                            plotEMG.updatePlot(i, emgDataCh3.get(iIdx), 2);
                            
                            iIdx = (++iIdx) % iPlotBufferSize;
                        }
                    }
                   
                    iCurMagIdx = (iCurMagIdx+1) % iPlotBufferSize;

                    Thread.sleep(8);  // Update the chart every 4ms
                }
                catch(Exception e){
                    e.printStackTrace();
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
        dEmgCh3 = 0.0;
        emgDataCh1 = new ArrayList<Double>();
        emgDataCh2 = new ArrayList<Double>();
        emgDataCh3 = new ArrayList<Double>();
        // ------------------------------------------------
        // Initialize the plots
        plotEMG = new PlotEMG2D(this, "Sensor 1", "time", "mg (milligauss)", iPlotBufferSize);
        plotEMG.display(false);
        // ------------------------------------------------
        // Initialize and open the serial port
        serial = new Serial(this);
        serial.initialize();
	System.out.println("Serial started");
        // ------------------------------------------------
        // Start the thread
        Thread tKickstart = new Thread(new KickstartThread());         // Magnetometer data
        tKickstart.start();
        
        
        initComponents();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jcb_ShowEMG = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jcb_ShowEMG.setText("Show EMG data");
        jcb_ShowEMG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcb_ShowEMGActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jcb_ShowEMG)
                .addContainerGap(267, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jcb_ShowEMG)
                .addContainerGap(271, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    // =======================================================================================
    // Events
    // =======================================================================================
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        
        bIsStopped = true;
        //serial.close();
    }//GEN-LAST:event_formWindowClosing

    private void jcb_ShowEMGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcb_ShowEMGActionPerformed
        
        plotEMG.display(jcb_ShowEMG.isSelected());
    }//GEN-LAST:event_jcb_ShowEMGActionPerformed

    // =======================================================================================
    // User-defined
    // =======================================================================================
    public void setEMGData_From_SerialPort(String strCh1, String strCh2, String strCh3)
    {
        //System.out.println(strCh1 + "," + strCh2 + "," + strCh3);
        
        dEmgCh1 = Double.valueOf(strCh1);
        dEmgCh2 = Double.valueOf(strCh2);
        dEmgCh3 = Double.valueOf(strCh3);
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
    private javax.swing.JCheckBox jcb_ShowEMG;
    // End of variables declaration//GEN-END:variables
    // </editor-fold>
}
