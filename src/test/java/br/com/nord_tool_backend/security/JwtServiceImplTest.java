package br.com.nord_tool_backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceImplTest {

    private static final String CHAVE_A = Base64.getEncoder().encodeToString(
            "chave-a-de-teste-com-mais-de-32-bytes!!".getBytes(StandardCharsets.UTF_8));
    private static final String CHAVE_B = Base64.getEncoder().encodeToString(
            "chave-b-de-teste-com-mais-de-32-bytes!!".getBytes(StandardCharsets.UTF_8));

    private static SecurityProperties props(String chaves, String ativa) {
        return new SecurityProperties(chaves, ativa, "", 15, 12, false);
    }

    private static UsuarioAutenticado usuario(Instant inicio) {
        Map<Modulo, Acao> permissoes = new EnumMap<>(Modulo.class);
        permissoes.put(Modulo.FINANCEIRO, Acao.ESCRITA);
        permissoes.put(Modulo.CASAMENTO, Acao.LEITURA);
        return new UsuarioAutenticado(5L, "a@b.com", "ADMIN", permissoes, 3, inicio);
    }

    @Test
    void emiteEValidaTokenComPermissoesVersaoEInicioDaSessao() {
        JwtService jwt = new JwtServiceImpl(props("a:" + CHAVE_A, ""));
        Instant agora = Instant.now();
        TokenEmitido emitido = jwt.emitir(usuario(agora.minusSeconds(60)), agora);

        UsuarioAutenticado u = jwt.validar(emitido.getToken()).orElseThrow(AssertionError::new);
        assertEquals(5L, u.getId());
        assertEquals(Acao.ESCRITA, u.acao(Modulo.FINANCEIRO));
        assertEquals(Acao.LEITURA, u.acao(Modulo.CASAMENTO));
        assertEquals(Acao.NENHUM, u.acao(Modulo.CAIXINHA));
        assertEquals(3, u.getVersaoSessao());
        assertEquals(agora.minusSeconds(60).getEpochSecond(), u.getInicioSessao().getEpochSecond());
        assertEquals(Duration.ofMinutes(15), Duration.between(agora, emitido.getExpiraEm()));
    }

    @Test
    void expiracaoNuncaPassaDoLimiteAbsolutoDaSessao() {
        JwtService jwt = new JwtServiceImpl(props("a:" + CHAVE_A, ""));
        Instant inicio = Instant.now().minus(Duration.ofHours(12)).plusSeconds(120);
        TokenEmitido emitido = jwt.emitir(usuario(inicio), Instant.now());
        assertEquals(inicio.plus(Duration.ofHours(12)), emitido.getExpiraEm());
    }

    @Test
    void tokenExpiradoAdulteradoOuDeOutraChaveEhRejeitado() {
        JwtService jwt = new JwtServiceImpl(props("a:" + CHAVE_A, ""));
        Instant antigo = Instant.now().minus(Duration.ofHours(1));
        assertFalse(jwt.validar(jwt.emitir(usuario(antigo), antigo).getToken()).isPresent());

        String valido = jwt.emitir(usuario(Instant.now()), Instant.now()).getToken();
        assertFalse(jwt.validar(valido + "x").isPresent());
        assertFalse(jwt.validar("lixo").isPresent());

        JwtService outra = new JwtServiceImpl(props("a:" + CHAVE_B, ""));
        assertFalse(outra.validar(valido).isPresent());
    }

    @Test
    void rotacaoAceitaTokensDaChaveAnteriorEAssinaComAAtiva() {
        JwtService antes = new JwtServiceImpl(props("a:" + CHAVE_A, ""));
        String daChaveA = antes.emitir(usuario(Instant.now()), Instant.now()).getToken();

        JwtService depois = new JwtServiceImpl(props("a:" + CHAVE_A + ";b:" + CHAVE_B, "b"));
        assertTrue(depois.validar(daChaveA).isPresent());
        String daChaveB = depois.emitir(usuario(Instant.now()), Instant.now()).getToken();

        JwtService soB = new JwtServiceImpl(props("b:" + CHAVE_B, ""));
        assertTrue(soB.validar(daChaveB).isPresent());
        assertFalse(soB.validar(daChaveA).isPresent());
    }

    @Test
    void tokenSemEmissorPublicoOuTipoCorretoEhRejeitado() {
        JwtService jwt = new JwtServiceImpl(props("a:" + CHAVE_A, ""));
        String semPublico = Jwts.builder()
                .setHeaderParam("kid", "a").setHeaderParam("typ", "at+jwt")
                .setIssuer("nord-tool-backend").setSubject("5")
                .claim("sv", 0).claim("auth_time", Instant.now().getEpochSecond())
                .setExpiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(CHAVE_A)), SignatureAlgorithm.HS256)
                .compact();
        assertFalse(jwt.validar(semPublico).isPresent());
    }

    @Test
    void configuracaoDeChaveInvalidaFalhaNaSubida() {
        assertThrows(IllegalStateException.class, () -> new JwtServiceImpl(props("", "")));
        assertThrows(IllegalStateException.class, () -> new JwtServiceImpl(props("a:" + Base64.getEncoder().encodeToString(new byte[16]), "")));
        assertThrows(IllegalStateException.class, () -> new JwtServiceImpl(props("a:" + CHAVE_A + ";b:" + CHAVE_B, "")));
        assertThrows(IllegalStateException.class, () -> new JwtServiceImpl(props("a:" + CHAVE_A, "x")));
        assertThrows(IllegalStateException.class, () -> new JwtServiceImpl(props("semdoispontos", "")));
        assertThrows(IllegalStateException.class, () -> new JwtServiceImpl(props("a:" + CHAVE_A + ";a:" + CHAVE_B, "a")));
        assertThrows(IllegalStateException.class, () -> new JwtServiceImpl(
                new SecurityProperties("", "", "curto", 15, 12, false)));
    }

    @Test
    void chaveTemporariaSoQuandoPermitidaESegredoLegadoContinuaAceito() {
        assertDoesNotThrow(() -> new JwtServiceImpl(new SecurityProperties("", "", "", 15, 12, true)));
        JwtService legado = new JwtServiceImpl(new SecurityProperties("", "", "segredo-legado-com-mais-de-32-bytes!!", 15, 12, false));
        assertTrue(legado.validar(legado.emitir(usuario(Instant.now()), Instant.now()).getToken()).isPresent());
    }
}
