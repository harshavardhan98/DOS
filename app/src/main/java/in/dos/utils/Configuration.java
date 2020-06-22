package in.dos.utils;

public class Configuration {

    final static String SEED_VALUE = "1111";
    final static Integer SAMPLING_FREQUENCY = 44100;
    // TODO check with codec for android devices
    final static Double BIT_DURATION = 0.0001;
    final static Integer SPREADING_FACTOR = 4;
    final static Double CODE_BIT_DURATION = BIT_DURATION / SPREADING_FACTOR;

}
