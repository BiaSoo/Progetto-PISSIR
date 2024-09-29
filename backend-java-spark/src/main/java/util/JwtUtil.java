package util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;

public class JwtUtil {

    private static final String SECRET_KEY = "my_secret_key_1234567890";
    // Metodo per validare il token JWT e restituire i claims
    public static Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                                .setSigningKey(SECRET_KEY.getBytes(StandardCharsets.UTF_8))
                                .parseClaimsJws(token)
                                .getBody();
            return claims;
        } catch (SignatureException e) {
            throw new RuntimeException("Invalid JWT signature");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new RuntimeException("Expired JWT token");
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token");
        }
    }

    public static void handleMqttRequest(JsonObject payload) {
        // Estrai il token dal payload
        String token = payload.get("token").getAsString();

        // Verifica il token
        Claims claims = validateToken(token);

        // Se il token è valido, procedi con l'elaborazione della richiesta
        if (claims != null) {
            // Continua con la gestione della richiesta
            System.out.println("Token valido. Procedere con la richiesta.");
        } else {
            throw new RuntimeException("Token non valido");
        }
    }
}
