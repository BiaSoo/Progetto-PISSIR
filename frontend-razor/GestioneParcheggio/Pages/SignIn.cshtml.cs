using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using Microsoft.Extensions.Logging;
using GestioneParcheggio.Services;
using System.Net.Http.Headers;
using System.Text.RegularExpressions;

namespace GestioneParcheggio.Pages
{
    public class SignInModel : PageModel
    {
        private readonly AuthService _authService;

        [BindProperty]
        public string? Username { get; set; }
        [BindProperty]
        public string? Password { get; set; }

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

        [BindProperty(Name = "utente-base")]
        public bool BaseUser { get; set; }

        [BindProperty(Name = "utente-premium")]
        public bool PremiumUser { get; set; }

        public SignInModel(AuthService authService)
        {
            _authService = authService;
        }

        public List<string>? CardTypes { get; set; }

        public async Task<IActionResult> OnGetAsync()
        {
            if (!_authService.IsAdmin(HttpContext.Request))
            {
                if (_authService.IsAuthenticated(HttpContext.Request))
                {
                    return RedirectToPage("IndexUser");
                }

                await LoadCardTypesAsync();
                return Page();
            }

            return RedirectToPage("Admin");
        }

        private async Task LoadCardTypesAsync()
        {
            try
            {
                var backendUrl = "http://localhost:8080/api/cardTypes";

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

        public async Task<IActionResult> OnPostAsync()
        {
            // Controllo che i dati della carta siano validi
            if (!IsValidCardData())
            {
                TempData["ErrorMessage"] = "Formato dati della carta non validi";
                return RedirectToPage("SignIn");
            }

            // Controllo il tipo di utente selezionato
            var UserType = "";
            if (BaseUser) {
                UserType = "Base";
            }
            else if (PremiumUser) {
                UserType = "Premium";
            }

            var token = _authService.GetToken(HttpContext.Request);

            using var httpClient = new HttpClient();
            httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
            var backendUrl = $"http://localhost:8080/api/signin";

            try
            {
                // Serializzazione dei dati della carta in JSON per l'invio
                var requestData = new 
                { 
                    username = Username, 
                    password = Password, 
                    tipoCarta = CardType, 
                    numeroCarta = CardNumberNoSpaces, 
                    dataScadenza = ExpirationDate, 
                    cvv = CVV, 
                    userType = UserType 
                };

                var jsonRequest = JsonSerializer.Serialize(requestData);

                // Invio effettivo della richiesta POST al backend
                var response = await httpClient.PostAsync(backendUrl, new StringContent(jsonRequest, Encoding.UTF8, "application/json"));
                
                if (response.IsSuccessStatusCode)
                {
                    var responseBody = await response.Content.ReadAsStringAsync();
                    var responseJson = JsonSerializer.Deserialize<SignInResponse>(responseBody, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    if (responseJson != null && responseJson.Token != null)
                    {
                        HttpContext.Response.Cookies.Append("Token", responseJson.Token, new CookieOptions { HttpOnly = true });
                        
                        return RedirectToPage("IndexUser");
                    }
                    else
                    {
                        TempData["ErrorMessage"] = "Errore durante la registrazione";
                        return RedirectToPage("SignIn");
                    }
                }
                else
                {
                    var errorResponse = await response.Content.ReadAsStringAsync();
                    TempData["ErrorMessage"] = errorResponse;
                    return RedirectToPage("SignIn");
                }
            }
            catch (Exception)
            {
                TempData["ErrorMessage"] = "Errore del Server";
                return RedirectToPage("SignIn");
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

        public class SignInResponse
        {
            public string? Token { get; set; }
            public string? Message { get; set; }
        }
    }
}

