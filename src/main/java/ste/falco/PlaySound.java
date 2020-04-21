package ste.falco;

import java.io.BufferedInputStream;
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
        Mixer mixer = null;
        String device = (args.length > 1)
                      ? '[' + args[1] + ']'
                      : null;

        for(Mixer.Info info: AudioSystem.getMixerInfo()) {
            System.out.print(info);
            if (device != null) {
                if (info.getName().indexOf(device) >= 0) {
                    mixer = AudioSystem.getMixer(info);
                    System.out.print(" (*)");
                }
            }
            System.out.println();
        }

        Clip clip = (mixer != null)
                  ? (Clip)mixer.getLine(new DataLine.Info(Clip.class, null))
                  : AudioSystem.getClip();  // defatul

        clip.open(
            AudioSystem.getAudioInputStream(
                new BufferedInputStream(
                    PlaySound.class.getResourceAsStream("/sounds/" + args[0])
                )
            )
        );
        clip.start();

        while(clip.getFramePosition() < clip.getFrameLength()) {
            Thread.sleep(250);
        }
    }

}
