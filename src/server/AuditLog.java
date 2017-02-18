package server;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import common.Individual;

public class AuditLog {
    private static Logger logger;
    
    public static void setUp() {
        logger = Logger.getLogger("audit");
        // - maybe let the caller deal with exceptions? Think about it
        try {
            FileHandler fh = new FileHandler("../audit.log", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        logger.info("Initializing audit log");
    }
    
    // - maybe make this use Individual and Command instances?
    public synchronized static void log(Individual i, String msg) {
        logger.info(i.getName() + ": " + msg);
    }
}
