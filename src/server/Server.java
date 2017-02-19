package server;

import java.io.*;
import java.net.*;
import java.security.KeyStore;

import javax.naming.InvalidNameException;
import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;

import common.Individual;
import common.Journals;
import server.commands.Command;

public class Server implements Runnable {
    private static final String path = "certificates/";

    private ServerSocket serverSocket = null;
    private static int numConnectedClients = 0;
    private static Journals journals;

    public Server(ServerSocket ss) throws IOException {
        serverSocket = ss;
        newListener();
    }

    public void run() {
        try {
            SSLSocket socket = (SSLSocket) serverSocket.accept();
            newListener();
            SSLSession session = socket.getSession();
            X509Certificate cert = (X509Certificate) session.getPeerCertificateChain()[0];
            String subject = cert.getSubjectDN().getName();
            String issuer = cert.getIssuerDN().getName();
            numConnectedClients++;
            System.out.println("client connected");
            System.out.println("client name (cert subject DN field): " + subject);
            System.out.println("issuer: " + issuer);
            System.out.println("serial number: " + cert.getSerialNumber());
            System.out.println(numConnectedClients + " concurrent connection(s)\n");
            
            Individual individual = null;
            try {
                individual = new Individual(subject);
            } catch (InvalidNameException e) {
                e.printStackTrace();
                socket.close();
                return; // user DN is malformed; we cannot continue
            }
            AuditLog.log(individual, "connected");

            PrintWriter out = null;
            BufferedReader in = null;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            CommandParser commandParser = new CommandParser();

            String clientMsg = null;
            while ((clientMsg = in.readLine()) != null) {
                System.out.println("received '" + clientMsg + "' from client");
                Command cmd = commandParser.parse(clientMsg);
                // - Maybe rethink this synchronization solution.
                // (it might be overly cautious, and thus possibly needlessly slow)
                // Multiline output should be handled correctly, as all string
                // fields are base64-encoded (and then space-separated).
                synchronized (journals) {
                    if (!cmd.hasPermission(individual, journals)) {
                        out.println("Permission denied");
                    } else {
                        out.println(cmd.execute(individual, journals));
                    }
                }
                out.flush();
                System.out.println("done\n");
            }
            in.close();
            out.close();
            socket.close();
            numConnectedClients--;
            AuditLog.log(individual, "disconnected");
            System.out.println("client disconnected");
            System.out.println(numConnectedClients + " concurrent connection(s)\n");
        } catch (IOException e) {
            System.out.println("Client died: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    private void newListener() {
        (new Thread(this)).start();
    } // calls run()

    public static void main(String args[]) {
        System.out.println("\nServer Started\n");
        AuditLog.setUp();
        
        // Load data and set up saving of data on shutdown.
        String dataFile = "data";
        try {
            journals = Journals.load(dataFile);
        } catch (IOException e1) {
            journals = new Journals();
            System.out.println("Could not load saved data");
            e1.printStackTrace();
        }
        class ShutdownHook extends Thread {
            public void run() {
                try {
                    journals.save(dataFile);
                } catch (FileNotFoundException e) {
                    System.out.println("Could not save data");
                    e.printStackTrace();
                }
            }
        }
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        
        // Set up network.
        final int DEFAULT_PORT = 9876;
        int port = DEFAULT_PORT;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        String type = "TLS";
        try {
            ServerSocketFactory ssf = getServerSocketFactory(type);
            ServerSocket ss = ssf.createServerSocket(port);
            ((SSLServerSocket) ss).setNeedClientAuth(true); // enables client
                                                            // authentication
            new Server(ss);
        } catch (IOException e) {
            System.out.println("Unable to start Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ServerSocketFactory getServerSocketFactory(String type) {
        if (type.equals("TLS")) {
            SSLServerSocketFactory ssf = null;
            try { // set up key manager to perform server authentication
                SSLContext ctx = SSLContext.getInstance("TLS");
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                KeyStore ks = KeyStore.getInstance("JKS");
                KeyStore ts = KeyStore.getInstance("JKS");
                char[] password = "password".toCharArray();

                ks.load(new FileInputStream(path + "serverkeystore"), password); // keystore
                                                                                 // password
                                                                                 // (storepass)
                ts.load(new FileInputStream(path + "servertruststore"), password); // truststore
                                                                                   // password
                                                                                   // (storepass)
                kmf.init(ks, password); // certificate password (keypass)
                tmf.init(ts); // possible to use keystore as truststore here
                ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                ssf = ctx.getServerSocketFactory();
                return ssf;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return ServerSocketFactory.getDefault();
        }
        return null;
    }
}
