package ro.tuc.simulator;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Makes realistic-looking energy values for 10-minute intervals:
 * value = base load (random, chosen once per run) x factor for the hour of the day x small fluctuation (+-10 %).
 */
public class EnergyGenerator {

    private final Random random = new Random();

    // average energy of this device in 10 minutes: 0.05 - 0.15 kWh (= 0.3 - 0.9 kW average power)
    private final double baseLoad = 0.05 + random.nextDouble() * 0.10;

    /** Energy (kWh) used in the 10 minutes that start at the given time. */
    public double nextValue(LocalDateTime time) {
        double fluctuation = 0.9 + random.nextDouble() * 0.2;  // between 0.9 and 1.1
        double value = baseLoad * hourFactor(time.getHour()) * fluctuation;
        return Math.round(value * 10000) / 10000.0;            // keep 4 decimals
    }

    // how much more (or less) than the base load the device uses at each hour of the day
    private static double hourFactor(int hour) {
        if (hour < 6) {
            return 0.4;     // night: almost everything is off
        } else if (hour < 9) {
            return 1.0;     // morning
        } else if (hour < 17) {
            return 0.7;     // day: nobody at home
        } else if (hour < 22) {
            return 1.6;     // evening peak: cooking, lights, TV
        }
        return 0.8;         // late evening
    }
}
