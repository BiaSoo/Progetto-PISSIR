package entity;

import java.time.LocalDateTime;

public class Parcheggio{

    private int id_parcheggio;
    private boolean occupato;
    private boolean riservato;
    private int id_veicolo;
    private LocalDateTime data_ora_arrivo;
    private String id_utente;

    public Parcheggio(int id_parcheggio, boolean occupato, boolean riservato, int id_veicolo, boolean flag_is_entrato){
        this.id_parcheggio=id_parcheggio;
        this.riservato=riservato;
        this.occupato=occupato;
        this.id_veicolo=id_veicolo;
    }

    public Parcheggio(int id_parcheggio, boolean occupato, boolean riservato, int id_veicolo, boolean flag_is_entrato, String id_utente){
        this.id_parcheggio=id_parcheggio;
        this.riservato=riservato;
        this.occupato=occupato;
        this.id_veicolo=id_veicolo;
        this.id_utente = id_utente;
    }

    public Parcheggio() {
    }

    public int getId_parcheggio() {
        return id_parcheggio;
    }

    public void setId_parcheggio(int id_parcheggio) {
        this.id_parcheggio = id_parcheggio;
    }

    public boolean isOccupato() {
        return occupato;
    }

    public void setOccupato(boolean occupato) {
        this.occupato = occupato;
    }

    public boolean getRiservato() {
        return riservato;
    }

    public void setRiservato(boolean riservato) {
        this.riservato = riservato;
    }

    public int getId_veicolo() {
        return id_veicolo;
    }

    public void setId_veicolo(int id_veicolo) {
        this.id_veicolo = id_veicolo;
    }

    public LocalDateTime getData_ora_arrivo() {
        return data_ora_arrivo;
    }

    public void setData_ora_arrivo(LocalDateTime data_ora_arrivo) {
        this.data_ora_arrivo = data_ora_arrivo;
    }

    public String getId_utente() {
        return id_utente;
    }

    public void setId_utente(String id_utente) {
        this.id_utente = id_utente;
    }
}