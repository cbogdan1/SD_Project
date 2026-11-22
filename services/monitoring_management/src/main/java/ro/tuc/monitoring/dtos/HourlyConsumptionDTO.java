package ro.tuc.monitoring.dtos;

/** The energy used by a device in one hour of a day. The REST endpoint returns 24 of these (hours 0 to 23). */
public class HourlyConsumptionDTO {

    private int hour;
    private double energyKwh;

    public HourlyConsumptionDTO() {
    }

    public HourlyConsumptionDTO(int hour, double energyKwh) {
        this.hour = hour;
        this.energyKwh = energyKwh;
    }

    public int getHour() {
        return hour;
    }

    public double getEnergyKwh() {
        return energyKwh;
    }
}
