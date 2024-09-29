using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;
using System.Net.Http.Headers;

namespace GestioneParcheggio.Pages
{
    public class ModificaPrezziModel : PageModel
    {
        private readonly AuthService _authService;

        [BindProperty(Name = "costoSosta")]
        public string? CostoSosta { get; set; }

        [BindProperty(Name = "costoRicarica")]
        public string? CostoRicarica { get; set; }

        public ModificaPrezziModel(AuthService authService)
        {
            _authService = authService;
        }

        public async Task<IActionResult> OnGetAsync()
        {

            if (!_authService.IsAuthenticated(HttpContext.Request))
            {
                return RedirectToPage("Index");
            }

            if (!_authService.IsAdmin(HttpContext.Request))
            {
                return RedirectToPage("IndexUser");
            }

            await LoadCurrentPricesAsync();

            return Page();
        }

        private async Task LoadCurrentPricesAsync()
        {
            try
            {
                var backendUrl = $"http://localhost:8080/api/currentPrices";

                using var httpClient = new HttpClient();

                var response = await httpClient.GetAsync(backendUrl);

                if (response.IsSuccessStatusCode)
                {
                    var responseBody = await response.Content.ReadAsStringAsync();

                    var options = new JsonSerializerOptions
                    {
                        PropertyNameCaseInsensitive = true
                    };
                    var deserializedCurrentPrices = JsonSerializer.Deserialize<Dictionary<string, object>>(responseBody, options);

                    // Verifica se la deserializzazione ha avuto successo
                    if (deserializedCurrentPrices != null)
                    {
                        CostoSosta = deserializedCurrentPrices["costoSosta"].ToString();
                        CostoRicarica = deserializedCurrentPrices["costoRicarica"].ToString();
                    }
                }
                else
                {
                    CostoSosta = "";
                    CostoRicarica = "";
                }
            }
            catch (Exception)
            {
                CostoSosta = "";
                CostoRicarica = "";
            }
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

            var backendUrl = $"http://localhost:8080/api/modificaPrezzi";
            try
            {
                // Serializzazione dei dati della carta in JSON per l'invio
                var requestData = new { CostoSosta, CostoRicarica };
                var jsonRequest = JsonSerializer.Serialize(requestData);

                // Invio effettivo della richiesta POST al backend
                var response = await httpClient.PostAsync(backendUrl, new StringContent(jsonRequest, Encoding.UTF8, "application/json"));
                if (response.IsSuccessStatusCode)
                {
                    var responseBody = await response.Content.ReadAsStringAsync();
                    var responseJson = JsonSerializer.Deserialize<UpgradeResponse>(responseBody, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    TempData["SuccessMessage"] = "Prezzi aggiornati correttamente!";
                    return RedirectToPage("ModificaPrezzi");
                }
                else
                {
                    var errorResponse = await response.Content.ReadAsStringAsync();
                    var errorMessage = JsonSerializer.Deserialize<UpgradeResponse>(errorResponse, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    TempData["ErrorMessage"] = errorMessage?.Message;
                    return RedirectToPage("ModificaPrezzi");
                }
            }
            catch (HttpRequestException)
            {
                TempData["ErrorMessage"] = "Errore del Server";
                return RedirectToPage("ModificaPrezzi");
            }
            catch (Exception)
            {
                TempData["ErrorMessage"] = "Errore del Server";
                return RedirectToPage("ModificaPrezzi");
            }
        }

        public class UpgradeResponse
        {
            public string? Message { get; set; }
        }
    }
}
