package entity;

import java.sql.Date;

public class CartaCredito {
    private int idCarta;
    private String numeroCarta;
    private Date dataScadenza;
    private String cvv;
    private String idUtente;
    private TipoCarta tipoCarta;

    public CartaCredito(int idCarta, String numeroCarta, Date dataScadenza, String cvv, String idUtente, TipoCarta tipoCarta) {
        this.idCarta = idCarta;
        this.numeroCarta = numeroCarta;
        this.dataScadenza = dataScadenza;
        this.cvv = cvv;
        this.idUtente = idUtente;
        this.tipoCarta = tipoCarta;
    }

    public CartaCredito(String numeroCarta, Date dataScadenza, String cvv, String idUtente, TipoCarta tipoCarta) {
        this.numeroCarta = numeroCarta;
        this.dataScadenza = dataScadenza;
        this.cvv = cvv;
        this.idUtente = idUtente;
        this.tipoCarta = tipoCarta;
    }

    public CartaCredito() {
        
    }

    public int getIdCarta() {
        return idCarta;
    }

    public void setIdCarta(int idCarta) {
        this.idCarta = idCarta;
    }

    public String getNumeroCarta() {
        return numeroCarta;
    }

    public void setNumeroCarta(String numeroCarta) {
        this.numeroCarta = numeroCarta;
    }

    public Date getDataScadenza() {
        return dataScadenza;
    }

    public void setDataScadenza(Date dataScadenza) {
        this.dataScadenza = dataScadenza;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(String idUtente) {
        this.idUtente = idUtente;
    }

    public TipoCarta getTipoCarta() {
        return tipoCarta;
    }
    
    public void setTipoCarta(TipoCarta tipoCarta) {
        this.tipoCarta = tipoCarta;
    }

    public enum TipoCarta {
        VISA,
        MASTERCARD
    }
}
