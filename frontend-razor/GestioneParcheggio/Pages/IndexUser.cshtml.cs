using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;

namespace GestioneParcheggio.Pages
{
    public class IndexUserModel : PageModel
    {
        private readonly AuthService _authService;
        public bool IsPremiumUser { get; set; }

        public IndexUserModel(AuthService authService)
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

            // Mi serve per capire se devo nascondere la card di prenotazione all'utente base
            IsPremiumUser = _authService.IsPremiumUser(HttpContext.Request);

            return Page();
        }
    }
}
