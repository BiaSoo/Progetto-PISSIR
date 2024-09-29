namespace GestioneParcheggio.Models
{
    public class Utente
    {
        public string QrCode { get; set; } // id equivale al QRCode
        public string Username { get; set; }
        public string Password { get; set; }
        public TipoUtente TipoUtente { get; set; }

        public Utente(string qrCode, string username, string password, TipoUtente tipoUtente)
        {
            QrCode = qrCode;
            Username = username;
            Password = password;
            TipoUtente = tipoUtente;
        }
    }

    public enum TipoUtente
    {
        Base,
        Premium,
        Admin
    }
}