
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortPacketListener;




/**
 * @author Angelo
 *
 */
public class ArduinoCom extends JFrame implements SerialPortPacketListener {

	SerialPort port;
	String output;
	String outputOneMovement;
	int windowSize=10;
	int stepSize=5; //number of dataLines moved per computing
	static String portName="COM3";
	static int gyroX=0,gyroY=1,gyroZ=2,gyroX2=3,gyroY2=4,gyroZ2=5,accelX=6,accelY=7,accelZ=8,accelX2=9,axelY2=10,accelZ2=11;
	BufferedReader serialReader=null;
	String movement="";
	
	
	String data="";
	
	
	//intialize all lists
	List<String> gyroXList=new ArrayList<String>();
	List<String> gyroYList=new ArrayList<String>();
	List<String> gyroZList=new ArrayList<String>();
	List<String> gyroX2List=new ArrayList<String>();
	List<String> gyroY2List=new ArrayList<String>();
	List<String> gyroZ2List=new ArrayList<String>();
	List<String> accelXList=new ArrayList<String>();
	List<String> accelYList=new ArrayList<String>();
	List<String> accelZList=new ArrayList<String>();
	List<String> accelX2List=new ArrayList<String>();
	List<String> accelY2List=new ArrayList<String>();
	List<String> accelZ2List=new ArrayList<String>();
	
	List<List<String>> allLists=new ArrayList<List<String>>(); //gyro X Y Z X1 X2 X3 ->accel X Y Z X2 Y2 Z2
	
	File pathToCsv=new File("C:\\Users\\Angelo\\Desktop\\movementValues.arff");
	String pathOut="C:\\Users\\Angelo\\Desktop\\movementValues.arff";
	//Interface setup
	public ArduinoCom(){
		

		setupInterface();

	}

	public static void main(String[] args) {
		
		ArduinoCom arduino=new ArduinoCom();
		arduino.setVisible(true);
		
		arduino.initializePortCom(arduino);
		
		
		
		
		
		arduino.initializeLists();
		arduino.readMovementDataFromFile();
		
		if(arduino.data.isBlank()) {
		/*
		 * writes Arff Data header to "data" String
		 * with 4 attributes at 6 sensor values for 2 sensors
		 */
		arduino.data+=" @relation Sittingmovements\n";
		String[] a= {"X","Y","Z", "X2", "Y2", "Z2"};
		String[] b= {"_Gyro","_Accel"};
		for(int i=0; i<2;i++) {
			for(int j=0;j<6;j++) {
			arduino.data+="@attribute MIN"+b[i]+a[j]+" numeric\r\n" + 
					"@attribute MAX"+b[i]+a[j]+" numeric\r\n" + 
					"@attribute Standard_deviation"+b[i]+a[j] +" numeric\r\n" + 
					"@attribute Median_value"+b[i]+a[j] +" numeric\r\n"
			
			;
			}
		
		}
		arduino.data+= "@attribute class {FrontBack, FrontBack-unlocked, LeftRight, DoNothing, Spin, Roll_around, SitDown, StandUp}\n"
				+"@data\n";	
		}
	
		try {
			//read for 15 sec
			Thread.sleep(150000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		
		arduino.port.removeDataListener();
		arduino.port.closePort();
		System.exit(0);
		
		
		//reads csvFile and Writes arffFile
		

		
		
	}
	public void readMovementDataFromFile(){
		try {
			BufferedReader arffReader=new BufferedReader(new FileReader(pathToCsv));
			String line;
			while((line=arffReader.readLine())!=null) {
				data+=line+"\r\n";
			}
			arffReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void initializePortCom(ArduinoCom arduino) {
		port=SerialPort.getCommPort(portName);
		port.setComPortParameters(115200, 8,1,0);
		//port.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0 , 0);

		if(port.openPort()) {
			System.out.println("Port successfully opened!");
		}
		else {
		while(!port.openPort())
			try {
				System.out.println("Port is not open");
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		port.addDataListener(arduino);
		
	}
	
	public void initializeLists() {
		allLists.add(gyroXList);allLists.add(gyroYList);allLists.add(gyroZList);allLists.add(gyroX2List);allLists.add(gyroY2List);allLists.add(gyroZ2List);
		allLists.add(accelXList);allLists.add(accelYList);allLists.add(accelZList);allLists.add(accelX2List);allLists.add(accelY2List);allLists.add(accelZ2List);
	}
	
	@Override
	public int getListeningEvents() {
		
		return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
	}
	@Override
	public void serialEvent(SerialPortEvent event) {
		if (event.getEventType()==SerialPort.LISTENING_EVENT_DATA_AVAILABLE ) {
			byte[] data= new byte[port.bytesAvailable()];
			if(port.bytesAvailable()==0)
				return;
			port.setComPortTimeouts(160000, 0, 0);
			/*
			Scanner s=new Scanner(port.getInputStream()).useDelimiter("\\A");
			
			outputOneMovement+=s.hasNext()? s.next() : " ";
			*/
			/*
			StringBuilder sb= new StringBuilder();
			BufferedReader br=new BufferedReader(new InputStreamReader(port.getInputStream()));
			String read;
			System.out.println("New");
			try {
				while((read=br.readLine())!=null) {
					sb.append(read);
					System.out.println(read);
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//System.out.println(sb);
			*/
			
			port.readBytes(data, data.length);
			for(int i=0;i<data.length;i++) {
			output+=(char)data[i];
			outputOneMovement+=(char)data[i];
			System.out.print((char)data[i]);
			
			}
			
		}
		

		
	}
	
	/*calculates the Movement after every Button Click and sets the output for
	 * the arff file
	 */
	//@movement the movement made, characterised by Button
	public void outputToArff(String movement) {
		Calculate cal=new Calculate();
		String line;
		
		
	try {
		
			
		String[] lineArray;
		//read first Line of data e.g. LR,FB
		this.movement=movement;
		
		
		int stepCount=0;
		while(true){
			int j=0;
			serialReader=new BufferedReader(new StringReader(outputOneMovement));
			if(movement.equalsIgnoreCase("SkipLastData")) {
				System.out.println(data);
				for(List<String>list: allLists) {
					list.clear();
				}
				return;
			}
			if((line=serialReader.readLine()).startsWith("nullets"))
				while(!(line=serialReader.readLine()).equalsIgnoreCase("databegin")) {
				}
		while(j<stepCount) {
			line=serialReader.readLine();
			j++;
		}
		/*
		 * either reads until serial is finished or 
		 * ends and writes data to file
		 */
		for(int i=0;i<windowSize;i++) {
			if((line = serialReader.readLine()) != null) {
				lineArray = line.split(";");
					accelXList.add(lineArray[0]);
					accelYList.add(lineArray[1]);
					accelZList.add(lineArray[2]);
					gyroXList.add(lineArray[3]);
					gyroYList.add(lineArray[4]);
					gyroZList.add(lineArray[5]);
					accelX2List.add(lineArray[6]);
					accelY2List.add(lineArray[7]);
					accelZ2List.add(lineArray[8]);
					gyroX2List.add(lineArray[9]);
					gyroY2List.add(lineArray[10]);
					gyroZ2List.add(lineArray[11]);
				
			}
			else {
				serialReader.close();
				outputOneMovement="";

				try(PrintWriter writer= new PrintWriter(new File(pathOut))){
					writer.write(data);
				}
				catch (Exception e) {
					System.out.println(e);
				}
				
				System.out.println(data);
				for(List<String>list: allLists) {
					list.clear();
				}
				
				return;
			};
			
		}
		stepCount+=stepSize;
		
		/*
		 * at the end of each window
		 * calculate all values min, max, mean, sd for all sensors(GyroX,Y,Z, Accel etc.)
		 */
		for(List<String> element:allLists) 
			
		data+=Arrays.toString(cal.calculateValues(element))+",";
		data+=" "+movement+"\n";
		data=data.replace("[", " ");
		data=data.replace("]", "");

		//reset all lists
		for(List<String>list: allLists) {
			list.clear();
		}
		
		
	}
	}
	catch(IOException e) {
		e.printStackTrace();
	}
			
	}

	@Override
	public int getPacketSize() {
		// TODO Auto-generated method stub
		return 100;
	}
	
	
	private void setupInterface() {
		JButton b1=new JButton("LeftRight");
		
		b1.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				outputToArff(((JButton)e.getSource()).getActionCommand());
				
			}
		});
JButton b2=new JButton("FrontBack");
		
		b2.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				outputToArff(((JButton)e.getSource()).getActionCommand());
				
			}
		});
JButton b3=new JButton("FrontBack-unlocked");
		
		b3.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				outputToArff(((JButton)e.getSource()).getActionCommand());
				
			}
		});
JButton b4=new JButton("Spin");
		
		b4.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				outputToArff(((JButton)e.getSource()).getActionCommand());
				
			}
		});
JButton b5=new JButton("Roll-Around");
		
		b5.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				outputToArff(((JButton)e.getSource()).getActionCommand());
				
			}
		});	
JButton b6=new JButton("DoNothing");
		
		b6.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				outputToArff(((JButton)e.getSource()).getActionCommand());
				
			}
		});
	
	JButton b7=new JButton("StandUp");
	
	b7.addActionListener(new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			outputToArff(((JButton)e.getSource()).getActionCommand());
		}
	});
JButton b8=new JButton("SitDown");
	
	b8.addActionListener(new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			outputToArff(((JButton)e.getSource()).getActionCommand());
		}
	});
JButton b9=new JButton("SkipLastData");
	
	b9.addActionListener(new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			outputToArff(((JButton)e.getSource()).getActionCommand());
		}
	});
	this.setTitle("Press to calculate movement Data!");
	this.setSize(400, 200);
	JPanel panel=new JPanel();
	JLabel label=new JLabel();
	
	panel.add(b1);
	panel.add(b2);
	panel.add(b3);
	panel.add(b4);
	panel.add(b5);
	panel.add(b6);
	panel.add(b7);
	panel.add(b8);
	panel.add(b9);
	panel.add(label);
	this.add(panel);
}
}
	



/** 
 * structure:
 * [Bewegungsname]zB. RollLR
 * AccelX, y, Z, gyroX, Y,Z,AccelX2, y2, Z2, gyroX2, Y2,Z2
 * AccelX, y, Z, gyroX, Y,Z,AccelX2, y2, Z2, gyroX2, Y2,Z2
 * ...
 *
 * 	
 * 
 */

/**
 * gewünschte Ausgabe: arff
 * [Koordinate/Bewegungsdimension] zB.GyroX
 * Min, Max, Mittelwert, Standardabweichung, Bewegungsname zB LR
 * Min, Max, Mittelwert, Standardabweichung, Bewegungsname zB LR
 * Min, Max, Mittelwert, Standardabweichung, Bewegungsname zB FB
 * Min, Max, Mittelwert, Standardabweichung, Bewegungsname zB FB
 * Min, Max, Mittelwert, Standardabweichung, Bewegungsname zB Roll
 * Min, Max, Mittelwert, Standardabweichung, Bewegungsname zB Turn
 * 
 * 
 * 
 * 
 *
 */

