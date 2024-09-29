using Microsoft.AspNetCore.Mvc.RazorPages;

namespace GestioneParcheggio.Pages.Shared.Modals
{
    public class ErrorModalModel : PageModel
    {
        public string? ErrorMessage { get; set; }
    }
}