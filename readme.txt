Configure Bluetooth
-------------------

Make sure bluetooth, pulseaudio are installed
(see http://youness.net/raspberry-pi/how-to-connect-bluetooth-headset-or-speaker-to-raspberry-pi-3 -
src/docs/How To Connect Bluetooth Headset Or Speaker To Raspberry Pi 3.pdf
in case not available any more online)


Connect RIP
-----------
See (https://tutorials-raspberrypi.com/connect-and-control-raspberry-pi-motion-detector-pir/ - 
src/docs/Connect and control Raspberry Pi motion detector PIR.pdf inc ase not 
available any more online)

Functionality
-------------
° Play a sound when motion is detected
° No more then 1 play every 10 minutes
° No play between 21:00 and 7:00

TODO
----
- sig handling to mute/unmute; eg: kill -s USR1 <pid>