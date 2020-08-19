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
package ste.falco;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.Mixer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 */
public class SoundMotionDetector implements AutoCloseable {

    public final String sound;

    protected Mixer mixer;
    protected Clip clip;

    protected final Logger LOG = Logger.getLogger("ste.falco");

    private final Clock CLOCK = Clock.systemDefaultZone();
    private LocalDateTime lastMoved = LocalDateTime.now(CLOCK).minusHours(24); // just to make sure the first ervent is capture

    public SoundMotionDetector(final String sound) {
        if (StringUtils.isBlank(sound)) {
            throw new IllegalArgumentException("sound can not be blank or null");
        }

        try {
            IOUtils.resourceToURL(sound);
        } catch (IOException x) {
            throw new IllegalArgumentException(
                    String.format("'%s' not found in classpath", sound)
            );
        }
        this.sound = sound;

        mixer = AudioSystem.getMixer(null);
    }

    public void startup() throws Exception {
        clip = SoundUtils.getClip(mixer);
        clip.addLineListener(new MotionClipListener());
        clip.open(
                AudioSystem.getAudioInputStream(
                        new ByteArrayInputStream(IOUtils.resourceToByteArray(sound))
                )
        );
    }

    //
    // TODO: writye bugfreecode
    //
    public boolean isLive() {
        return (clip != null);
    }

    public void moved() {
        System.out.println("CHECK2.1");
        if (clip == null) {
            throw new IllegalStateException("moved() called before the instance is started up; make sure to call startup()");
        }

        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("motion detected");
        }
        if (shallPlay()) {
            lastMoved = LocalDateTime.now(CLOCK);
            clip.setFramePosition(0);
            System.out.println("CHECK2.2");
            clip.start();
            System.out.println("CHECK2.3");
        } else {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("too early or not in day light - I am muted");
            }
        }
    }

    public void setVolume(double volume) {
        if (volume < 0d || volume > 2d) {
            throw new IllegalArgumentException("invalid volume " + volume + " - it must in range (0.0, 2.0)");
        }
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        gain.setValue((float)(20d * Math.log10(volume)));
    }

    public double getVolume() {
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

        //
        // rounding to 2 decimals
        //
        return Math.round(Math.pow(10d, gain.getValue() / 20d)*100)/100d;
    }

    // --------------------------------------------------------- Private methods

    private boolean shallPlay() {
        LocalDateTime now = LocalDateTime.now(CLOCK);
        int hour = now.getHour();

        if ((hour < 20) && (hour > 7)) {
            return now.minusMinutes(10).isAfter(lastMoved);
        }

        return false;
    }

    private void printControl(Control control, String indent) {
        System.out.printf("%s%s%n", indent, control);
        if (control instanceof CompoundControl) {
            Control[] controls = ((CompoundControl) control).getMemberControls();
            indent += "  ";
            for (Control c : controls) {
                printControl(c, indent);
            }
        }
    }


    // ----------------------------------------------------------- AutoCloseable

    @Override
    public void close() {
        // TODOD: maybe close the clip?
    }

    // ------------------------------------------------------ LoggingClipListern
    private class MotionClipListener implements LineListener {

        @Override
        public void update(LineEvent e) {
            System.out.println("CHECK3 " + e);
            if (LOG.isLoggable(Level.FINEST)) {
                if (e.getType() == LineEvent.Type.START) {
                    LOG.finest("playing " + sound);
                } else {
                    LOG.finest(String.valueOf(e));
                }
            }

            if (e.getType() == LineEvent.Type.STOP) {
                clip.close();
                try {
                    clip.open(
                            AudioSystem.getAudioInputStream(
                                    new ByteArrayInputStream(IOUtils.resourceToByteArray(sound))
                            )
                    );
                } catch (Exception x) {
                    if (LOG.isLoggable(Level.SEVERE)) {
                        LOG.throwing(MotionClipListener.class.getName(), "playing sound", x);
                    }
                }
            }
        }

    }
}
