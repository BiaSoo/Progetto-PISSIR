using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;
using System.Text.Json;
using Newtonsoft.Json;
using System.Text.Json.Serialization;

namespace GestioneParcheggio.Pages
{
    public class StatoParcheggioModel : PageModel
    {
        private readonly AuthService _authService;

        public List<ParkingStatus>? Status { get; set; }

        public StatoParcheggioModel(AuthService authService)
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

            await LoadParkingStatusAsync();

            return Page();
        }

        private async Task LoadParkingStatusAsync()
        {
            try
            {
                var backendUrl = $"http://localhost:8080/api/statoParcheggio";

                using var httpClient = new HttpClient();

                var response = await httpClient.GetAsync(backendUrl);

                if (response.IsSuccessStatusCode)
                {
                    var responseBody = await response.Content.ReadAsStringAsync();

                    var options = new JsonSerializerOptions
                    {
                        PropertyNameCaseInsensitive = true
                    };
                    var deserializedParkingStatus = JsonConvert.DeserializeObject<List<ParkingStatus>>(responseBody);

                    // Verifica se la deserializzazione ha avuto successo
                    if (deserializedParkingStatus != null)
                    {
                        Status = deserializedParkingStatus;
                    }
                }
                else
                {
                    Status = new List<ParkingStatus>();
                }
            }
            catch (Exception)
            {
                Status = new List<ParkingStatus>();
            }
        }

        public class ParkingStatus
        {
            [JsonPropertyName("postoAuto")]
            public string? PostoAuto { get; set; }

            [JsonPropertyName("riservato")]
            public bool Riservato { get; set; }

            [JsonPropertyName("occupato")]
            public bool Occupato { get; set; }
        }
    }
}
