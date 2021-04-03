java sound configuration
# falco
Raspberry PI based pigeons dissuader

Connect RIP
-----------
See (https://tutorials-raspberrypi.com/connect-and-control-raspberry-pi-motion-detector-pir/ -
src/docs/Connect and control Raspberry Pi motion detector PIR.pdf in case not
available any more online)

Features
--------
* Play a sound when motion is detected
* No more then 1 play every 10 minutes
* No play between 21:00 and 7:00

Utilities
---------

PlaySound: testing app to play a sound in the resources, optionally specifying
the sound device to use.

> java -cp lib/falco-${project.version}.jar ste.falco.PlaySound resource [device]

For example, given the output of aplay -L below

```
null
    Discard all samples (playback) or generate zero samples (capture)
default:CARD=ALSA
    bcm2835 ALSA, bcm2835 ALSA
    Default Audio Device
sysdefault:CARD=ALSA
    bcm2835 ALSA, bcm2835 ALSA
    Default Audio Device
dmix:CARD=ALSA,DEV=0
    bcm2835 ALSA, bcm2835 ALSA
    Direct sample mixing device
hw:CARD=ALSA,DEV=0
    bcm2835 ALSA, bcm2835 ALSA
    Direct hardware device without any conversions
hw:CARD=ALSA,DEV=1
    bcm2835 ALSA, bcm2835 IEC958/HDMI
    Direct hardware device without any conversions
hw:CARD=ALSA,DEV=2
    bcm2835 ALSA, bcm2835 IEC958/HDMI1
    Direct hardware device without any conversions
plughw:CARD=ALSA,DEV=0
    bcm2835 ALSA, bcm2835 ALSA
    Hardware device with all software conversions
plughw:CARD=ALSA,DEV=1
    bcm2835 ALSA, bcm2835 IEC958/HDMI
    Hardware device with all software conversions
plughw:CARD=ALSA,DEV=2
    bcm2835 ALSA, bcm2835 IEC958/HDMI1
    Hardware device with all software conversions
```

Run:

> java -cp lib/falco-${project.version}.jar ste.falco.PlaySound red-tailed-hawk-sound.wav plughw:0,2

Note that resource is relative to /sounds.

JMX configuration
To enable JMX for remote management and troubleshooting, uncomment the relevant
properties in bin/falco.

Note that to make it working java.rmi.server.hostname must be set to the
hostname the device is reached by the rest of the network. For instance, in a
home network, the name is usually something like mypi.local where mypi is the
name provided during Pi set-up.

To change the hostname, use the command

sudo raspi-config

Choose Network options/Hostname and set the wanted name.


TODO
----
