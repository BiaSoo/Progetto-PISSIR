using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;
using Microsoft.AspNetCore.Mvc;
using QRCoder;
using SixLabors.ImageSharp;
using SixLabors.ImageSharp.Formats.Png;
using SixLabors.ImageSharp.PixelFormats;
using SixLabors.ImageSharp.Processing;

namespace GestioneParcheggio.Pages
{
    public class QRCodeUserModel : PageModel
    {
        private readonly AuthService _authService;
        public byte[]? QRCodeImage { get; set; }

        public QRCodeUserModel(AuthService authService)
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

            string? qrCodeString = _authService.GetQrCode(HttpContext.Request);

            if (qrCodeString!= null)
            {
                // Crea un nuovo oggetto QRCodeGenerator
                QRCodeGenerator qrGenerator = new();

                // Genera il codice QR
                QRCodeData qrCodeData = qrGenerator.CreateQrCode(qrCodeString, QRCodeGenerator.ECCLevel.Q);

                Base64QRCode qrCode = new(qrCodeData);
                string qrCodeImageAsBase64 = qrCode.GetGraphic(20);
                byte[] qrCodeImageBytes = Convert.FromBase64String(qrCodeImageAsBase64);

                // Rimpicciolisco l'immagine del QR code
                using MemoryStream ms = new(qrCodeImageBytes);
                using Image<Rgba32> image = Image.Load<Rgba32>(ms);
                image.Mutate(x => x.Resize(new Size(250, 250)));
                using MemoryStream msResized = new();
                image.Save(msResized, new PngEncoder());
                QRCodeImage = msResized.ToArray();
            }

            return Page();
        }
    }
}
