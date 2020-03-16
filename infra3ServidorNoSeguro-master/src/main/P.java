package main;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class P {
	private static ServerSocket ss;	
	private static final String MAESTRO = "MAESTRO: ";
	private static X509Certificate certSer; /* acceso default */
	private static KeyPair keyPairServidor; /* acceso default */
	private static final int numeroThreads = 25;
	private static int cont=1;
	static Object o=new Object();
	static Object p=new Object();
	public static String log = "";
	public static String ruta = "./resultados.txt";
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub

		System.out.println(MAESTRO + "Establezca puerto de conexion:");
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		int ip = Integer.parseInt(br.readLine());
		System.out.println(MAESTRO + "Empezando servidor maestro en puerto " + ip);
		// Adiciona la libreria como un proveedor de seguridad.
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());		
		
		// Crea el archivo de log
		File file = null;
		keyPairServidor = S.grsa();
		certSer = S.gc(keyPairServidor);
		
   
        file = new File(ruta);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter fw = new FileWriter(file);
        fw.close();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");  
        Date date = new Date();  
        log+="Fecha: "+formatter.format(date)+"\r\n";
        D.init(certSer, keyPairServidor, file);
        ExecutorService executor = Executors.newFixedThreadPool(numeroThreads);
        
		// Crea el socket que escucha en el puerto seleccionado.
		ss = new ServerSocket(ip);
		System.out.println(MAESTRO + "Socket creado.");
		ThreadCargaCPU tcpu = new ThreadCargaCPU();
		tcpu.start();
		 
		 //init array with file length
		 byte[] bytesArray;
		
         
         
		 System.out.println("Elija un archivo para enviar (1 o 2):");
		 System.out.println("1.Archivo de texto 250MB");
		 System.out.println("2.Archivo de texto 100MB");
		 String send="";
		 if(br.readLine().equals("1")) {
			  
			 send="./data/Archivo1.txt";
			 log+="Archivo Enviado: archivo1 \r\n";
			 File ej=new File("./data/Archivo1.txt");
			 log+="Tamaño archivo: "+ej.length();
		 }else {
			 send="./data/Archivo2.txt";
			 log+="Archivo Enviado: archivo2 \r\n";
			 File ej=new File("./data/Archivo2.txt");
			 log+="Tamaño archivo: "+ej.length();
		 }
		 
		 File sendFile = new File(send);
		 bytesArray = new byte[(int) sendFile.length()]; 
		 FileInputStream fis = new FileInputStream(sendFile);
		 BufferedInputStream bis = new BufferedInputStream(fis);
		 bis.read(bytesArray,0,bytesArray.length);
		 System.out.println("¿A cuantos clientes desea enviar el archivo?");
		 String clientes=br.readLine();
		 ArrayList<D> clients=new ArrayList<D>();
		 try {
				FileWriter fw1 = new FileWriter(new File(ruta),true);
				fw1.write(log + "\r");
				fw1.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		 
		for (int i=0;true;i++) {
			
			if(cont>=Integer.parseInt(clientes)) {
				synchronized(p) {
					p.wait();	
					}
				synchronized(o) {
					o.notifyAll();
				}
				
			}
			
			try { 
				log="";
				Socket sc = ss.accept();
				System.out.println(MAESTRO + "Cliente " + i + " aceptado.");
				log+=MAESTRO + "Cliente " + i + " aceptado.";
				 try {
						FileWriter fw1 = new FileWriter(new File(ruta),true);
						fw1.write(log + "\r");
						fw1.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				D d = new D(sc,i,bytesArray);
				clients.add(d);
				executor.execute(d);
			} catch (IOException e) {
				System.out.println(MAESTRO + "Error creando el socket cliente.");
				e.printStackTrace();
			}
		}
		
	}
	public static void dormir() {
		try {
			synchronized(o) {
				synchronized(p) {
					
					p.notify();
				}
				cont++;
				o.wait();
			}
		
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static synchronized void log(long t1,long t2) {
		try {
			log="Tiempo de envío: "+(t2-t1+"\r\n");
			log+="Archivo recibido exitosamente \r\n";
			FileWriter fw1 = new FileWriter(new File(ruta),true);
			fw1.write(log + "\r");
			fw1.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
