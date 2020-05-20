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
import java.time.Clock;
import java.time.LocalDateTime;
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
import ste.falco.SoundMotionDetector;
import ste.falco.SoundUtils;


/**
 *
 */
public class FalcoCLI extends SoundMotionDetector {

    private static final int DEFAULT_HEARTBEAT_PERIOD = 5*60*1000; // 5 minutes in milliseconds
    private static Logger LOG = Logger.getLogger("ste.falco");

    private final Clock CLOCK = Clock.systemDefaultZone();

    private Heartbeat heartbeatTask;
    private LocalDateTime lastMoved = LocalDateTime.now(CLOCK).minusHours(24); // just to make sure the first ervent is capture


    public static void main(String... args) {
        System.out.println("Welcome to Falco");

        if (args.length > 0) {
            System.out.println("usage:");
            System.out.println("ste.falco.FalcoCLI [--help]");
            return;
        }

        System.out.println("-- started");
        while (true) {
            try (FalcoCLI moctor = new FalcoCLI()) {
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.info("falco started");
                }

                while (moctor.isLive()) {
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

    public FalcoCLI() throws Exception {
        super("/sounds/red-tailed-hawk-sound.wav");
        this.heartbeatTask = new Heartbeat(DEFAULT_HEARTBEAT_PERIOD);
        startup();
    }

    /**
     *
     * @param heartbeatTask the task to be executed at heartbeat
     *
     * @throws Exception same as startup()
     */
    protected FalcoCLI(Heartbeat heartbeatTask) throws Exception  {
        super("/sounds/red-tailed-hawk-sound.wav");
        this.heartbeatTask = heartbeatTask;
        startup();
    }

    @Override
    public void moved() {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("motion detected");
        }
        if (shallPlay()) {
            lastMoved = LocalDateTime.now(CLOCK);
            play();
        } else {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("too early or not in day light - I am muted");
            }
        }
    }

    @Override
    public void startup() throws Exception {
        super.startup();

        jmxSetup();

        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

        ses.scheduleAtFixedRate(heartbeatTask, 0, heartbeatTask.period, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        try { jmxShutdown(); } catch (Exception x) {}
    }

    // ---------------------------------------------------------- friend methods

    /**
     * This is trick (maybe dirty) to be able to call super.moved() from the JMX
     * bean.
     */
    void play() {
        super.moved();
    }

    // --------------------------------------------------------- private methods

    private boolean shallPlay() {
        LocalDateTime now = LocalDateTime.now(CLOCK);
        int hour = now.getHour();

        if ((hour < 20) && (hour > 7)) {
            return now.minusMinutes(10).isAfter(lastMoved);
        }

        return false;
    }

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
        throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException  {
        ManagementFactory
            .getPlatformMBeanServer()
            .unregisterMBean(
                new ObjectName("ste.falco.jmx:name=TrafficControl")
            );
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

    // ---------------------------------------------------------- TrafficControl

    public static interface TrafficControlMBean {
        public void move();
        public void play();
        public void reinit();
    };

    public static class TrafficControl implements TrafficControlMBean {

        private final FalcoCLI falco;

        public TrafficControl(FalcoCLI falco) {
            this.falco = falco;
        }

        @Override
        public void move() {
            falco.moved();
        }

        @Override
        public void play() {
            falco.play();
        }

        @Override
        public void reinit() {
            falco.shutdown();
        }

    };

}
