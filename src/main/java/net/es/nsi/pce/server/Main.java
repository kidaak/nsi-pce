package net.es.nsi.pce.server;

import net.es.nsi.pce.config.ConfigurationManager;
import org.slf4j.LoggerFactory;

/**
 * This is the main execution thread for the PAth Computation Engine.  The
 * runtime configuration is loaded from disk, the HTTP server is started, and
 * a shutdown hook is added to monitor for termination conditions and clean up
 * services.
 * 
 * @author hacksaw
 */
public class Main {
    static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);

    // Keep running PCE while true.
    private static boolean keepRunning = true;

    /**
     * Returns a boolean indicating whether the PCE should continue running
     * (true) or should terminate (false).
     * 
     * @return true if the PCE should be running, false otherwise.
     */
    public static boolean isKeepRunning() {
        return keepRunning;
    }

    /**
     * Set whether the PCE should be running (true) or terminated (false).
     * 
     * @param keepRunning true if the PCE should be running, false otherwise.
     */
    public static void setKeepRunning(boolean keepRunning) {
        Main.keepRunning = keepRunning;
    }

    /**
     * Main initialization and execution thread for the NSI Path Computation
     * Engine.  Method will loop until a signal is received to shutdown.
     * 
     * @param args No arguments are accepted.
     * @throws Exception If anything fails during initialization.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void main(String[] args) throws Exception {
        // Load PCE configuration from disk.
        ConfigurationManager.INSTANCE.initialize("config/");

        // Start the main HTP container.
        log.info("Path Computation Engine starting...");
        PCEServer.INSTANCE.start(ConfigurationManager.INSTANCE.getPceConfig());
        log.info("PCE initialized and running.");

        // Listen for a shutdown event so we can clean up.
        Runtime.getRuntime().addShutdownHook(
            new Thread() {
                public void run() {
                    log.info("Shutting down PCE...");
                    PCEServer.INSTANCE.stop();
                    log.info("...Shutdown complete.");
                    Main.setKeepRunning(false);
                }
            }
        );

        // Loop until we are told to shutdown.
        while (keepRunning) {
            Thread.sleep(1);
        }
    }
}