using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;
using System.Text.Json;
using System.Net.Http.Headers;

namespace GestioneParcheggio.Pages;

public class UserTypeModel : PageModel
{
    private readonly AuthService _authService;
    public bool IsPremiumUser { get; set; }

    public UserTypeModel(AuthService authService)
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

        // Mi serve per capire cosa mostrare all'utente
        IsPremiumUser = _authService.IsPremiumUser(HttpContext.Request);

        return Page();
    }

    public async Task<IActionResult> OnPostUpgradeToPremiumAsync()
    {
        var backendUrl = "http://localhost:8080/api/upgradeToPremium";

        // se token non è presente o scaduto non posso chiedere il passaggio premium
        if (!_authService.IsAuthenticated(HttpContext.Request))
            return RedirectToPage("Index");

        var token = _authService.GetToken(HttpContext.Request);

        using var httpClient = new HttpClient();
        httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);

        try
        {
            var response = await httpClient.PostAsync(backendUrl, null);

            if (response.IsSuccessStatusCode)
            {
                var responseBody = await response.Content.ReadAsStringAsync();
                var responseJson = JsonSerializer.Deserialize<UpgradeResponse>(responseBody, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                if (responseJson != null && responseJson.Token != null)
                {
                    HttpContext.Response.Cookies.Delete("Token");
                    HttpContext.Response.Cookies.Append("Token", responseJson.Token);

                    IsPremiumUser = true;
                    TempData["SuccessMessage"] = "Passaggio a Premium effettuato con successo!";
                    return Page();
                }
                else
                {
                    TempData["ErrorMessage"] = "Errore durante il passaggio a Premium: Riprova tra qualche minuto";
                    return Page();
                }
            }
            else
            {
                // Gestisco gli errori ricevuti dal server
                var errorResponse = await response.Content.ReadAsStringAsync();
                var errorMessage = JsonSerializer.Deserialize<UpgradeResponse>(errorResponse, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                TempData["ErrorMessage"] = errorMessage?.Message;
                return Page();
            }
        }
        catch (HttpRequestException)
        {
            // In caso dovesse fallire la richiesta al server (es: server spento)
            TempData["ErrorMessage"] = "Il Server non risponde: Riprova tra qualche minuto";
            return Page();
        }
        catch (Exception)
        {
            TempData["ErrorMessage"] = "Il Server non risponde: Riprova tra qualche minuto";
            return Page();
        }
    }

    public class UpgradeResponse
    {
        public string? Token { get; set; }
        public string? Message { get; set; }
    }
}