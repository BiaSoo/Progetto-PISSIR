using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.Text;
using System.Text.Json;
using System.Net.Http.Headers;
using GestioneParcheggio.Services;

namespace GestioneParcheggio.Pages
{
    public class LoginModel : PageModel
    {
        private readonly AuthService _authService;

        public LoginModel(AuthService authService)
        {
            _authService = authService;
            Username = string.Empty;
            Password = string.Empty;
        }

        [BindProperty]
        public string Username { get; set; }
        [BindProperty]
        public string Password { get; set; }

        public IActionResult OnGet()
        {
            if (!_authService.IsAdmin(HttpContext.Request))
            {
                if (_authService.IsAuthenticated(HttpContext.Request))
                {
                    return RedirectToPage("IndexUser");
                }

                return Page();
            }

            return RedirectToPage("Admin");
        }

        public async Task<IActionResult> OnPostAsync()
        {
            try
            {
                var requestUrl = "http://localhost:8080/api/login";
                var credentials = new { Username, Password };
                var json = JsonSerializer.Serialize(credentials, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                var content = new StringContent(json, Encoding.UTF8, "application/json");
                content.Headers.ContentType = new MediaTypeHeaderValue("application/json");

                using var httpClient = new HttpClient();

                var response = await httpClient.PostAsync(requestUrl, content);

                if (response.IsSuccessStatusCode)
                {
                    var responseBody = await response.Content.ReadAsStringAsync();
                    var responseJson = JsonSerializer.Deserialize<LoginResponse>(responseBody, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    if (responseJson != null && responseJson.Token != null)
                    {
                        HttpContext.Response.Cookies.Append("Token", responseJson.Token, new CookieOptions { HttpOnly = true });

                        // Controllo se è admin
                        if (Username == "admin")
                        {
                            return RedirectToPage("Admin");
                        }
                        else
                        {
                            return RedirectToPage("IndexUser");
                        }
                    }
                    else
                    {
                        TempData["ErrorMessage"] = "Errore del Server";
                        return Page();
                    }
                }
                else
                {
                    var errorResponse = await response.Content.ReadAsStringAsync();
                    TempData["ErrorMessage"] = errorResponse;
                    return Page();
                }
            }
            catch (Exception)
            {
                TempData["ErrorMessage"] = "Errore del Server";
                return Page();
            }
        }
    }

    public class LoginResponse
    {
        public string? Token { get; set; }
        public string? Message { get; set; }
    }
}