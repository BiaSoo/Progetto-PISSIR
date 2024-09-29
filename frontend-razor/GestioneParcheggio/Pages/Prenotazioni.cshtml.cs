using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;
using System.Text.Json;
using Newtonsoft.Json;
using System.Text.Json.Serialization;
using System.Text;
using System.Net.Http.Headers;

namespace GestioneParcheggio.Pages;

public class PrenotazioniModel : PageModel
{
    private readonly AuthService _authService;
    public List<Prenotazione>? Prenotazioni { get; set; }

    public PrenotazioniModel(AuthService authService)
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
            await LoadReservationsAsync(qrCode);
        }

        return Page();
    } 

    private async Task LoadReservationsAsync(string qrCode)
    {
        try
        {
            var backendUrl = $"http://localhost:8080/api/mieprenotazioni?qrCode={qrCode}";

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

                var deserializedReservations = JsonConvert.DeserializeObject<List<Prenotazione>>(responseBody);

                // Verifica se la deserializzazione ha avuto successo
                if (deserializedReservations != null)
                {
                    Prenotazioni = deserializedReservations;
                }
                else {
                    Prenotazioni = new List<Prenotazione>();
                }
            }
            else
            {
                Prenotazioni = new List<Prenotazione>();
            }
        }
        catch (Exception)
        {
           Prenotazioni = new List<Prenotazione>();
        }
    }

    public async Task<IActionResult> OnPostDeletePrenotazioneAsync(int idPrenotazione)
    {
        try
        {
            // Crea un oggetto JSON con l'ID della prenotazione
            var json = new { id_prenotazione = idPrenotazione };

            // Converte l'oggetto JSON in una stringa
            var jsonStr = JsonConvert.SerializeObject(json);

            // Crea un contenuto HTTP con il tipo di contenuto JSON
            var content = new StringContent(jsonStr, Encoding.UTF8, "application/json");

            // Configura l'HttpClient
            var httpClient = new HttpClient();
            httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

            var request = new HttpRequestMessage(HttpMethod.Delete, "http://localhost:8080/api/cancellaPrenotazione")
            {
                Content = content
            };

            var response = await httpClient.SendAsync(request);

            // Verifica se la risposta è OK
            if (response.IsSuccessStatusCode)
            {
                // Ritorna un messaggio di successo
                TempData["SuccessMessage"] = "Prenotazione cancellata con Successo";
                return RedirectToPage("Prenotazioni");
            }
            else
            {
                TempData["SuccessMessage"] = "Errore durante la cancellazione della prenotazione";
                return RedirectToPage("Prenotazioni");
            }
        }
        catch (Exception)
        {
            TempData["SuccessMessage"] = "Errore del server";
            return RedirectToPage("Prenotazioni");
        }
    }

    public class Prenotazione
    {
        [JsonPropertyName("idPostoAuto")]
        public int? IdPostoAuto { get; set; }

        [JsonPropertyName("tempoArrivo")]
        public DateTime TempoArrivo { get; set; }

        [JsonPropertyName("durataPermanenza")]
        public long DurataPermanenza { get; set; }

        [JsonPropertyName("tempoUscita")]
        public DateTime TempoUscita { get; set; }

        [JsonPropertyName("timestampCreazione")]
        public DateTime TimestampCreazione { get; set; }

        [JsonPropertyName("id_prenotazione")]
        public int Id_prenotazione { get; set; }
    }
}