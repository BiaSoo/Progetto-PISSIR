using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;

namespace GestioneParcheggio.Pages
{
    public class IndexModel : PageModel
    {
        private readonly ILogger<IndexModel> _logger;

        private readonly AuthService _authService;

        public IndexModel(ILogger<IndexModel> logger, AuthService authService)
        {
            _logger = logger;
            _authService = authService;
        }

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
    }
}