package br.com.inventorymed.auth;

import br.com.inventorymed.config.JwtProperties;
import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.HospitalMembership;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public IssuedToken issueAccessToken(AppUser user, HospitalMembership membership) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("name", user.getFullName());

        if (membership == null) {
            claims.claim("authorities", List.of());
        } else {
            claims
                .claim("hospital_id", membership.getHospital().getId().toString())
                .claim("role", membership.getRole().name())
                .claim("authorities", List.of("ROLE_" + membership.getRole().name()));
        }

        String value = jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    claims.build()
                )
            )
            .getTokenValue();

        return new IssuedToken(
            value,
            properties.accessTokenTtl().toSeconds(),
            membership == null ? null : membership.getHospital().getId()
        );
    }

    public record IssuedToken(String value, long expiresInSeconds, UUID hospitalId) {}
}
