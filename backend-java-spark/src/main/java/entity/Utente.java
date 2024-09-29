package entity;

public class Utente {
    private String qrcode; // id equivale al QRCode
    private String username;
    private String password;
    private TipoUtente tipoUtente; // Modifica il tipo di dato

    public Utente(String qrcode, String username, String password, TipoUtente tipoUtente) {
        this.qrcode = qrcode;
        this.username = username;
        this.password = password;
        this.tipoUtente = tipoUtente;
    }

    // Metodi getter e setter per gli attributi della classe Utente

    public String getQrcode() {
        return qrcode;
    }

    public void setQrcode(String qrcode) {
        this.qrcode = qrcode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public TipoUtente getTipoUtente() {
        return tipoUtente;
    }

    public void setTipoUtente(TipoUtente tipoUtente) {
        this.tipoUtente = tipoUtente;
    }

    public enum TipoUtente {
        Base,
        Premium,
        Admin
    }    
}
