package com.example.androidusbhostarduino;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.nio.ByteBuffer;

public class MainActivity extends ActionBarActivity implements Runnable {

	private static final int CMD_ROBO_ON_OFF = 49;
	private static final int CMD_SENSOR_TRF = 50;
	private static final int CMD_SENSOR_PING = 51;
	private static final int CMD_GIROSCOPIO = 52;
	//private static final int CMD_LED_ON_OFF = 5;
	private static final int CMD_MOTOR_C = 53;
	//private static final int CMD_PARAR = 6;
	private static final int CMD_FRENTE = 54;
	private static final int CMD_ESQUERDA = 55;
	private static final int CMD_DIREITA = 56; //0x0a
	private static final int CMD_RE = 57;  //0b
	private static final int CMD_ALINHAR_ESQUERDA = 48;  //0c
	private static final int CMD_ALINHAR_DIREITA = 61;  //0d

	SeekBar bar;
	ToggleButton buttonLed;

	int liga =0;
	int s = 5;
	int comandoTRF;
	int comandoPing;
	int giroscopio;
	boolean ligarPing = true;
	boolean virarEsquerda = false;
	boolean afterRe = false;

	private UsbManager usbManager;
	private UsbDevice deviceFound;
	private UsbDeviceConnection usbDeviceConnection;
	private UsbInterface usbInterfaceFound = null;
	private UsbEndpoint endpointOut = null;
	private UsbEndpoint endpointIn = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		bar = (SeekBar) findViewById(R.id.seekbar);
		buttonLed = (ToggleButton) findViewById(R.id.arduinoled);
		buttonLed.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
				if (isChecked) {
					sendCommandLED(CMD_ROBO_ON_OFF);
					sendCommandMotoresDE(CMD_FRENTE);
					liga = 1;
					s = 0;
					Toast.makeText(MainActivity.this, " Ligou", Toast.LENGTH_SHORT).show();
				} else {
					sendCommandLED(CMD_ROBO_ON_OFF);
					//sendCommand(CMD_PARAR);
					liga = 0;
					s = 4;
					Toast.makeText(MainActivity.this, "Desligou ", Toast.LENGTH_SHORT).show();
				}
			}
		});
		usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
	}

	@Override
	public void onResume() {
		super.onResume();

		Intent intent = getIntent();
		String action = intent.getAction();

		UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			setDevice(device);
		} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
			if (deviceFound != null && deviceFound.equals(device)) {
				setDevice(null);
			}
		}
	}

	private void setDevice(UsbDevice device) {
		usbInterfaceFound = null;
		endpointOut = null;
		endpointIn = null;

		for (int i = 0; i < device.getInterfaceCount(); i++) {
			UsbInterface usbif = device.getInterface(i);

			UsbEndpoint tOut = null;
			UsbEndpoint tIn = null;

			int tEndpointCnt = usbif.getEndpointCount();
			if (tEndpointCnt >= 2) {
				for (int j = 0; j < tEndpointCnt; j++) {
					if (usbif.getEndpoint(j).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
						if (usbif.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_OUT) {
							tOut = usbif.getEndpoint(j);
						} else if (usbif.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_IN) {
							tIn = usbif.getEndpoint(j);
						}
					}
				}
				if (tOut != null && tIn != null) {
					// This interface have both USB_DIR_OUT
					// and USB_DIR_IN of USB_ENDPOINT_XFER_BULK
					usbInterfaceFound = usbif;
					endpointOut = tOut;
					endpointIn = tIn;
				}
			}
		}

		if (usbInterfaceFound == null) {
			return;
		}

		deviceFound = device;

		if (device != null) {
			UsbDeviceConnection connection =
					usbManager.openDevice(device);
			if (connection != null &&
					connection.claimInterface(usbInterfaceFound, true)) {
				usbDeviceConnection = connection;
				Thread thread = new Thread(this);
				thread.start();

			} else {
				usbDeviceConnection = null;
			}
		}
	}

	private void sendCommandLED(int control) {
		synchronized (this) {

			if (usbDeviceConnection != null) {
				byte[] message = new byte[3];
				message[0] = (byte) control;
				usbDeviceConnection.bulkTransfer(endpointOut,
						message, message.length, 0);
			}
		}
		TextView scoreView = (TextView) findViewById(R.id.RxCmdLED);
		scoreView.setText(String.valueOf(control));
	}

	private void sendCommandMotoresDE(int control) {
		synchronized (this) {

			if (usbDeviceConnection != null) {
				byte[] message = new byte[3];
				message[1] = (byte) control;
				usbDeviceConnection.bulkTransfer(endpointOut,
						message, message.length, 0);
			}
		}
		TextView scoreView = (TextView) findViewById(R.id.RxCmdMotoresDE);
		scoreView.setText(String.valueOf(control));
	}

	private void sendCommandMotorC(int control) {
		synchronized (this) {

			if (usbDeviceConnection != null) {
				byte[] message = new byte[3];
				message[2] = (byte) control;
				usbDeviceConnection.bulkTransfer(endpointOut,
						message, message.length, 0);
			}
		}
		TextView scoreView = (TextView) findViewById(R.id.MotorC);
		scoreView.setText(String.valueOf(control));
	}

	@Override
	public void run() {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		UsbRequest request = new UsbRequest();
		request.initialize(usbDeviceConnection, endpointIn);
		while (true) {
			if(s == 0){
				sendCommandLED(CMD_SENSOR_TRF);
			} else if(s == 1){
				sendCommandLED(CMD_SENSOR_PING);
			} else if(s == 2){
				sendCommandLED(CMD_GIROSCOPIO);
			} else if(s == 3){
				algoritmo();
			} else{
			} if (request.queue(buffer, 1) == true) {
				if (usbDeviceConnection.requestWait() == request) {
					byte rxCmd = buffer.get(0);
					//displayRxCmd(rxCmd);
					if (rxCmd != 0) {
						if (liga == 1){
							bar.setProgress((int) rxCmd);
							if (s == 0) {
								if(rxCmd < 25){
									comandoTRF = 1;
								}
								if(rxCmd > 24){
									comandoTRF = 2;
								}
								s++;
							} else if (s == 1) {
								if(rxCmd < 9){
									comandoPing = 1;
								}
								if(rxCmd > 8 && rxCmd < 14){
									comandoPing = 2;
								}
								if(rxCmd > 13 && rxCmd < 26){
									comandoPing = 3;
								}
								if(rxCmd > 25 && rxCmd < 51){
									comandoPing = 4;
								}
								if(rxCmd > 50){
									comandoPing = 5;
								}
								s++;
							} else if (s == 2) {
								if(rxCmd > -21 && rxCmd < 7){
									giroscopio = 1;
								}
								if(rxCmd < -20){
									giroscopio = 2;
								}
								if(rxCmd > 6){
									giroscopio = 3;
								}
								//algoritmo
								s++;
							} else if(s == 3){
								if(rxCmd == 3){
									Toast.makeText(MainActivity.this, "Executou movimento", Toast.LENGTH_SHORT).show();
								}
								s = 0;
							} else if(s == 4) {
								if (rxCmd == 1) {
									Toast.makeText(MainActivity.this, "Ligado", Toast.LENGTH_SHORT).show();
								} else if (rxCmd == 2) {
									Toast.makeText(MainActivity.this, "Desligado ", Toast.LENGTH_SHORT).show();
								}
								s = 0;
							}
						}
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				} else {
					break;
				}
			}
				/*if (request.queue(buffer, 2) == true){
				if (usbDeviceConnection.requestWait() == request) {
					byte rxCmd2 = buffer.get(1);
					if(rxCmd2 != 0){

					}
				}
			}*/
			else {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Alerta");
				builder.setMessage("Dados não recebidos");
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
						Toast.makeText(MainActivity.this, "Verifique conexão", Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}

	public void algoritmo(){
		if(giroscopio == 1){  //nao z
			if(ligarPing == false){     //ping false
				if(comandoTRF == 2){
					//7
					if(comandoPing < 4){
						ligarPing = true;
						sendCommandLED(CMD_ROBO_ON_OFF);//Led ON
						//direita apos quina
					}
					//6
					else if(comandoPing > 3){
						sendCommandLED(CMD_ROBO_ON_OFF);//Led OFF
						sendCommandMotoresDE(CMD_FRENTE);  //frente -3
					}
				}
				else if(comandoTRF == 1){
					ligarPing = true;
					sendCommandMotoresDE(CMD_ESQUERDA); //esquerda -5
				}
			}
			else if(ligarPing == true && virarEsquerda == false){   //esquerda false -- ping true

				//1  4  8
				if(comandoTRF == 2 && comandoPing < 4){     //trf 2 --- ping 1,2,3
					sendCommandMotoresDE(CMD_FRENTE);  //frente -3
				}
				//2  9
				else if(comandoTRF == 1 && comandoPing < 4){    //trf 1 --- ping 1,2,3
					sendCommandLED(CMD_ROBO_ON_OFF); //parar -7
					sendCommandMotorC(CMD_MOTOR_C); //0°
					virarEsquerda = true;
				}
				//5
				else if(comandoPing == 4){      //ping 4
					ligarPing = false;
					sendCommandMotoresDE(CMD_DIREITA); //direita -4
				}
				else if(comandoPing == 5){      //ping 5
					ligarPing = false;
					sendCommandMotoresDE(CMD_DIREITA); //direita -4
				}
			}
			else if (ligarPing == true && virarEsquerda == true){   //esquerda true -- ping true
				//10
				if(comandoPing < 4){
					afterRe = true;
					sendCommandMotoresDE(CMD_RE);  // Re -6
				}
				//3
				else if(comandoPing == 4 && afterRe == false){
					virarEsquerda = false;
					sendCommandMotorC(CMD_MOTOR_C); //180°
					sendCommandMotoresDE(CMD_ESQUERDA); //esquerda -5
				}
				else if(comandoPing == 5 && afterRe == false){
					virarEsquerda = false;
					sendCommandMotorC(CMD_MOTOR_C); //180°
					sendCommandMotoresDE(CMD_ESQUERDA); //esquerda -5
				}
				//11
				else if(comandoPing > 3 && afterRe == true){
					//delay(1200);
					afterRe = false;
					virarEsquerda = false;
					ligarPing = false;
					sendCommandMotorC(CMD_MOTOR_C); //180°
					sendCommandMotoresDE(CMD_ESQUERDA); //esquerda -5
				}
			}
		}
		if(giroscopio == 2){
			afterRe = false;
			sendCommandMotoresDE(CMD_ALINHAR_ESQUERDA); //girar p/esquerda -9
		}
		if(giroscopio == 3){

			afterRe = false;
			sendCommandMotoresDE(CMD_ALINHAR_DIREITA); //girar p/direita -8
		}
	}
}

