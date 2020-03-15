package main;

import java.io.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.bind.DatatypeConverter;

public class D extends Thread {

	public static final String OK = "OK";
	public static final String ALGORITMOS = "ALGORITMOS";
	public static final String CERTSRV = "CERTSRV";
	public static final String CERCLNT = "CERCLNT";
	public static final String SEPARADOR = ":";
	public static final String HOLA = "HOLA";
	public static final String INICIO = "INICIO";
	public static final String ERROR = "ERROR";
	public static final String REC = "recibio-";
	public static final int numCadenas = 8;

	// Atributos
	private Socket sc = null;
	private String dlg;
	private byte[] mybyte;
	private static File file;
	private static X509Certificate certSer;
	private static KeyPair keyPairServidor;
	private ThreadCargaCPU monitor;
	private byte[] bytes;
	
	public static void init(X509Certificate pCertSer, KeyPair pKeyPairServidor, File pFile) {
		certSer = pCertSer;
		keyPairServidor = pKeyPairServidor;
		file = pFile;
	}
	
	public D (Socket csP, int idP, ThreadCargaCPU monitorP,byte[] bytes) {
		monitor = monitorP;
		sc = csP;
		dlg = new String("delegado " + idP + ": ");
		this.bytes=bytes;
		try {
		mybyte = new byte[520]; 
		mybyte = certSer.getEncoded();
		} catch (Exception e) {
			System.out.println("Error creando encoded del certificado para el thread" + dlg);
			e.printStackTrace();
		}
	}
	

	
	/*
	 * Generacion del archivo log. 
	 * Nota: 
	 * - Debe conservar el metodo como está. 
	 * - Es el único metodo permitido para escribir en el log.
	 */
	private void escribirMensaje(String pCadena) {
		
		try {
			FileWriter fw = new FileWriter(file,true);
			fw.write(pCadena + "\r");
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void run() {
		String[] cadenas;
		cadenas = new String[numCadenas];
		String linea;
	    System.out.println(dlg + "Empezando atencion.");
	        try {
	        	
				PrintWriter ac = new PrintWriter(sc.getOutputStream() , true);
				BufferedReader dc = new BufferedReader(new InputStreamReader(sc.getInputStream()));
				monitor.activar();
				/***** Fase 1:  *****/
				linea = dc.readLine();
				cadenas[0] = "Fase1: ";
				if (!linea.equals(HOLA)) {
					ac.println(ERROR);
				    sc.close();
					throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
				} else {
					ac.println(OK);
					cadenas[0] = dlg + REC + linea + "-continuando.";
					System.out.println(cadenas[0]);
				}
				linea = dc.readLine();

				this.wait();
				/***** Fase 2:  *****/
			
				 OutputStream out;
				 //init array with file length
				 if(linea.contentEquals("LISTO")) {
		         out = sc.getOutputStream();
		         out.write(bytes,0,bytes.length);
		         out.flush();
		         ac.println(bytes.hashCode());
				 }else {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
				 }
				 
	        } catch (Exception e) {
	        	monitor.liberar();
	        	synchronized(this){
	        	String log = "";
	          log+=("Transaccion Perdida");
	          e.printStackTrace();
	          	for(int i=0; i < numCadenas; i++) {
	          		if(cadenas[i] != null && cadenas[i] != "") {
	          			log+=(cadenas[i]+"\r\n");
	          		}
	          	}
	          	log+=(e.getMessage()+"\r\n//\r\n");
	          	escribirMensaje(log);}
	          }
	        
	}
	
	public static String toHexString(byte[] array) {
	    return DatatypeConverter.printBase64Binary(array);
	}

	public static byte[] toByteArray(String s) {
	    return DatatypeConverter.parseBase64Binary(s);
	}
	
	public static double getSystemCpuLoad() throws Exception{
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
		AttributeList list = mbs.getAttributes(name, new String[] {"SystemCpuLoad"});
		if(list.isEmpty()) 	return Double.NaN;
		
		Attribute att = (Attribute) list.get(0);
		Double val = (Double) att.getValue();
		
		if(val == -1.0) return Double.NaN;
		
		return ((int)(val *1000) /10.0);
	}
	
}
