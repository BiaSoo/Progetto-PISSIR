using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;
using Microsoft.AspNetCore.Mvc;
using System.Text;
using System.Text.Json;
using MQTTnet.Server;
using GestioneParcheggio.mqtt;

namespace GestioneParcheggio.Pages
{
  public class NotificaModel : PageModel
  {
      private readonly AuthService _authService;
      private readonly MqttService _mqttService;
      public string ?Message { get; set; }

      public NotificaModel(AuthService authService, MqttService mqttService)
      {
          _authService = authService;
          _mqttService = mqttService;
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
            string? Message = mqtt.MqttService.ReceivedMessage;
            if (Message != null) {
            using JsonDocument jsonDoc = JsonDocument.Parse(Message);
            int idRicarica = jsonDoc.RootElement.GetProperty("id_ricarica").GetInt32();
            await LoadRicaricaAsync(idRicarica, qrCode);
            //Message = $"Ricarica effettuata con id {idRicarica}";
        }
          }

          return Page();
      }

    private async Task LoadRicaricaAsync(int id_ricarica, string qrCode)
    {
        string message; 

        try
        {
            var backendUrl = $"http://localhost:8080/api/check_ricarica_effettuata?id_ricarica={id_ricarica}";
            using var httpClient = new HttpClient();
            var response = await httpClient.GetAsync(backendUrl);

            if (response.IsSuccessStatusCode)
            {
                var responseBody = await response.Content.ReadAsStringAsync();
                var jsonObject = JsonDocument.Parse(responseBody);
                var idUtente = jsonObject.RootElement.GetProperty("id_utente").GetString();
                var targa = jsonObject.RootElement.GetProperty("targa").GetString();

                if (idUtente == qrCode)
                {
                    message = $"MWBot ha completato la ricarica alla tua auto con targa: {targa}";
                }
                else
                {
                    message = "";
                }
            }
            else
            {
                message = "";
            }
        }
        catch (Exception)
        {
            message = "";
        }

        Message = message;
    }
  }
}
