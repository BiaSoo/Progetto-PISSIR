using GestioneParcheggio.Services;

namespace GestioneParcheggio.Middleware
{
    public class TokenMiddleware
    {
        private readonly RequestDelegate _next;
        private readonly TokenValidator _tokenValidator;

        public TokenMiddleware(RequestDelegate next, TokenValidator tokenValidator)
        {
            _next = next;
            _tokenValidator = tokenValidator;
        }

        public async Task InvokeAsync(HttpContext context)
        {
            Console.WriteLine("WE ora prendo token");
            var token = context.Request.Cookies["token"];
            if (string.IsNullOrEmpty(token))
            {
                context.Response.Redirect("/login");
                return;
            } 

            if (!_tokenValidator.ValidateToken(token))
            {
                context.Response.Redirect("/login");
                return;
            }

            await _next(context);
        }
    }
}