using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;
using System.Net.Http.Headers;
using System.Globalization;

namespace GestioneParcheggio.Pages
{
    public class PrenotaPostoModel : PageModel
    {
        private readonly AuthService _authService;
        public bool IsPremiumUser { get; set; }

        [BindProperty(Name = "ingresso")]
        public string? IngressoString { get; set; }

        [BindProperty(Name = "uscita")]
        public string? UscitaString { get; set; }

        public DateTime Ingresso { get; set; }
        public DateTime Uscita { get; set; }

        public PrenotaPostoModel(AuthService authService)
        {
            _authService = authService;
        }

        public IActionResult OnGet()
        {
            if (_authService.IsAdmin(HttpContext.Request))
            {
                return RedirectToPage("Admin");
            }

            if (!_authService.IsAuthenticated(HttpContext.Request))
            {
                return RedirectToPage("Index");
            }

            // Mi serve per capire se può accedere alla pagina (solo utente premium può accedere alla pagina)
            IsPremiumUser = _authService.IsPremiumUser(HttpContext.Request);

            if (!IsPremiumUser) {
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
            if (!DateTime.TryParseExact(IngressoString, "dd-MM-yyyy HH:mm", CultureInfo.InvariantCulture, DateTimeStyles.None, out DateTime ingressoTemp) ||
                !DateTime.TryParseExact(UscitaString, "dd-MM-yyyy HH:mm", CultureInfo.InvariantCulture, DateTimeStyles.None, out DateTime uscitaTemp))
            {
                TempData["ErrorMessage"] = "La data di Ingresso o di Uscita non è nel formato corretto";
                return RedirectToPage("PrenotaPosto");
            }  
            Ingresso = ingressoTemp;
            Uscita = uscitaTemp;

            // Verifica se la data di ingresso è successiva alla data attuale
            if (Ingresso < DateTime.Now || Uscita < DateTime.Now)
            {
                TempData["ErrorMessage"] = "La data di Ingresso deve essere successiva a quella attuale";
                return Page();
            }

            // Verifica se la data di uscita è precedente a quella di ingresso
            if (Uscita <= Ingresso)
            {
                TempData["ErrorMessage"] = "La data di Uscita deve essere successiva a quella di Ingresso";
                return Page();
            }

            try {
                var formData = new
                {
                    Ingresso = Ingresso.ToString("dd-MM-yyyy HH:mm"),
                    Uscita = Uscita.ToString("dd-MM-yyyy HH:mm")
                };

                var response = await httpClient.PostAsync("http://localhost:8080/api/prenotaposto", new StringContent(JsonSerializer.Serialize(formData), Encoding.UTF8, "application/json"));

                if (response.IsSuccessStatusCode)
                {
                    var responseBody = await response.Content.ReadAsStringAsync();
                    var responseJson = JsonSerializer.Deserialize<UpgradeResponse>(responseBody, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    // Mostra messaggio di successo
                    TempData["SuccessMessage"] = responseJson?.Message;
                    return Page();
                }
                else
                {
                    var errorResponse = await response.Content.ReadAsStringAsync();
                    var errorMessage = JsonSerializer.Deserialize<UpgradeResponse>(errorResponse, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    TempData["ErrorMessage"] = errorMessage?.Message;
                    return Page();
                }
            } 
            catch (HttpRequestException)
            {
                TempData["ErrorMessage"] = "Il Server non risponde: Riprova tra qualche minuto";
                return Page();
            }
            catch (Exception)
            {
                TempData["ErrorMessage"] = "Si è verificato un errore: Riprova tra qualche minuto";
                return Page();
            }
        }

        public class UpgradeResponse
        {
            public string? Token { get; set; }
            public string? Message { get; set; }
        }
    }
}
