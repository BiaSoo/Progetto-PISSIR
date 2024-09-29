using System.IdentityModel.Tokens.Jwt;

namespace GestioneParcheggio.Services
{
    public class TokenValidator
    {
        public bool ValidateToken(string token)
        {
            try
            {
                // Decodifica il token senza validarne la firma
                var handler = new JwtSecurityTokenHandler();

                if (handler.ReadToken(token) is not JwtSecurityToken jwtToken)
                {
                    return false;
                }

                // Estrae la data di scadenza (exp)
                var exp = jwtToken.ValidTo;

                // Verifica se il token è scaduto
                if (exp < DateTime.UtcNow)
                {
                    return false;
                }

                // Token è valido (non scaduto)
                return true;
            }
            catch (Exception)
            {
                return false;
            }
        }
    }
}



