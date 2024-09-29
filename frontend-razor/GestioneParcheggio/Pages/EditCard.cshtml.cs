using System.Text.Json;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;
using Microsoft.AspNetCore.Mvc;
using JsonSerializer = System.Text.Json.JsonSerializer;
using System.Text.RegularExpressions;
using System.Text;
using System.Net.Http.Headers;

namespace GestioneParcheggio.Pages
{
    public class EditCardModel : PageModel
    {
        private readonly AuthService _authService;

        public List<string>? CardTypes { get; set; }

        [BindProperty(Name = "tipoCarta")]
        public string? CardType { get; set; }
            
        [BindProperty(Name = "numeroCarta")]
        public string? CardNumber { get; set; }

        public string? CardNumberFormatted { get; set; }

        public string? CardNumberNoSpaces { get; set; }

        [BindProperty(Name = "dataScadenza")]
        public string? ExpirationDate { get; set; }

        [BindProperty(Name = "cvv")]
        public string? CVV { get; set; }

        public EditCardModel(AuthService authService)
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

            await LoadCardTypesAsync();

            string? qrCode = _authService.GetQrCode(HttpContext.Request);

            if (qrCode != null) {
                await LoadCurrentCardAsync(qrCode);
            }

            return Page();
        }

        private async Task LoadCardTypesAsync()
        {
            try
            {
                var backendUrl = $"http://localhost:8080/api/cardTypes";

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
                        CardTypes = deserializedCardTypes;
                    }
                    else {
                        CardTypes = new List<string>();
                    }
                }
                else
                {
                    CardTypes = new List<string>();
                }
            }
            catch (Exception)
            {
                CardTypes = new List<string>();
            }
        }

        private async Task LoadCurrentCardAsync(string qrCode)
        {
            try
            {
                var backendUrl = $"http://localhost:8080/api/currentCard?qrCode={qrCode}";

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
                    var deserializedCard = JsonSerializer.Deserialize<Dictionary<string, object>>(responseBody, options);

                    // Verifica se la deserializzazione ha avuto successo
                    if (deserializedCard != null)
                    {
                        // Estrarre i dettagli della carta di credito dalla mappa
                        CardType = deserializedCard["tipo_carta"].ToString();
                        CardNumber = deserializedCard["numero_carta"].ToString();
                        ExpirationDate = deserializedCard["data_scadenza"].ToString();
                        CVV = deserializedCard["cvv"].ToString();

                        if (CardNumber != null) {
                            CardNumberFormatted = Regex.Replace(CardNumber, @"(\d{4})(?=\d)", "$1 ");
                        }
                    }
                    else
                    {
                        CardType = "";
                        CardNumber = "";
                        ExpirationDate = "";
                        CVV = "";
                    }
                }
                else
                {
                    CardType = "";
                    CardNumber = "";
                    ExpirationDate = "";
                    CVV = "";   
                }
            }
            catch (Exception)
            {
                CardType = "";
                CardNumber = "";
                ExpirationDate = "";
                CVV = ""; 
            }
        }

        public async Task<IActionResult> OnPostAsync()
        {
            // Invio i dati al backend spark se utente è autenticato
            if (!_authService.IsAuthenticated(HttpContext.Request))
            {
                return RedirectToPage("Index");
            }

            // Controllo che i dati della carta siano validi
            if (!IsValidCardData())
            {
                TempData["ErrorMessage"] = "Formato dati della carta non validi";
                return RedirectToPage("EditCard");
            }

            var token = _authService.GetToken(HttpContext.Request);
            using var httpClient = new HttpClient();
            httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);

            var backendUrl = $"http://localhost:8080/api/upgradeToCard";
            try
            {
                // Serializzazione dei dati della carta in JSON per l'invio
                var requestData = new { tipoCarta = CardType, numeroCarta = CardNumberNoSpaces, dataScadenza = ExpirationDate, cvv = CVV };
                var jsonRequest = JsonSerializer.Serialize(requestData);

                // Invio effettivo della richiesta POST al backend
                var response = await httpClient.PostAsync(backendUrl, new StringContent(jsonRequest, Encoding.UTF8, "application/json"));
                if (response.IsSuccessStatusCode)
                {
                    var responseBody = await response.Content.ReadAsStringAsync();
                    var responseJson = JsonSerializer.Deserialize<UpgradeResponse>(responseBody, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    TempData["SuccessMessage"] = responseJson?.Message;
                    return RedirectToPage("EditCard");
                }
                else
                {
                    var errorResponse = await response.Content.ReadAsStringAsync();
                    var errorMessage = JsonSerializer.Deserialize<UpgradeResponse>(errorResponse, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    TempData["ErrorMessage"] = errorMessage?.Message;
                    return RedirectToPage("EditCard");
                }
            }
            catch (HttpRequestException)
            {
                TempData["ErrorMessage"] = "Il Server non risponde: Riprova tra qualche minuto";
                return RedirectToPage("EditCard");
            }
            catch (Exception)
            {
                TempData["ErrorMessage"] = "Si è verificato un errore: Riprova tra qualche minuto";
                return RedirectToPage("EditCard");
            }
        }

        private bool IsValidCardData()
        {
            // Rimuovi tutti gli spazi dal numero della carta
            CardNumberNoSpaces = CardNumber?.Replace(" ", "") ?? string.Empty;

            // Verifica il numero della carta (deve essere di 16 cifre senza spazi)
            if (!Regex.IsMatch(CardNumberNoSpaces, @"^\d{16}$"))
            {
                return false;
            }

            // Verifica la data di scadenza (deve essere nel formato MM/YY)
            if (string.IsNullOrWhiteSpace(ExpirationDate) || !Regex.IsMatch(ExpirationDate, @"^(0[1-9]|1[0-2])/\d{2}$"))
            {
                return false;
            }

            // Verifica il CVV (deve essere di 3 cifre)
            if (string.IsNullOrWhiteSpace(CVV) || !Regex.IsMatch(CVV, @"^\d{3}$"))
            {
                return false;
            }

            return true;
        }

        public class UpgradeResponse
        {
            public string? Token { get; set; }
            public string? Message { get; set; }
        }
    }
}

