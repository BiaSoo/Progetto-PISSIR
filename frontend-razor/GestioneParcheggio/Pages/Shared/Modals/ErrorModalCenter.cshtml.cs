using Microsoft.AspNetCore.Mvc.RazorPages;

namespace GestioneParcheggio.Pages.Shared.Modals
{
    public class ErrorModalCenterModel : PageModel
    {
        public string? ErrorMessage { get; set; }
    }
}