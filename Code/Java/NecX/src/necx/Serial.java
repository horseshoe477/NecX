package necx;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;

public class Serial implements SerialPortEventListener 
{
	SerialPort serialPort;
        String strCfgPath = "./port.cfg";
	private static final String PORT_NAMES[] = { 
			"/dev/tty.usbserial-A9C7BPLT",   // Mac OS X
			"/dev/ttyUSB0",                  // Linux
			"COM3",// in CSE 409: "COM4",    // Windows
                        };

	private InputStream input;
        private InputStreamReader inputStreamReader;
        private BufferedReader br;
	private OutputStream output;
	private static final int TIME_OUT = 2000;   // Milliseconds to block while waiting for port open
	private static final int DATA_RATE = 57600;  //Default bits per second for COM port
        
        private Mainframe parent;

        public Serial(){}
        public Serial(Mainframe parent){
            this.parent = parent;
        }
        
        // =======================================================================================
        // Initialize
        // =======================================================================================
	public void initialize() 
        {
            CommPortIdentifier portId = null;
            Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

            // Check if user-defined configurations exist
            if(IsCfgExisted())
                reloadCfg();
            
            // iterate through, looking for the port
            while (portEnum.hasMoreElements()) {
                CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
                for (String portName : PORT_NAMES) {
                    if (currPortId.getName().equals(portName)) {
                        portId = currPortId;
                        break;
                    }
                }
            }

            // Check if the COM port is found
            if (portId == null) {
                System.out.println("Could not find COM port.");
                return;
            }

            // Open the port and get ready for reading
            try {
                // open serial port, and use class name for the appName.
                serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);

                // set port parameters
                serialPort.setSerialPortParams(DATA_RATE,
                                SerialPort.DATABITS_8,
                                SerialPort.STOPBITS_1,
                                SerialPort.PARITY_NONE);

                // open the streams
                input = serialPort.getInputStream();
                inputStreamReader = new InputStreamReader(input);
                br = new BufferedReader(inputStreamReader);
                output = serialPort.getOutputStream();

                // add event listeners
                serialPort.addEventListener(this);
                serialPort.notifyOnDataAvailable(true);
            } 
            catch (Exception e) {
                e.printStackTrace();
                this.close();
            }
	}
        
        // Check if the configuration file exists
        private boolean IsCfgExisted()
        {
            File f = new File(strCfgPath);
            if(f.exists())
                return true;
            return false;
        }
        
        // Reload the configuration
        private void reloadCfg()
        {
            String strMsg = "";
            ArrayList<String> arrMsgs = new ArrayList<String>();
            try{
                FileReader fr = new FileReader(strCfgPath);
                BufferedReader br = new BufferedReader(fr);
                while ((strMsg = br.readLine()) != null) {
                    arrMsgs.add(strMsg);
                }
                if(arrMsgs.size() == 3){   // Should contain "exact" 3 lines (Mac, Linux, Windows)
                    for(int i=0;i<arrMsgs.size();i++){
                        PORT_NAMES[i] = arrMsgs.get(i);
                    }
                }
                br.close();
                fr.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        // =======================================================================================
        // Close the port (This should be called when you stop using the port)
        // =======================================================================================
	//public synchronized void close() {
        public void close(){
          synchronized(parent){
            try{
                if (serialPort != null) {
                    serialPort.removeEventListener();
                    serialPort.close();
                }
            }
            catch(Exception e){
                System.out.print(e.getMessage());
            }
          }
	}

        // =======================================================================================
        // Handle an event on the serial port
        // =======================================================================================
	//public synchronized void serialEvent(SerialPortEvent oEvent)
        @Override
        public void serialEvent(SerialPortEvent oEvent)
        {
            if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                try{
                    String strIn = br.readLine();
                    String[] strPara = strIn.split(",");
                    if(strPara.length != 3){
                        System.out.println("[Serial error]: " + strIn);
                        return;
                    }

                    // Check data validaty
                    try{
                        for(int i=0 ; i<strPara.length ; i++)
                            Double.valueOf(strPara[i]);
                    }
                    catch(Exception e){
                        System.out.println("[Serial error]: " + strIn);
                        return;
                    }
                    
                    // Update data to window interface
                    parent.setEMGData_From_SerialPort(strPara[0], strPara[1], strPara[2]);
                } 
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // Ignore all the other eventTypes, but you should consider the other ones.
	}
}