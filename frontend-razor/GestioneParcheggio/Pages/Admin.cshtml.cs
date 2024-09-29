using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;

namespace GestioneParcheggio.Pages
{
    public class AdminModel : PageModel
    {
        private readonly AuthService _authService;

        public AdminModel(AuthService authService)
        {
            _authService = authService;
        }

        public IActionResult OnGet()
        {
            if (!_authService.IsAuthenticated(HttpContext.Request))
            {
                return RedirectToPage("Index");
            }

            if (!_authService.IsAdmin(HttpContext.Request))
            {
                return RedirectToPage("IndexUser");
            }

            return Page();
        }
    }
}
