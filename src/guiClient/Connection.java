package guiClient;

import java.io.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.security.KeyStore;

/*
 * Client program based on given code for project 1.
 *
 * This program assumes that the client is not inside a firewall.
 * The application can be modified to connect to a server outside
 * the firewall by following SSLSocketClientWithTunneling.java.
 */
public class Connection implements Runnable {
	private String[] args;
	private String cmd = null, response;
	
	private String truststore, keystore;
	private char[] tpass, kpass;
	private String host;
	private int port;

	public synchronized String command(String cmd) {
	    this.cmd = cmd;
	    response = null;
	    notifyAll();
        try {
            while (null == response) {
                wait();
            } 
        } catch (InterruptedException e) {
            e.printStackTrace();
	    }
	    return response;
	}
	
	public Connection(String host, int port, String truststore, String keystore, char[] tpass, char[] kpass) {
	    this.truststore = truststore;
	    this.keystore = keystore;
	    this.tpass = tpass;
	    this.kpass = kpass;
	    this.host = host;
	    this.port = port;
	    new Thread(this).start();
	}
	
    public synchronized void run() {
        try { /* set up a key manager for client authentication */
            SSLSocketFactory factory = null;
            try {
                KeyStore ks = KeyStore.getInstance("JKS");
                KeyStore ts = KeyStore.getInstance("JKS");
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                SSLContext ctx = SSLContext.getInstance("TLS");
                ks.load(new FileInputStream(keystore), kpass);
				ts.load(new FileInputStream(truststore), tpass);
				kmf.init(ks, kpass);
				tmf.init(ts); // keystore can be used as truststore here
				ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                factory = ctx.getSocketFactory();
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }
            SSLSocket socket = (SSLSocket)factory.createSocket(host, port);

            socket.startHandshake();

            SSLSession session = socket.getSession();
            X509Certificate cert = (X509Certificate)session.getPeerCertificateChain()[0];

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;
			for (;;) {
                while (null == cmd) wait();
                msg = cmd;
                cmd = null;
                
                out.println(msg);
                out.flush();
                
                response = in.readLine();
                notifyAll();
            }
            /*in.close();
			out.close();
            socket.close();*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
