package entity;

public class Costi {
    private double costoEuroOra;
    private double costoEuroKw;

    public Costi(double costoEuroOra, double costoEuroKw) {
        this.costoEuroOra = costoEuroOra;
        this.costoEuroKw = costoEuroKw;
    }

    public Costi() {
        
    }

    public double getCostoEuroOra() {
        return costoEuroOra;
    }

    public void setCostoEuroOra(double costoEuroOra) {
        this.costoEuroOra = costoEuroOra;
    }

    public double getCostoEuroKw() {
        return costoEuroKw;
    }

    public void setCostoEuroKw(double costoEuroKw) {
        this.costoEuroKw = costoEuroKw;
    }
}
