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

import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
public class SoundMotionDetector extends MotionDetector {

    public final String sound;

    private Mixer mixer;
    private Clip clip;

    protected final Logger LOG = Logger.getLogger("ste.falco");

    public SoundMotionDetector(String sound) {
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

    @Override
    public void startup() throws Exception {
        super.startup();
        clip = SoundUtils.getClip(mixer);
        clip.addLineListener(new MotionClipListener());
        clip.open(
                AudioSystem.getAudioInputStream(
                        new ByteArrayInputStream(IOUtils.resourceToByteArray(sound))
                )
        );
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        if (event.getState().isHigh()) {
            moved();
        }
    }

    @Override
    public void moved() {
        super.moved(); // it checks everything is ready
        clip.setFramePosition(0);
        clip.start();
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

    // ------------------------------------------------------ LoggingClipListern
    private class MotionClipListener implements LineListener {

    @Override
    public void update(LineEvent e) {
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
