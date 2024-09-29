using System.IdentityModel.Tokens.Jwt;
using Newtonsoft.Json;
using GestioneParcheggio.Models;

namespace GestioneParcheggio.Services
{
    public class AuthService
    {
        private readonly TokenValidator _tokenValidator;

        public AuthService(TokenValidator tokenValidator)
        {
            _tokenValidator = tokenValidator;
        }

        public string? GetToken(HttpRequest request)
        {
            return request.Cookies?.TryGetValue("Token", out var token) == true ? token : null;
        }

        public bool IsAuthenticated(HttpRequest request)
        {
            var token = GetToken(request);

            if (token == null)
            {
                return false;
            }

            return _tokenValidator.ValidateToken(token);
        }

        public string? GetUserName(HttpRequest request)
        {
            var token = GetToken(request);
            if (token is null)
            {
                return null;
            }

            if (_tokenValidator.ValidateToken(token))
            {
                var handler = new JwtSecurityTokenHandler();
                var jwtToken = handler.ReadJwtToken(token);
                var usernameClaim = jwtToken.Claims.FirstOrDefault(c => c.Type == "sub");
                return usernameClaim?.Value;
            }
            return null;
        }

        public bool IsAdmin(HttpRequest request)
        {
            var token = GetToken(request);
            if (token == null)
            {
                return false;
            }

            var handler = new JwtSecurityTokenHandler();
            var jwtToken = handler.ReadJwtToken(token);
            var userTypeClaim = jwtToken.Claims.FirstOrDefault(c => c.Type == "userType");
            if (userTypeClaim == null)
            {
                return false;
            }

            var userType = JsonConvert.DeserializeObject<TipoUtente>(userTypeClaim.Value);
            return userType == TipoUtente.Admin;
        }

        public bool IsPremiumUser(HttpRequest request)
        {
            var token = GetToken(request);
            if (token == null)
            {
                return false;
            }

            var handler = new JwtSecurityTokenHandler();
            var jwtToken = handler.ReadJwtToken(token);
            var userTypeClaim = jwtToken.Claims.FirstOrDefault(c => c.Type == "userType");
            if (userTypeClaim == null)
            {
                return false;
            }

            var userType = JsonConvert.DeserializeObject<TipoUtente>(userTypeClaim.Value);
            return userType == TipoUtente.Premium;
        }

        public string? GetQrCode(HttpRequest request)
        {
            var token = GetToken(request);
            if (token == null)
            {
                return null;
            }

            var handler = new JwtSecurityTokenHandler();
            var jwtToken = handler.ReadJwtToken(token);
            var qrCodeClaim = jwtToken.Claims.FirstOrDefault(c => c.Type == "qrCode");
            return qrCodeClaim?.Value;
        }

        public bool Logout(HttpResponse response, HttpRequest request)
        {
            if (request.Cookies.ContainsKey("Token"))
            {
                response.Cookies.Delete("Token");
                return true;
            }
            else
            {
                return false;
            }
        }
    }
}