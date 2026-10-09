package br.com.nord_tool_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service @Slf4j
public class JwtServiceImpl implements JwtService {

    static final int MIN_SECRET_BYTES = 32;
    static final String EMISSOR = "nord-tool-backend";
    static final String PUBLICO = "nord-tool-frontend";
    static final String TIPO = "at+jwt";
    static final String KID_COMPATIBILIDADE = "padrao";
    static final String KID_EFEMERO = "efemero";

    private final Map<String, SecretKey> chaves;
    private final String kidAtivo;
    private final Duration duracaoToken;
    private final Duration duracaoMaximaSessao;

    public JwtServiceImpl(SecurityProperties props) {
        this.duracaoToken = Duration.ofMinutes(props.getAccessTokenMinutes());
        this.duracaoMaximaSessao = Duration.ofHours(props.getSessionMaxHours());
        if (duracaoToken.isNegative() || duracaoToken.isZero() || duracaoMaximaSessao.compareTo(duracaoToken) < 0) {
            throw new IllegalStateException("Duração do token/sessão inválida (NORD_ACCESS_TOKEN_MINUTES / NORD_SESSION_MAX_HOURS)");
        }
        Map<String, SecretKey> lidas = lerChaves(props);
        if (lidas.isEmpty()) {
            if (!props.isAllowEphemeralSecret()) {
                throw new IllegalStateException("Defina NORD_JWT_KEYS (ou NORD_JWT_SECRET): chave JWT obrigatória");
            }
            log.warn("Sem chave JWT configurada: usando chave temporária (só no perfil local; tokens deixam de valer ao reiniciar).");
            byte[] aleatoria = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(aleatoria);
            lidas.put(KID_EFEMERO, Keys.hmacShaKeyFor(aleatoria));
        }
        this.chaves = Collections.unmodifiableMap(lidas);
        this.kidAtivo = escolherAtiva(props.getActiveKid(), lidas);
    }

    private static Map<String, SecretKey> lerChaves(SecurityProperties props) {
        Map<String, SecretKey> lidas = new LinkedHashMap<>();
        String lista = props.getJwtKeys();
        if (lista != null && !lista.trim().isEmpty()) {
            for (String item : lista.split(";")) {
                if (item.trim().isEmpty()) continue;
                int separador = item.indexOf(':');
                if (separador <= 0) {
                    throw new IllegalStateException("NORD_JWT_KEYS: use o formato kid:base64;kid2:base64");
                }
                String kid = item.substring(0, separador).trim();
                byte[] bytes;
                try {
                    bytes = Base64.getDecoder().decode(item.substring(separador + 1).trim());
                } catch (IllegalArgumentException ex) {
                    throw new IllegalStateException("NORD_JWT_KEYS: a chave " + kid + " não é base64 válido");
                }
                if (bytes.length < MIN_SECRET_BYTES) {
                    throw new IllegalStateException("NORD_JWT_KEYS: a chave " + kid + " deve ter ao menos 32 bytes");
                }
                if (lidas.put(kid, Keys.hmacShaKeyFor(bytes)) != null) {
                    throw new IllegalStateException("NORD_JWT_KEYS: kid repetido " + kid);
                }
            }
        }
        String segredo = props.getJwtSecret();
        if (lidas.isEmpty() && segredo != null && !segredo.trim().isEmpty()) {
            byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < MIN_SECRET_BYTES) {
                throw new IllegalStateException("NORD_JWT_SECRET deve ter ao menos 32 bytes");
            }
            lidas.put(KID_COMPATIBILIDADE, Keys.hmacShaKeyFor(bytes));
        }
        return lidas;
    }

    private static String escolherAtiva(String configurada, Map<String, SecretKey> chaves) {
        if (configurada != null && !configurada.trim().isEmpty()) {
            if (!chaves.containsKey(configurada.trim())) {
                throw new IllegalStateException("NORD_JWT_ACTIVE_KID não corresponde a nenhuma chave de NORD_JWT_KEYS");
            }
            return configurada.trim();
        }
        if (chaves.size() > 1) {
            throw new IllegalStateException("Com mais de uma chave em NORD_JWT_KEYS, defina NORD_JWT_ACTIVE_KID");
        }
        return chaves.keySet().iterator().next();
    }

    @Override
    public Duration getDuracaoToken() {
        return duracaoToken;
    }

    @Override
    public Duration getDuracaoMaximaSessao() {
        return duracaoMaximaSessao;
    }

    @Override
    public TokenEmitido emitir(UsuarioAutenticado usuario, Instant agora) {
        Instant fimSessao = usuario.getInicioSessao().plus(duracaoMaximaSessao);
        Instant expira = agora.plus(duracaoToken);
        if (expira.isAfter(fimSessao)) expira = fimSessao;
        List<String> permissoes = usuario.getPermissoes().entrySet().stream()
                .map(e -> e.getKey().name() + ":" + e.getValue().name())
                .collect(Collectors.toList());
        String token = Jwts.builder()
                .setHeaderParam("kid", kidAtivo)
                .setHeaderParam("typ", TIPO)
                .setIssuer(EMISSOR)
                .setAudience(PUBLICO)
                .setSubject(String.valueOf(usuario.getId()))
                .setId(UUID.randomUUID().toString())
                .claim("email", usuario.getEmail())
                .claim("perfil", usuario.getPerfil())
                .claim("permissoes", permissoes)
                .claim("sv", usuario.getVersaoSessao())
                .claim("auth_time", usuario.getInicioSessao().getEpochSecond())
                .setIssuedAt(Date.from(agora))
                .setExpiration(Date.from(expira))
                .signWith(chaves.get(kidAtivo), SignatureAlgorithm.HS256)
                .compact();
        return new TokenEmitido(token, expira);
    }

    @Override
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                        @Override
                        public Key resolveSigningKey(JwsHeader header, Claims claims) {
                            SecretKey chave = header.getKeyId() == null ? null : chaves.get(header.getKeyId());
                            if (chave == null) throw new SignatureException("kid desconhecido");
                            return chave;
                        }
                    })
                    .requireIssuer(EMISSOR)
                    .requireAudience(PUBLICO)
                    .build()
                    .parseClaimsJws(token);
            if (!TIPO.equals(jws.getHeader().getType())) return Optional.empty();
            Claims claims = jws.getBody();
            Number inicio = claims.get("auth_time", Number.class);
            Number versao = claims.get("sv", Number.class);
            if (inicio == null || versao == null || claims.getSubject() == null) return Optional.empty();
            return Optional.of(new UsuarioAutenticado(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get("perfil", String.class),
                    permissoes(claims.get("permissoes", List.class)),
                    versao.intValue(),
                    Instant.ofEpochSecond(inicio.longValue())));
        } catch (JwtException | IllegalArgumentException | ClassCastException ex) {
            return Optional.empty();
        }
    }

    private static Map<Modulo, Acao> permissoes(List<?> lista) {
        Map<Modulo, Acao> mapa = new EnumMap<>(Modulo.class);
        if (lista == null) return mapa;
        for (Object item : lista) {
            String texto = String.valueOf(item);
            int separador = texto.indexOf(':');
            if (separador < 0) continue;
            Optional<Modulo> modulo = Modulo.de(texto.substring(0, separador));
            Acao acao = Acao.de(texto.substring(separador + 1));
            if (modulo.isPresent() && acao != Acao.NENHUM) mapa.put(modulo.get(), acao);
        }
        return mapa;
    }
}
