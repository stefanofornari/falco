
package ste.falco;

import java.io.BufferedInputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;

/**
 *
 * 
 */
public class PlaySound {
    
    public static void main(String... args) throws Exception {
        
        
        for(Mixer.Info info: AudioSystem.getMixerInfo()) {
            System.out.println(info);
        }
        
        //  AudioSystem.getMixerInfo()[2]
        Mixer mixer = AudioSystem.getMixer(AudioSystem.getMixerInfo()[2]);
        Clip clip = (Clip)mixer.getLine(new DataLine.Info(Clip.class, null));
        
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(
            new BufferedInputStream(
                PlaySound.class.getResourceAsStream("/red-tailed-hawk-sound.wav")
            )
        );
        //clip = AudioSystem.getClip();
        clip.open(audioIn);
        clip.start();
        
        Thread.sleep(5000);
    }
    
}
