using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using GestioneParcheggio.Services;

namespace GestioneParcheggio.Pages {
  public class LogoutModel : PageModel
  {
    private readonly AuthService _authService;
    public LogoutModel(AuthService authService)
    {
        _authService = authService;
    }

        public IActionResult OnPost()
        {        
            bool isDisconnected = _authService.Logout(HttpContext.Response, HttpContext.Request);

            if (isDisconnected) {
              bool aut = _authService.IsAuthenticated(HttpContext.Request);
              return RedirectToPage("Index");
            }

            else
                return RedirectToPage("Index");
        }
    }
}
