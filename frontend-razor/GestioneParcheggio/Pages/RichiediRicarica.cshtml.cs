using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;
using System.Text.Json;
using JsonSerializer = System.Text.Json.JsonSerializer;
using System.Text.RegularExpressions;
using System.Net.Http.Headers;
using System.Text;

namespace GestioneParcheggio.Pages;

public class RichiediRicaricaModel : PageModel
{
    private readonly AuthService _authService;

    public List<string>? LicensePlates { get; set; }

    [BindProperty(Name = "percentualeRicarica")]
    public string? PercentualeRicarica { get; set; }

    [BindProperty(Name = "veicolo")]
    public string? Veicolo { get; set; }

    int totalMinutes;

    public RichiediRicaricaModel(AuthService authService)
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
           await LoadLicensePlateAsync(qrCode);
        }

        return Page();
    }

    private async Task LoadLicensePlateAsync(string qrCode)
    {
        try
        {
            var backendUrl = $"http://localhost:8080/api/veicoliNelParcheggio?qrCode={qrCode}";

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
                var deserializedCardTypes = JsonSerializer.Deserialize<List<string>>(responseBody, options);

                // Verifica se la deserializzazione ha avuto successo
                if (deserializedCardTypes != null)
                {
                    LicensePlates = deserializedCardTypes;
                }
                else {
                    LicensePlates = new List<string>();
                }
            }
            else
            {
                LicensePlates = new List<string>();
            }
        }
        catch (Exception)
        {
            LicensePlates = new List<string>();
        }
    }

    public async Task<IActionResult> OnPostAsync()
    {
        // Controllo che i dati della carta siano validi
        if (!IsValidFormatRicarica())
        {
            TempData["ErrorMessage"] = "I dati inseriti non sono validi";
            return RedirectToPage("RichiediRicarica");
        }

        // Invio i dati al backend spark se utente è autenticato
        if (!_authService.IsAuthenticated(HttpContext.Request))
        {
            return RedirectToPage("Index");
        }

        using var httpClient = new HttpClient();
        var backendUrl = "http://localhost:8080/api/richiediRicarica";

        try
        {
            // Invio la richiesta al backend
            var response = await httpClient.PostAsync(backendUrl, null);

            // Controllo lo stato della risposta
            if (response.IsSuccessStatusCode)
            {
                // Leggo la risposta del backend
                var responseBody = await response.Content.ReadAsStringAsync();

                try
                {
                    totalMinutes = int.Parse(responseBody);
                }
                catch (FormatException)
                {
                    TempData["ErrorMessage"] = "Errore del Server";
                    return RedirectToPage("RichiediRicarica");
                }

                if (totalMinutes == 0)
                    TempData["Message"] = "MWBot è disponibile ora";
                else
                    TempData["Message"] = "MWBot è disponibile tra circa: " +totalMinutes + " minuti";
            }
            else
            {
                TempData["ErrorMessage"] = "Errore del Server";
            }
        }
        catch (HttpRequestException)
        {
            TempData["ErrorMessage"] = "Errore del Server";
        }
        catch (Exception)
        {
            TempData["ErrorMessage"] = "Errore del Server";
        }

        // Memorizzo i dati inseriti dall'utente in TempData
        TempData["PercentualeRicarica"] = PercentualeRicarica;
        TempData["Veicolo"] = Veicolo;

        return RedirectToPage("RichiediRicarica");
    }

    public async Task<IActionResult> OnPostConfermaAsync()
    {
        // Invio i dati al backend spark se utente è autenticato
        if (!_authService.IsAuthenticated(HttpContext.Request))
        {
            return RedirectToPage("Index");
        }

        var token = _authService.GetToken(HttpContext.Request);
        using var httpClient = new HttpClient();
        httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var backendUrl = $"http://localhost:8080/api/confermaRicarica";

        // Recupero i dati memorizzati in TempData
        PercentualeRicarica = TempData["PercentualeRicarica"] as string;
        Veicolo = TempData["Veicolo"] as string;

        try {
                // Serializzazione dei dati della carta in JSON per l'invio
                var requestData = new { percentualeRicarica = PercentualeRicarica, veicolo = Veicolo };
                var jsonRequest = JsonSerializer.Serialize(requestData);

                // Invio effettivo della richiesta POST al backend
                var response = await httpClient.PostAsync(backendUrl, new StringContent(jsonRequest, Encoding.UTF8, "application/json"));
                if (response.IsSuccessStatusCode)
                {
                    var responseBody = await response.Content.ReadAsStringAsync();

                    // Utilizza System.Text.Json per deserializzare il JSON
                    var options = new JsonSerializerOptions
                    {
                        PropertyNameCaseInsensitive = true
                    };
                    var deserializedMinutes = JsonSerializer.Deserialize<Dictionary<string, object>>(responseBody, options);
                    string? totalMinutesConfermed;

                    if (deserializedMinutes == null) {
                        TempData["ErrorMessage"] = "Errore del Server";
                        return RedirectToPage("RichiediRicarica");   
                    }

                    totalMinutesConfermed = deserializedMinutes["totalMinutes"].ToString();

                    if (totalMinutesConfermed != null && totalMinutesConfermed == "0") {
                        TempData["SuccessMessage"] = "MWBot raggiungerà la tua auto tra pochi istanti";
                    }
                    else {
                        TempData["SuccessMessage"] = "MBW raggiungerà la tua auto tra circa: " + totalMinutesConfermed + " minuti";
                    }

                    return RedirectToPage("RichiediRicarica");
                }
                else
                {
                    TempData["ErrorMessage"] = "Errore del Server";
                    return RedirectToPage("RichiediRicarica");
                }
        } 
        catch (HttpRequestException)
        {
            TempData["ErrorMessage"] = "Errore del Server";
        }
        catch (Exception)
        {
            TempData["ErrorMessage"] = "Errore del Server";
        }

        return RedirectToPage("RichiediRicarica");
    }

    private bool IsValidFormatRicarica()
    {
        if (string.IsNullOrWhiteSpace(PercentualeRicarica) || !Regex.IsMatch(PercentualeRicarica, @"^[1-9]\d{1,2}$"))
        {
            return false;
        }

        return true;
    }
}