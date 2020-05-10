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
                String.format("'%s' does not exist in classpath", sound)
            );
        }
        this.sound = sound;

        mixer = AudioSystem.getMixer(null);
    }

    @Override
    public void startup() throws Exception {
        super.startup();
        clip = SoundUtils.getClip(mixer);
        clip.addLineListener(new LoggingClipListener());
        clip.open(
            AudioSystem.getAudioInputStream(
                new ByteArrayInputStream(IOUtils.resourceToByteArray(sound))
            ));
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
    };

    // ------------------------------------------------------ LoggingClipListern

    private class LoggingClipListener implements LineListener {

        @Override
        public void update(LineEvent e) {
            if (LOG.isLoggable(Level.FINEST)) {
                if (e.getType() == LineEvent.Type.START) {
                    LOG.finest("playing " + sound);
                } else {
                    LOG.finest(String.valueOf(e));
                }
            }
        }

    }
}
