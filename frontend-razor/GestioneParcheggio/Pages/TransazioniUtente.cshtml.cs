using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;
using System.Text.Json;
using System.Text.Json.Serialization;
using Newtonsoft.Json;

namespace GestioneParcheggio.Pages;

public class TransazioniUtenteModel : PageModel
{
    private readonly AuthService _authService;
    public List<Transazione>? Transazioni { get; set; }

    public TransazioniUtenteModel(AuthService authService)
    {
        _authService = authService;
    }

    public async Task<IActionResult> OnGetAsync()
    {
        if (_authService.IsAdmin(HttpContext.Request))
        {
            return RedirectToPage("Admin");
        }

        if (!_authService.IsAuthenticated(HttpContext.Request))
        {
            return RedirectToPage("Index");
        }

        string? qrCode = _authService.GetQrCode(HttpContext.Request);

        if (qrCode != null) {
            await LoadTransactionsAsync(qrCode);
        }

        return Page();
    } 

    private async Task LoadTransactionsAsync(string qrCode)
    {
        try
        {
            var backendUrl = $"http://localhost:8080/api/transazioniUtente?qrCode={qrCode}";

            using var httpClient = new HttpClient();

            var response = await httpClient.GetAsync(backendUrl);

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
        catch (Exception)
        {
           Transazioni = new List<Transazione>();
        }
    }

    public class Transazione
    {
        [JsonPropertyName("tipoTransazione")]
        public string? TipoTransazione { get; set; }

        [JsonPropertyName("importo")]
        public decimal Importo { get; set; }

        [JsonPropertyName("dataTransazione")]
        public DateTime DataTransazione { get; set; }
    }
}