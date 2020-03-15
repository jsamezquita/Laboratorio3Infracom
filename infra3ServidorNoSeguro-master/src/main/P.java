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
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class P {
	private static ServerSocket ss;	
	private static final String MAESTRO = "MAESTRO: ";
	private static X509Certificate certSer; /* acceso default */
	private static KeyPair keyPairServidor; /* acceso default */
	private static final int numeroThreads = 25;
	
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
		String ruta = "./resultados.txt";
   
        file = new File(ruta);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter fw = new FileWriter(file);
        fw.close();
        
        D.init(certSer, keyPairServidor, file);
        ExecutorService executor = Executors.newFixedThreadPool(numeroThreads);
        
		// Crea el socket que escucha en el puerto seleccionado.
		ss = new ServerSocket(ip);
		System.out.println(MAESTRO + "Socket creado.");
		ThreadCargaCPU tcpu = new ThreadCargaCPU();
		tcpu.start();
		 
		 //init array with file length
		 byte[] bytesArray;
		 FileInputStream fis = new FileInputStream(file);
		 BufferedInputStream bis = new BufferedInputStream(fis);
         
         
		 System.out.println("Elija un archivo para enviar (1 o 2):");
		 System.out.println("1.Archivo de texto 250MB");
		 System.out.println("2.Archivo de texto 100MB");
		 String send="";
		 if(br.readLine().equals("1")) {
			  
			 send="./data/Archivo1.txt";
		 }else {
			 send="./data/Archivo2.txt";
		 }
		 File sendFile = new File(send);
		 bytesArray = new byte[(int) sendFile.length()]; 
		 bis.read(bytesArray,0,bytesArray.length);
		 System.out.println("¿A cuantos clientes desea enviar el archivo?");
		 String clientes=br.readLine();
		 ArrayList<D> clients=new ArrayList<D>();
		for (int i=0;true;i++) {
			if(i>=Integer.parseInt(clientes)) {
				for(int j=0;j<clients.size();j++) {
					clients.get(i).notifyAll();				
					}
			}
			try { 
				Socket sc = ss.accept();
				System.out.println(MAESTRO + "Cliente " + i + " aceptado.");
				D d = new D(sc,i, tcpu,bytesArray);
				clients.add(d);
				executor.execute(d);
			} catch (IOException e) {
				System.out.println(MAESTRO + "Error creando el socket cliente.");
				e.printStackTrace();
			}
		}
		
	}
}
