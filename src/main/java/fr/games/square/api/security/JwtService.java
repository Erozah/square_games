package fr.games.square.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final PublicKey publicKey;

    public JwtService(@Value("${jwt.rsa.public-key}") String publicKeyStr) {
        this.publicKey = parsePublicKey(publicKeyStr);
        log.info("Initialized JwtService with RSA public key for token verification");
    }

    private static PublicKey parsePublicKey(String keyStr) {
        if (keyStr == null || keyStr.isBlank()) {
            throw new IllegalArgumentException("RSA public key must not be blank");
        }
        try {
            String clean = keyStr.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] bytes = Base64.getDecoder().decode(clean);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA public key", e);
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            return !extractAllClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        try {
            String userIdStr = extractAllClaims(token).get("userId", String.class);
            return userIdStr != null ? UUID.fromString(userIdStr) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String extractUsername(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public String extractRoles(String token) {
        try {
            return extractAllClaims(token).get("roles", String.class);
        } catch (Exception e) {
            return null;
        }
    }
}
