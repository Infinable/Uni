#include <Wire.h>
#include <WiFi.h>

int16_t accelX, accelY, accelZ;
float accOffX,accOffY,accOffZ, accOffX2,accOffY2,accOffZ2;
double roll, pitch, yaw,rollA,pitchA;
//G=m^2/s
float  accelXinG,accelYinG,accelZinG,accelXinG2,accelYinG2,accelZinG2;
//default output for accel/gyro -32768-+32768

float temperature;
int16_t gyroX, gyroY, gyroZ;
//degrees per second
float gyroXinDgps,gyroYinDgps,gyroZinDgps,gyroXinDgps2,gyroYinDgps2,gyroZinDgps2;
float gyrOffX, gyrOffY,gyrOffZ, gyrOffX2, gyrOffY2, gyrOffZ2;
const int I2C=0x68;
const byte I2C2=0x69;
float elapsedTime, currentTime, previousTime;

String fulldata="";
const int datasize=1;
int ind=0;
int count=0;

WiFiServer server(80);
String header;

const char * networkName="o2-WLAN77";
const char * password="H8B7386B94V93YX3";

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  WiFi.begin(networkName, password);
  while(WiFi.status()!=WL_CONNECTED){
    delay(500);
    Serial.println("Connecting to WiFi..");
  }
  
  
  Serial.println("Connected to Wifi.");
  Serial.print("IP-Adress: ");
  Serial.println(WiFi.localIP());
  server.begin();
  
  Wire.begin();
  setupMPU(I2C);
  setupMPU(I2C2);
  
  calculateOffset(I2C);
  calculateOffset(I2C2);
  Serial.println("databegin");
  
}

void printout(String a, float n1, String b, float n2, String c, float n3){
  Serial.print(a);
  Serial.print(n1);
  Serial.print(b);
  Serial.print(n2);
  Serial.print(c);
  Serial.print(n3);
}

void loop() {
  // put your main code here, to run repeatedly:
  
  for(int i=0;i<10;i++){
  recordAccelerometer(I2C);
  recordAccelerometer(I2C2);
  }
  //index for printing every 10th value
  ind++;
  if(ind==datasize){
    ind=0;
    
    fulldata+="\nGyroX: ";
    fulldata+= gyroXinDgps;
    fulldata+="\nGyroY: ";
    fulldata+= gyroYinDgps;
    fulldata+= "\nGyroZ: ";
    fulldata+=gyroZinDgps;

    fulldata+="\nGyroX2: ";
    fulldata+= gyroXinDgps2;
    fulldata+="\nGyroY2: ";
    fulldata+= gyroYinDgps2;
    fulldata+= "\nGyroZ2: ";
    fulldata+=gyroZinDgps2;
    fulldata+="\n";
    //fulldata+=count++;
    fulldata+= "AccelX: ";
    fulldata+=accelXinG;
    fulldata+="\nAccelY: ";
    fulldata+=accelYinG;
    fulldata+="\nAccelZ: ";
    fulldata+=accelZinG;

    fulldata+= "\nAccelX2: ";
    fulldata+=accelXinG2;
    fulldata+="\nAccelY2: ";
    fulldata+=accelYinG2;
    fulldata+="\nAccelZ2: ";
    fulldata+=accelZinG2;
    

    printout("",accelXinG,"; ",accelYinG, "; ",accelZinG);
    printout("; ",gyroXinDgps,"; ", gyroYinDgps,"; ",gyroZinDgps);
    printout("; ",accelXinG2,"; ",accelYinG2, "; ",accelZinG2);
    printout("; ",gyroXinDgps2,"; ", gyroYinDgps2,"; ",gyroZinDgps2);
    Serial.println("");
  }
  
  WiFiClient client= server.available();

  if(client){
    Serial.println("new client.");
    String currentLine="";
    while(client.connected()){
      if(client.available()){
        char c=client.read();
        
        header+=c;
        if(c=='\n'){ //end of request
          Serial.write("Clients request: ");
          Serial.print(header);
          client.println("HTTP/1.1 200 OK");
          client.println("Content-type: text/html");
          client.println("Connection: close");
          client.println();
          
          if(header.indexOf("GET /data")>=0){
            Serial.println("test3");
            Serial.println(accelXinG);
            
         
          /*client.println("<!DOCTYPE HTML>");
          client.println("<html>");
          client.println("<link rel=\"icon\" href=\"data:,\">");
          //client.print("<meta http-equiv=\"refresh\" content=\"1\">");
          */
      
          client.print(fulldata);
          fulldata="";
          count=0;
          /*
          client.print("<p> X: "); client.println(accelXinG);
          client.print("</p><p>Y: "); client.println(accelYinG);
          client.print("</p><p>Z: "); client.println(accelZinG);
          client.print("</p>");
          client.print("<p>Pitch: "); client.println(pitchA);
          client.print("</p>Roll: "); client.println(rollA);
          client.print("</p>");
          client.println("Gyroskopwerte: ");
          client.print("<p>X "); client.println(gyroXinDgps);
          client.print("</p><p>Y "); client.println(gyroYinDgps);
          client.print("</p><p>Z "); client.println(gyroZinDgps);
          client.print("</p>");*/
          client.stop();

          /*
          client.println("<br />");
          client.println("</HTML>");
          */
          delay(10);
          }
        }
        else if(c=='r'){
          break;
        }
       
      }
      //client.stop();
    }
  }
  //every 0.1 sec a value is recorded
  delay(100);

}
  
void setupMPU(byte I2C){
  Wire.beginTransmission(I2C); //I2C adress of MPU
  Wire.write(0x6B);   //Register Adress for Power, otherwise at the start in sleep mode
  Wire.write(0x00); //no sleep mode
  Serial.print("transmission: ");
  Serial.println(Wire.endTransmission());
  Wire.beginTransmission(I2C);
  Wire.write(0x1C);
  Wire.write(0b00000000); //sets accelerometer to +-2g
  Serial.print("setting up Accelerometer ");
  Serial.println(Wire.endTransmission());
  Wire.beginTransmission(I2C);
  Wire.write(0x1B); //gyr register
  Wire.write(0b00000000); //range 250 degrees/second
  Wire.endTransmission();
  Serial.println("I2C-Adress:");
  Serial.println(I2C);
  
}

void recordAccelerometer(byte I2C){
  Wire.beginTransmission(I2C);
  Wire.write(0x3B); //Accel Register X2,Y2,Z2
  Wire.endTransmission();
  //Serial.print("Gyroscope at "); Serial.println(I2C); 
  //Serial.print("requested Bytes: ");
  //1-6 xyz acc, 7,8 temp  9-14 gyr
  Wire.requestFrom(I2C,14, true);
  accelX=Wire.read()<<8|Wire.read();
  accelY=Wire.read()<<8|Wire.read();
  accelZ=Wire.read()<<8|Wire.read();
  /*
   *  Accelerometer: X,Y,Z Beschleunigungswerte (g) m/s^2
   *  Gyroskop: Rotationsgeschwindigkeit (°/s)
   */
   if(I2C==0x68){
  accelXinG=accelXinG*0.9+0.1*((accelX-accOffX)/16384.0);
  accelYinG=accelYinG*0.9+0.1*((accelY-accOffY)/16384.0);
  accelZinG=accelZinG*0.9+0.1*((accelZ-accOffZ+16384)/16384.0);
  }
  else{
    accelXinG2=accelXinG2*0.9+0.1*((accelX-accOffX2+16384)/16384.0); //+1Offset in the X axis of sensor stands vertically sideways(connector at the bottom)
  
  accelYinG2=accelYinG2*0.9+0.1*((accelY-accOffY2)/16384.0);
 
  accelZinG2=accelZinG2*0.9+0.1*((accelZ-accOffZ2)/16384.0);  
  }
  pitchA=((atan((accelYinG) / sqrt(pow((accelXinG), 2) + pow((accelZinG), 2))) * 180.0 / PI));
  rollA=((atan(-1 * (accelXinG) / (accelZinG)) * 180 / PI));
  temperature=(Wire.read()<<8|Wire.read())/340.0+36.53; 
  //Serial.print("Temperature in °C: ");
  //Serial.println(temperature/340.0+36.53); //from mpu documentation

   //gyroscope and angle calculation
  previousTime=currentTime;
  currentTime=millis();
  elapsedTime=(currentTime-previousTime) /1000; //time in sec;

  gyroX=Wire.read()<<8|Wire.read();
  gyroY=Wire.read()<<8|Wire.read();
  gyroZ=Wire.read()<<8|Wire.read();
  //Serial.print("Gyro X: ");
  if(I2C==0x68){
  gyroXinDgps=gyroXinDgps*0.9+0.1*((gyroX-gyrOffX)/131.0);

  
  gyroYinDgps=gyroYinDgps*0.9+0.1*((gyroY-gyrOffY)/131.0);
 gyroZinDgps=gyroZinDgps*0.9+0.1*((gyroZ-gyrOffZ)/131.0);
  }
  else{
    gyroXinDgps2=gyroXinDgps2*0.9+0.1*((gyroX-gyrOffX2)/131.0);


  gyroYinDgps2=gyroYinDgps2*0.9+0.1*((gyroY-gyrOffY2)/131.0);
  gyroZinDgps2=gyroZinDgps2*0.9+0.1*((gyroZ-gyrOffZ2)/131.0);
  }
  //write data to data arrays
  
  pitch=  pitch+gyroXinDgps*elapsedTime;
  roll= roll+gyroYinDgps*elapsedTime;
  yaw= yaw+gyroZinDgps*elapsedTime;

  /*  Serial.println("Roll, pitch, yaw: ");
  Serial.print(roll); Serial.print(" ");
  Serial.print(pitch);Serial.print(" ");
  Serial.print(yaw); Serial.print(" ");
  */
}

void calculateOffset(byte I2C){ //also outputs accel, temperature and gyro values into Serial
  //loop 200 times for average offset value
  int iter=20;
  for(int i=0;i<iter;i++){
  Wire.beginTransmission(I2C);
  Wire.write(0x3B); //Accel Register X2,Y2,Z2
  Wire.endTransmission();
  Serial.print("requested Bytes: ");
  Serial.println(Wire.requestFrom(I2C,14, true)); //1-6 xyz acc, 7,8 temp  9-14 gyr
  accelX=Wire.read()<<8|Wire.read();
  accelY=Wire.read()<<8|Wire.read();
  accelZ=Wire.read()<<8|Wire.read();
  /*
   *  Accelerometer: X,Y,Z Beschleunigungswerte (g)
   *  Gyroskop: Rotationsgeschwindigkeit (°/s)
   */
  Serial.print("Accel X=");
  Serial.println(accelX/16384.0);
  Serial.print("Accel Y=");
  Serial.println(accelY/16384.0);
  Serial.print("Accel Z=");
  Serial.println(accelZ/16384.0);
  temperature=Wire.read()<<8|Wire.read(); 
  Serial.print("Temperature in °C: ");
  Serial.println(temperature/340.0+36.53); //from mpu documentation
  gyroX=Wire.read()<<8|Wire.read();
  gyroY=Wire.read()<<8|Wire.read();
  gyroZ=Wire.read()<<8|Wire.read();
  Serial.print("Gyro X: ");
  Serial.println(gyroX/131.0);
  Serial.print("Gyro Y: ");
  Serial.println(gyroY/131.0);
  Serial.print("Gyro Z: ");
  Serial.println(gyroZ/131.0);
  if(I2C==0x68){
    accOffX+=accelX;
    accOffY+=accelY;
    accOffZ+=accelZ;

    gyrOffX+=gyroX;
    gyrOffY+=gyroY;
    gyrOffZ+=gyroZ;
  }
  else{
    accOffX2+=accelX;
    accOffY2+=accelY;
    accOffZ2+=accelZ;

    gyrOffX2+=gyroX;
    gyrOffY2+=gyroY;
    gyrOffZ2+=gyroZ;
  }
  
  }
  if(I2C==0x68){

    accOffX/=iter;
    accOffY/=iter;
    accOffZ/=iter;

    gyrOffX/=iter;
    gyrOffY/=iter;
    gyrOffZ/=iter;
  }
  else{
    accOffX2/=iter;
    accOffY2/=iter;
    accOffZ2/=iter;

    gyrOffX2/=iter;
    gyrOffY2/=iter;
    gyrOffZ2/=iter;
  }

  Serial.println("-------------------Accelerometer and Gyroscope Offsets");
  Serial.println(accOffX);
  Serial.println(accOffY);
  Serial.println(accOffZ);

  Serial.println(gyrOffX);
  Serial.println(gyrOffY);
  Serial.println(gyrOffZ);

  Serial.println("MPU2");
    Serial.println(accOffX2);
  Serial.println(accOffY2);
  Serial.println(accOffZ2);
 Serial.println(gyrOffX2);
  Serial.println(gyrOffY2);
  Serial.println(gyrOffZ2);

  
}
