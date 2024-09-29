package entity;

import java.time.LocalDateTime;

public class Veicolo {
    private int idVeicolo;
    private String targa;
    private String modello;
    private String idUtente;
    private float capacitaBatteria;
    private boolean dentroParcheggio;
    private LocalDateTime data_ingresso;

    public Veicolo(String targa, String modello, String idUtente, float capacitaBatteria, boolean dentroParcheggio, LocalDateTime data_ingresso) {
        this.targa = targa;
        this.modello = modello;
        this.idUtente = idUtente;
        this.capacitaBatteria = capacitaBatteria;
        this.dentroParcheggio = dentroParcheggio;
        this.data_ingresso = data_ingresso;

    }

    public Veicolo() {
    }

    public int getIdVeicolo() {
        return idVeicolo;
    }

    public void setIdVeicolo(int idVeicolo) {
        this.idVeicolo = idVeicolo;
    }

    public String getTarga() {
        return targa;
    }

    public void setTarga(String targa) {
        this.targa = targa;
    }

    public String getModello() {
        return modello;
    }

    public void setModello(String modello) {
        this.modello = modello;
    }

    public String getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(String idUtente) {
        this.idUtente = idUtente;
    }

    public float getCapacitaBatteria() {
        return capacitaBatteria;
    }

    public void setCapacitaBatteria(float capacitaBatteria) {
        this.capacitaBatteria = capacitaBatteria;
    }

    public boolean isDentroParcheggio() {
        return dentroParcheggio;
    }

    public void setDentroParcheggio(boolean dentroParcheggio) {
        this.dentroParcheggio = dentroParcheggio;
    }

    public LocalDateTime getDataIngresso() {
        return data_ingresso;
    }

    public void setDataIngresso(LocalDateTime data_ingresso) {
        this.data_ingresso = data_ingresso;
    }
}
