
package ste.falco;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 * This example code demonstrates how to setup a listener
 * for GPIO pin state changes on the Raspberry Pi.
 *
 * @author Robert Savage
 */
public class PIRListening {

    public static void main(String args[]) throws InterruptedException {
        System.out.println("<--Pi4J--> GPIO Listening Example ... started.");
        
        //GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));

        // create gpio controller
        final GpioController gpio = GpioFactory.getInstance();
        
        // provision gpio pin #23 as an input pin with its internal pull down resistor enabled
        final GpioPinDigitalInput pin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, "Motion detection", PinPullResistance.PULL_DOWN);

        // set shutdown state for this input pin
        pin.setShutdownOptions(true);

        // create and register gpio pin listener
        pin.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());
            }

        });

        System.out.println(" ... complete the GPIO #04 circuit and see the listener feedback here in the console.");

        // keep program running until user aborts (CTRL-C)
        while(true) {
            Thread.sleep(500);
        }

        // stop all GPIO activity/threads by shutting down the GPIO controller
        // (this method will forcefully shutdown all GPIO monitoring threads and scheduled tasks)
        // gpio.shutdown();   <--- implement this method call if you wish to terminate the Pi4J GPIO controller
    }
}
