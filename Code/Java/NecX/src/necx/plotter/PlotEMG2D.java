package necx.plotter;

import java.awt.Color;
import necx.Mainframe;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public final class PlotEMG2D extends javax.swing.JFrame 
{
    Mainframe parent;
    
    // Chart
    XYSeries seriesMagX,seriesMagY,seriesMagZ;
    XYSeriesCollection dataSet;
    JFreeChart chart;
    
    // =======================================================================================
    // Constructor
    // =======================================================================================
    public PlotEMG2D() {}
    
    public PlotEMG2D(Mainframe parent, String strPlotName, String strX, String strY, int iBufSize) {
        initComponents();
        
        this.parent = parent;
        
        initPlot(strPlotName, strX, strY, iBufSize);
    }
    
    // =======================================================================================
    // User-defined
    // =======================================================================================
    
    // -------------------------------------------------
    private void initPlot(String strPlotName, String strX, String strY, int iBufSize)
    {
        // Initialize the plots
        seriesMagX = new XYSeries("Ch1");
        seriesMagY = new XYSeries("Ch2");
        seriesMagZ = new XYSeries("Ch3");
        dataSet = new XYSeriesCollection();
        dataSet.addSeries(seriesMagX);
        dataSet.addSeries(seriesMagY);
        dataSet.addSeries(seriesMagZ);
        chart = ChartFactory.createXYLineChart(
            strPlotName, strX, strY, dataSet, PlotOrientation.VERTICAL, true, true, false);
        
        // Initialize the data series
        for (int i = 0; i < iBufSize; i++) {
            seriesMagX.add(i, 0.0);
            seriesMagY.add(i, 0.0);
            seriesMagZ.add(i, 0.0);
        }
        
        // Set value range
        setValueRange(chart, 0, iBufSize, 0, 600);
        // Set line color
        setLineColor(255,0,0,0);
        setLineColor(0,255,0,1);
        setLineColor(0,0,255,2);
        
        // Add the chart into JPanel
        ChartPanel panel = new ChartPanel(chart);
        panel.setVisible(true);
        panel.setSize(jp_Plot.getWidth(), jp_Plot.getHeight());
        this.jp_Plot.add(panel, -1);
        this.jp_Plot.setVisible(true);
        this.jp_Plot.revalidate();
        this.jp_Plot.repaint();
    }
    
    // -------------------------------------------------
    public void updatePlot(int iX, double dY, int iSeriesID){
        //Number nIdx = new Integer(iX);
        //Number nVal = new Double(dY);
        switch(iSeriesID){
            case 0: seriesMagX.update(iX, dY); break;
            case 1: seriesMagY.update(iX, dY); break;
            case 2: seriesMagZ.update(iX, dY); break;
            /* For the new jfreechart 1.0.14
            case 0: seriesMagX.update(nIdx, nVal); break;
            case 1: seriesMagY.update(nIdx, nVal); break;
            case 2: seriesMagZ.update(nIdx, nVal); break;
            */
        }
    }
    
    // -------------------------------------------------
    public void display(boolean bIsShow){
        this.setVisible(bIsShow);
    }
    
    // -------------------------------------------------
    public void setLineColor(int iR, int iG, int iB, int iSeriesID){
        XYItemRenderer xyRender = chart.getXYPlot().getRenderer();
        xyRender.setSeriesPaint(iSeriesID, new Color(iR, iG, iB));
        chart.getXYPlot().setRenderer(iSeriesID, xyRender);
    }
    // -------------------------------------------------
    public void setValueRange(JFreeChart chart, double dMinX, double dMaxX, double dMinY, double dMaxY)
    {
        ValueAxis va1x = chart.getXYPlot().getDomainAxis();
        ValueAxis va1y = chart.getXYPlot().getRangeAxis();
        va1x.setAutoRange(false);
        va1y.setAutoRange(false);
        va1x.setRange(dMinX, dMaxX);
        va1y.setRange(dMinY, dMaxY);
        chart.getXYPlot().setDomainAxis(va1x);
        chart.getXYPlot().setRangeAxis(va1y);
    }
    
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jp_Plot = new javax.swing.JPanel();

        jLabel1.setText("jLabel1");

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        jp_Plot.setBorder(new javax.swing.border.MatteBorder(null));

        org.jdesktop.layout.GroupLayout jp_PlotLayout = new org.jdesktop.layout.GroupLayout(jp_Plot);
        jp_Plot.setLayout(jp_PlotLayout);
        jp_PlotLayout.setHorizontalGroup(
            jp_PlotLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 547, Short.MAX_VALUE)
        );
        jp_PlotLayout.setVerticalGroup(
            jp_PlotLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 346, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jp_Plot, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jp_Plot, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    // <editor-fold defaultstate="collapsed" desc="Form component declaration">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jp_Plot;
    // End of variables declaration//GEN-END:variables
    // </editor-fold>
}
