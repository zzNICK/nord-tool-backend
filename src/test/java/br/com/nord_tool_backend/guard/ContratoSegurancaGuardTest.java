package br.com.nord_tool_backend.guard;

import br.com.nord_tool_backend.config.CorrelacaoFilter;
import br.com.nord_tool_backend.controller.read.AuthReadController;
import br.com.nord_tool_backend.controller.read.HealthReadController;
import br.com.nord_tool_backend.controller.write.AuthWriteController;
import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.dto.UsuarioDto;
import br.com.nord_tool_backend.handler.GlobalExceptionHandler;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.JwtAuthenticationFilter;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.JwtServiceImpl;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.SecurityConfig;
import br.com.nord_tool_backend.security.SecurityContextUsuarioAtualProvider;
import br.com.nord_tool_backend.security.SecurityProperties;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AuthService;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.SessaoService;
import br.com.nord_tool_backend.service.impl.AutorizacaoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guarda de segurança (exceção prevista em steering/nord-tool-backend/testing.md): contrato HTTP de
 * autenticação e autorização — 401 sem token, com token inválido/expirado/de outra chave ou com sessão revogada,
 * 403 sem permissão do módulo, rotas públicas, idCorrelacao, cabeçalhos de segurança e corpo de erro sem
 * detalhes internos. Regras de negócio ficam nos *ServiceImplTest.
 */
@WebMvcTest(controllers = {ContratoSegurancaGuardTest.RotaProtegida.class, HealthReadController.class,
        AuthReadController.class, AuthWriteController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtServiceImpl.class, SecurityProperties.class,
        GlobalExceptionHandler.class, CorrelacaoFilter.class, AutorizacaoServiceImpl.class,
        SecurityContextUsuarioAtualProvider.class, ContratoSegurancaGuardTest.RotaProtegida.class})
@TestPropertySource(properties = "nord-tool.security.jwt-secret=" + ContratoSegurancaGuardTest.SEGREDO)
class ContratoSegurancaGuardTest {

    static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes!!";
    private static final String ROTA_PROTEGIDA = "/api/v1/nord-tool/apartamentoVistoria";
    private static final String ROTA_FINANCEIRO = "/api/v1/nord-tool/financeiro/guarda";

    /** Rotas de teste: uma só autenticada e outra que exige FINANCEIRO:LEITURA no serviço. */
    @RestController
    static class RotaProtegida {
        @Autowired AutorizacaoService autorizacao;

        @GetMapping(ROTA_PROTEGIDA)
        String listar() { return "ok"; }

        @GetMapping(ROTA_FINANCEIRO)
        String financeiro() {
            autorizacao.exigir(Modulo.FINANCEIRO, Acao.LEITURA);
            return "ok";
        }
    }

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean AuthService authService;
    @MockBean SessaoService sessaoService;

    @BeforeEach
    void sessaoValidaPorPadrao() {
        when(sessaoService.valida(any())).thenReturn(true);
    }

    private String token(Map<Modulo, Acao> permissoes, Instant agora) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(1L, "a@b.com", "ADMIN", permissoes, 0, agora);
        return jwt.emitir(usuario, agora).getToken();
    }

    private String tokenAdmin() {
        Map<Modulo, Acao> todas = new EnumMap<>(Modulo.class);
        for (Modulo m : Modulo.values()) todas.put(m, Acao.ADMIN);
        return token(todas, Instant.now());
    }

    @Test
    void rotaProtegidaSemTokenRetorna401ComCorpoPadrao() throws Exception {
        semDetalhesInternos(mvc.perform(get(ROTA_PROTEGIDA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.nrStatus").value(401))
                .andExpect(jsonPath("$.cdErro").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.txMensagem").exists());
    }

    @Test
    void rotaProtegidaComTokenRetorna200() throws Exception {
        mvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    void tokenInvalidoOuExpiradoRetorna401() throws Exception {
        semDetalhesInternos(mvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "Bearer lixo")))
                .andExpect(status().isUnauthorized());
        String expirado = token(new EnumMap<>(Modulo.class), Instant.now().minus(Duration.ofHours(2)));
        mvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAssinadoComOutraChaveRetorna401() throws Exception {
        JwtService outraChave = new JwtServiceImpl(
                new SecurityProperties("", "", "outra-chave-de-teste-com-mais-de-32-bytes", 15, 12, false));
        UsuarioAutenticado usuario = new UsuarioAutenticado(1L, "a@b.com", "ADMIN", new EnumMap<>(Modulo.class), 0, Instant.now());
        String forjado = outraChave.emitir(usuario, Instant.now()).getToken();
        mvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "Bearer " + forjado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessaoRevogadaRetorna401MesmoComTokenValido() throws Exception {
        when(sessaoService.valida(any())).thenReturn(false);
        mvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void semPermissaoDoModuloRetorna403ComCorpoPadrao() throws Exception {
        Map<Modulo, Acao> soCasamento = new EnumMap<>(Modulo.class);
        soCasamento.put(Modulo.CASAMENTO, Acao.ESCRITA);
        semDetalhesInternos(mvc.perform(get(ROTA_FINANCEIRO)
                .header("Authorization", "Bearer " + token(soCasamento, Instant.now()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.nrStatus").value(403))
                .andExpect(jsonPath("$.cdErro").value("ACESSO_NEGADO"));
        mvc.perform(get(ROTA_FINANCEIRO).header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    void erroDevolveOIdDeCorrelacaoRecebido() throws Exception {
        mvc.perform(get(ROTA_PROTEGIDA).header(CorrelacaoFilter.HEADER, "guarda-correlacao-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(CorrelacaoFilter.HEADER, "guarda-correlacao-123"))
                .andExpect(jsonPath("$.idCorrelacao").value("guarda-correlacao-123"));
    }

    @Test
    void idDeCorrelacaoInseguroEhSubstituido() throws Exception {
        mvc.perform(get(ROTA_PROTEGIDA).header(CorrelacaoFilter.HEADER, "<script>"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(CorrelacaoFilter.HEADER, not(containsString("<"))))
                .andExpect(jsonPath("$.idCorrelacao").isNotEmpty());
    }

    @Test
    void loginEHealthSaoPublicos() throws Exception {
        when(authService.login(any(), anyString())).thenReturn(new LoginResponseDto("tok", "x", 15, "y", new UsuarioDto()));
        mvc.perform(post("/api/v1/nord-tool/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"senha\":\"x\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/nord-tool/health")).andExpect(status().isOk());
    }

    @Test
    void meERefreshExigemToken() throws Exception {
        mvc.perform(get("/api/v1/nord-tool/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/nord-tool/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void preflightOptionsEhLiberado() throws Exception {
        mvc.perform(options(ROTA_PROTEGIDA)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void respostasTrazemCabecalhosDeSeguranca() throws Exception {
        mvc.perform(get(ROTA_PROTEGIDA).header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    private ResultActions semDetalhesInternos(ResultActions resultado) throws Exception {
        return resultado
                .andExpect(jsonPath("$.body").doesNotExist())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at br.com"))));
    }
}
