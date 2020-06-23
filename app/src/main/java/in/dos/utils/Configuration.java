package in.dos.utils;

public class Configuration {

    public final static String SEED_VALUE = "1111";
    public final static Integer SAMPLING_RATE = 44100;
    // TODO check with codec for android devices
    public final static Double BIT_DURATION = 0.0001;
    public final static Integer SPREADING_FACTOR = 4;
    public final static Double CODE_BIT_DURATION = BIT_DURATION / SPREADING_FACTOR;
    public final static Integer CARRIER_FREQUENCY = 18000;

}
