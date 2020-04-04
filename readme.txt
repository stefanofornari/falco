Configure OpenJDK Sound
-----------------------

sudo vi /etc/java-8-openjdk/sound.properties

Comment out the icedtea classpath configs:

#javax.sound.sampled.Clip=org.classpath.icedtea.pulseaudio.PulseAudioMixerProvider
#javax.sound.sampled.Port=org.classpath.icedtea.pulseaudio.PulseAudioMixerProvider
#javax.sound.sampled.SourceDataLine=org.classpath.icedtea.pulseaudio.PulseAudioMixerProvider
#javax.sound.sampled.TargetDataLine=org.classpath.icedtea.pulseaudio.PulseAudioMixerProvider

Remove the comments from the sun classpath configs:

javax.sound.sampled.Clip=com.sun.media.sound.DirectAudioDeviceProvider
javax.sound.sampled.Port=com.sun.media.sound.PortMixerProvider
javax.sound.sampled.SourceDataLine=com.sun.media.sound.DirectAudioDeviceProvider
javax.sound.sampled.TargetDataLine=com.sun.media.sound.DirectAudioDeviceProvider
