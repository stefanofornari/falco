/*
 * Copyright (C) 2020 Stefano Fornari.
 * All Rights Reserved.  No use, copying or distribution of this
 * work may be made except in accordance with a valid license
 * agreement from Stefano Fornari.  This notice must be
 * included on all copies, modifications and derivatives of this
 * work.
 *
 * STEFANO FORNARI MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. STEFANO FORNARI SHALL NOT BE LIABLE FOR ANY
 * DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */
package ste.falco.ui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.commons.io.IOUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import ste.falco.MotionDetector;
import ste.falco.SoundMotionDetector;
import ste.falco.SoundUtils;

/**
 *
 */
public class FalcoCLI implements AutoCloseable {

    private static final int DEFAULT_HEARTBEAT_PERIOD = 5 * 60 * 1000; // 5 minutes in milliseconds
    private static Logger LOG = Logger.getLogger("ste.falco");

    private Heartbeat heartbeatTask;

    public        final SoundMotionDetector moctor;
    public static final FalcoOptions DEFAULTS = new FalcoOptions();

    public static void main(String... args) {
        System.out.println("Welcome to Falco");

        FalcoCLI.FalcoOptions options = new FalcoCLI.FalcoOptions();
        CommandLine cli = new CommandLine(options);
        try {
            cli.execute(args);
        } catch (ParameterException x) {
            //
            // picocli shows already the error message and the usage.
            //
            System.out.println(x.getMessage());
            cli.usage(System.out);
            return;
        }

        if (cli.isUsageHelpRequested()) {
            return;
        }

        System.out.println("-- started");
        while (true) {
            try (FalcoCLI falco = new FalcoCLI(options)) {
                falco.startup();

                if (LOG.isLoggable(Level.INFO)) {
                    LOG.info("falco started");
                }

                while (falco.moctor.isLive()) {
                    Thread.sleep(250);
                }

                if (LOG.isLoggable(Level.INFO)) {
                    LOG.info("falco recycled");
                }
            } catch (Exception x) {
                LOG.throwing(FalcoCLI.class.getName(), "main", x);
            }
        }
    }

    /**
     * Creates a new instance with default options (as per FalcoCLI.DEFAULS)
     *
     * @throws Exception in case of errors
     */
    public FalcoCLI() {
        this(DEFAULTS);
    }

    /**
     *
     * @param heartbeatTask the task to be executed at heartbeat
     *
     * @throws Exception same as startup()
     */
    public FalcoCLI(FalcoCLI.FalcoOptions options) {
        moctor = new MotionDetector("/sounds/red-tailed-hawk-sound.wav");
        heartbeatTask = null;

        if (!options.noHeartbeat) {
            try {
                heartbeatTask = new Heartbeat(DEFAULT_HEARTBEAT_PERIOD);
            } catch (Exception x) {
                //
                // The heartbeat won't start...
            }
        } else {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("Heartbeat disabled");
            }
        }
    }

    public void startup() throws Exception {
        jmxSetup();

        moctor.startup();

        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

        if (heartbeatTask != null) {
            ses.scheduleAtFixedRate(heartbeatTask, 0, heartbeatTask.period, TimeUnit.MILLISECONDS);
        }
    }

    public void shutdown() {
        try {
            //
            // TODO: to fix based on the type...
            //
            ((MotionDetector)moctor).shutdown();
            jmxShutdown();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    // ---------------------------------------------------------- friend methods
    /**
     * This is trick (maybe dirty) to be able to call super.moved() from the JMX
     * bean.
     */
    void play() {
        moctor.moved();
    }

    // --------------------------------------------------------- private methods
    private void jmxSetup()
            throws MalformedObjectNameException, InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        ManagementFactory
                .getPlatformMBeanServer()
                .registerMBean(
                        new TrafficControl(this),
                        new ObjectName("ste.falco.jmx:name=TrafficControl")
                );
    }

    private void jmxShutdown()
            throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException {
        ManagementFactory
                .getPlatformMBeanServer()
                .unregisterMBean(
                        new ObjectName("ste.falco.jmx:name=TrafficControl")
                );
    }

    @Override
    public void close() throws Exception {
        shutdown();
    }

    // ----------------------------------------------------------- HeartbeatTask
    protected static class Heartbeat implements Runnable {

        public final long period;

        private Clip clip;

        /**
         * @param period the delay in milliseconds between to beats
         */
        public Heartbeat(long period)
                throws LineUnavailableException, UnsupportedAudioFileException, IOException {
            this.period = period;
            clip = SoundUtils.getClip(AudioSystem.getMixer(null));
            clip.open(
                    AudioSystem.getAudioInputStream(
                            new ByteArrayInputStream(IOUtils.resourceToByteArray("/sounds/heartbeat.wav"))
                    ));
        }

        @Override
        public void run() {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("heartbeat");
            }

            clip.setFramePosition(0);
            clip.start();
        }
    }

    // ------------------------------------------------------------ FalcoOptions
    @Command(
            name = "ste.falco.FalcoCLI",
            description = "A pigeon dissuader that plays the sound of a red tailed hawk"
    )
    protected static class FalcoOptions {

        @Option(
                names = {"--help", "-h"},
                description = "This help message",
                usageHelp = true
        )
        public boolean helpRequested;

        @Option(
                names = {"--noheartbeat"},
                description = "Do not play the heartbeat"
        )
        public boolean noHeartbeat = false;

        @Option(
                names = {"--nogpio"},
                description = "Do not use GPIO"
        )
        public boolean noGPIO = false;
    }

    // ---------------------------------------------------------- TrafficControl

    public static interface TrafficControlMBean {

        public void move();

        public void play();

        public void reinit();

        public void setVolume(double volume);

        public void getVolume();
    };

    public static class TrafficControl implements TrafficControlMBean {

        private final FalcoCLI falco;

        public TrafficControl(FalcoCLI falco) {
            this.falco = falco;
        }

        @Override
        public void move() {
            falco.moctor.moved();
        }

        @Override
        public void play() {
            falco.play();
        }

        @Override
        public void reinit() {
            falco.shutdown();
        }

        @Override
        public void setVolume(double volume) {
            falco.moctor.setVolume(volume);
        }

        @Override
        public void getVolume() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    };

}
