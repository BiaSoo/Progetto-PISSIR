using System.Text.Json;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;
using Microsoft.AspNetCore.Mvc;
using System.Net.Http.Headers;
using System.Globalization;
using System.Text;
using System.Text.Json.Serialization;
using Newtonsoft.Json;
using JsonSerializer = System.Text.Json.JsonSerializer;

namespace GestioneParcheggio.Pages
{
    public class TransazioniUtentiModel : PageModel
    {
        private readonly AuthService _authService;

        [BindProperty(Name = "ingresso-search")]
        public string? InizioString { get; set; }

        [BindProperty(Name = "uscita-search")]
        public string? FineString { get; set; }

        public DateTime Inizio { get; set; }
        public DateTime Fine { get; set; }

        [BindProperty(Name = "pagamenti-sosta")]
        public bool PagamentiSosta { get; set; }

        [BindProperty(Name = "pagamenti-ricarica")]
        public bool PagamentiRicarica { get; set; }

        [BindProperty(Name = "pagamenti-abbonamento")]
        public bool PagamentiAbbonamento { get; set; }

        [BindProperty(Name = "utenti-base")]
        public bool UtentiBase { get; set; }

        [BindProperty(Name = "utenti-premium")]
        public bool UtentiPremium { get; set; }

        public List<string>? FilterList { get; set; }

        public List<Transazione>? Transazioni { get; set; }

        public bool IsSearchExecuted { get; set; } // per far comparire il risultato della ricerca solo dopo aver mandato la richiesta POST al backend spark

        public TransazioniUtentiModel(AuthService authService)
        {
            _authService = authService;
        }

        public IActionResult OnGet()
        {
            if (!_authService.IsAuthenticated(HttpContext.Request))
            {
                return RedirectToPage("Index");
            }

            if (!_authService.IsAdmin(HttpContext.Request))
            {
                return RedirectToPage("IndexUser");
            }

            return Page();
        }

        public async Task<IActionResult> OnPostAsync()
        {
            if (!_authService.IsAuthenticated(HttpContext.Request))
            {
                return RedirectToPage("Index");
            }

            var token = _authService.GetToken(HttpContext.Request);
            using var httpClient = new HttpClient();
            httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);

            // Verifica se le date sono valide
            if (!DateTime.TryParseExact(InizioString, "dd-MM-yyyy HH:mm", CultureInfo.InvariantCulture, DateTimeStyles.None, out DateTime ingressoTemp) ||
                !DateTime.TryParseExact(FineString, "dd-MM-yyyy HH:mm", CultureInfo.InvariantCulture, DateTimeStyles.None, out DateTime uscitaTemp))
            {
                TempData["ErrorMessage"] = "La data di Ingresso o di Uscita non è nel formato corretto";
                return RedirectToPage("TransazioniUtenti");
            }  
            Inizio = ingressoTemp;
            Fine = uscitaTemp;

            // Verifica se la data di fine è precedente a quella di inizio
            if (Fine <= Inizio)
            {
                TempData["ErrorMessage"] = "La data di Fine deve essere successiva a quella di Inizio";
                return RedirectToPage("TransazioniUtenti");
            }

            FilterList = new List<String>();

            // Creo lista dei filtri selezionati da inviare al backend
            if (PagamentiSosta) FilterList.Add("sosta");
            if (PagamentiRicarica) FilterList.Add("ricarica");
            if (PagamentiAbbonamento) FilterList.Add("abbonamento");
            if (UtentiBase) FilterList.Add("base");
            if (UtentiPremium) FilterList.Add("premium");

            string filters = string.Join(",", FilterList);       

            try {
                var formData = new
                {
                    dataOraInizio = Inizio.ToString("dd-MM-yyyy HH:mm"),
                    dataOraFine = Fine.ToString("dd-MM-yyyy HH:mm"),
                    filters
                };

                var response = await httpClient.PostAsync("http://localhost:8080/api/transazioniUtenti", new StringContent(JsonSerializer.Serialize(formData), Encoding.UTF8, "application/json"));

                if (response.IsSuccessStatusCode)
                {
                    var responseBody = await response.Content.ReadAsStringAsync();

                    // Utilizza System.Text.Json per deserializzare il JSON
                    var options = new JsonSerializerOptions
                    {
                        PropertyNameCaseInsensitive = true
                    };

                    var deserializedTransactions = JsonConvert.DeserializeObject<List<Transazione>>(responseBody);

                    // Verifica se la deserializzazione ha avuto successo
                    if (deserializedTransactions != null)
                    {
                        Transazioni = deserializedTransactions;
                    }
                    else {
                        Transazioni = new List<Transazione>();
                    }
                }
                else
                {
                    Transazioni = new List<Transazione>();
                }
            } 
            catch (HttpRequestException)
            {
                return RedirectToPage("Index");
            }
            catch (Exception)
            {
                Transazioni = new List<Transazione>();
            }

            IsSearchExecuted = true;

            return Page();
        }

        public class Transazione
        {
            [JsonPropertyName("username")]
            public string? Username { get; set; }

            [JsonPropertyName("tipoTransazione")]
            public string? TipoTransazione { get; set; }

            [JsonPropertyName("importo")]
            public string? Importo { get; set; }

            [JsonPropertyName("dataTransazione")]
            public DateTime DataTransazione { get; set; }
        }
    }
}
