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
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 */
public class SoundMotionDetector extends MotionDetector {
    
    public final String sound;
    
    private Mixer mixer;
    private Clip clip;
    
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
        clip = getClip();
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
    
    public void moved() {
        super.moved(); // it checks everything is ready
        clip.setFramePosition(0);
        clip.start();
    };
    
    // --------------------------------------------------------- private methods
    
    private Clip getClip() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                             AudioSystem.NOT_SPECIFIED,
                                             16, 2, 4,
                                             AudioSystem.NOT_SPECIFIED, true);
        DataLine.Info info = new DataLine.Info(Clip.class, format);
        return (Clip)mixer.getLine(info);
    }
}
