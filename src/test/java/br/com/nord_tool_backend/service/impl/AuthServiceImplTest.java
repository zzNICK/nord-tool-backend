package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.dto.UsuarioDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.LimiteRequisicoesException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import br.com.nord_tool_backend.form.AlterarSenhaForm;
import br.com.nord_tool_backend.form.LoginForm;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.JwtServiceImpl;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.SecurityProperties;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.LimiteTentativasService;
import br.com.nord_tool_backend.service.SessaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private static final Instant AGORA = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    private static final Clock RELOGIO = Clock.fixed(AGORA, ZoneOffset.UTC);
    private static final LocalDateTime AGORA_LOCAL = LocalDateTime.ofInstant(AGORA, ZoneOffset.UTC);
    private static final String SENHA = "senha-correta-123";
    private static final String IP = "203.0.113.7";

    private UsuarioRepository repository;
    private BCryptPasswordEncoder encoder;
    private JwtService jwtService;
    private SessaoService sessaoService;
    private LimiteTentativasService limite;
    private AutorizacaoService autorizacao;
    private AuthServiceImpl service;
    private String hashSenha;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        encoder = spy(new BCryptPasswordEncoder(4));
        hashSenha = encoder.encode(SENHA);
        jwtService = new JwtServiceImpl(new SecurityProperties("", "", "segredo-de-teste-com-mais-de-32-bytes!!", 15, 12, false));
        sessaoService = mock(SessaoService.class);
        limite = mock(LimiteTentativasService.class);
        autorizacao = mock(AutorizacaoService.class);
        service = new AuthServiceImpl(repository, encoder, jwtService, sessaoService, limite, autorizacao, RELOGIO);
        when(repository.listarPermissoes(7L)).thenReturn(List.of(new PerfilPermissao("*", "ADMIN")));
        when(repository.listarPermissoesUsuario(1L)).thenReturn(Collections.emptyList());
    }

    private Usuario usuario(boolean ativo, LocalDateTime bloqueadoAte, int versao) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNmEmail("admin@nord.com");
        u.setNmNome("Admin");
        u.setNmSenhaHash(hashSenha);
        u.setIdPerfil(7L);
        u.setCdPerfil("ADMIN");
        u.setInAtivo(ativo);
        u.setNrFalhasLogin(0);
        u.setDhBloqueadoAte(bloqueadoAte);
        u.setNrVersaoSessao(versao);
        return u;
    }

    private static LoginForm form(String email, String senha) {
        LoginForm f = new LoginForm();
        f.setEmail(email);
        f.setSenha(senha);
        return f;
    }

    private UsuarioAutenticado sessao(int versao, Instant inicio) {
        return new UsuarioAutenticado(1L, "admin@nord.com", "ADMIN", new EnumMap<>(Modulo.class), versao, inicio);
    }

    private static Map<String, String> permissoes(UsuarioDto dto) {
        return dto.getPermissoes().stream().collect(Collectors.toMap(PerfilPermissao::getCdModulo, PerfilPermissao::getCdAcao));
    }

    // ---------- login ----------

    @Test
    void loginValidoDevolveTokenComPermissoesResolvidasEZeraAsFalhas() {
        when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(usuario(true, null, 4)));

        LoginResponseDto resposta = service.login(form(" admin@nord.com ", SENHA), IP);

        assertEquals(15, resposta.getInatividadeMinutos());
        assertEquals(AGORA.plus(Duration.ofMinutes(15)).toString(), resposta.getExpiraEm());
        assertEquals(AGORA.plus(Duration.ofHours(12)).toString(), resposta.getSessaoExpiraEm());
        assertEquals(Modulo.values().length, resposta.getUsuario().getPermissoes().size());
        UsuarioAutenticado doToken = jwtService.validar(resposta.getToken()).orElseThrow(AssertionError::new);
        assertEquals(4, doToken.getVersaoSessao());
        assertEquals(AGORA, doToken.getInicioSessao());
        assertEquals(Acao.ADMIN, doToken.acao(Modulo.FINANCEIRO));
        verify(repository).registrarLoginSucesso(1L);
    }

    @Test
    void permissaoMaisEspecificaPrevaleceENenhumNegaOModulo() {
        when(repository.listarPermissoes(7L)).thenReturn(List.of(
                new PerfilPermissao("*", "ESCRITA"), new PerfilPermissao("FINANCEIRO", "LEITURA")));
        when(repository.listarPermissoesUsuario(1L)).thenReturn(List.of(
                new PerfilPermissao("CASAMENTO", "NENHUM"), new PerfilPermissao("CAIXINHA", "ADMIN")));
        when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(usuario(true, null, 0)));

        Map<String, String> efetivas = permissoes(service.login(form("admin@nord.com", SENHA), IP).getUsuario());

        assertEquals("LEITURA", efetivas.get("FINANCEIRO"));
        assertEquals("ADMIN", efetivas.get("CAIXINHA"));
        assertEquals("ESCRITA", efetivas.get("VISTORIA"));
        assertFalse(efetivas.containsKey("CASAMENTO"));
    }

    @Test
    void curingaDoUsuarioPrevaleceSobreOCuringaDoPerfilMasNaoSobreOModuloDoPerfil() {
        when(repository.listarPermissoes(7L)).thenReturn(List.of(
                new PerfilPermissao("*", "ADMIN"), new PerfilPermissao("CRONOGRAMA", "ESCRITA")));
        when(repository.listarPermissoesUsuario(1L)).thenReturn(List.of(new PerfilPermissao("*", "LEITURA")));
        when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(usuario(true, null, 0)));

        Map<String, String> efetivas = permissoes(service.login(form("admin@nord.com", SENHA), IP).getUsuario());

        assertEquals("LEITURA", efetivas.get("FINANCEIRO"));
        assertEquals("ESCRITA", efetivas.get("CRONOGRAMA"));
    }

    @Test
    void todaRecusaTemAMesmaMensagemEExecutaOBcrypt() {
        when(repository.buscarPorEmail("naoexiste@nord.com")).thenReturn(Optional.empty());
        when(repository.buscarPorEmail("bloqueado@nord.com")).thenReturn(Optional.of(usuario(true, AGORA_LOCAL.plusMinutes(5), 0)));
        when(repository.buscarPorEmail("inativo@nord.com")).thenReturn(Optional.of(usuario(false, null, 0)));
        when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(usuario(true, null, 0)));
        when(repository.incrementarFalhas(1L)).thenReturn(1);
        clearInvocations(encoder);

        for (LoginForm tentativa : List.of(form("naoexiste@nord.com", SENHA), form("bloqueado@nord.com", SENHA),
                form("inativo@nord.com", SENHA), form("admin@nord.com", "senha-errada-999"))) {
            NaoAutenticadoException erro = assertThrows(NaoAutenticadoException.class, () -> service.login(tentativa, IP));
            assertEquals(AuthServiceImpl.MSG_CREDENCIAIS, erro.getMessage());
            assertEquals("CREDENCIAIS_INVALIDAS", erro.getCdErro());
        }
        verify(encoder, times(4)).matches(anyString(), anyString());
        verify(repository, never()).registrarLoginSucesso(anyLong());
    }

    @Test
    void senhaErradaContaFalhaDeFormaAtomicaEBloqueiaProgressivamente() {
        when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(usuario(true, null, 0)));
        LoginForm errada = form("admin@nord.com", "senha-errada-999");

        when(repository.incrementarFalhas(1L)).thenReturn(4);
        assertThrows(NaoAutenticadoException.class, () -> service.login(errada, IP));
        verify(repository, never()).bloquearAte(anyLong(), any());

        int[][] falhasEMinutos = {{5, 1}, {6, 2}, {7, 4}, {8, 8}, {9, 15}, {30, 15}};
        for (int[] caso : falhasEMinutos) {
            reset(repository);
            when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(usuario(true, null, 0)));
            when(repository.incrementarFalhas(1L)).thenReturn(caso[0]);
            assertThrows(NaoAutenticadoException.class, () -> service.login(errada, IP));
            verify(repository).bloquearAte(1L, AGORA_LOCAL.plusMinutes(caso[1]));
        }
    }

    @Test
    void contaBloqueadaOuInativaNaoContaFalha() {
        when(repository.buscarPorEmail("bloqueado@nord.com")).thenReturn(Optional.of(usuario(true, AGORA_LOCAL.plusMinutes(5), 0)));
        when(repository.buscarPorEmail("inativo@nord.com")).thenReturn(Optional.of(usuario(false, null, 0)));

        assertThrows(NaoAutenticadoException.class, () -> service.login(form("bloqueado@nord.com", "qualquer-senha-1"), IP));
        assertThrows(NaoAutenticadoException.class, () -> service.login(form("inativo@nord.com", SENHA), IP));
        verify(repository, never()).incrementarFalhas(anyLong());
    }

    @Test
    void bloqueioVencidoPermiteLogin() {
        when(repository.buscarPorEmail("admin@nord.com")).thenReturn(Optional.of(usuario(true, AGORA_LOCAL.minusSeconds(1), 0)));
        assertNotNull(service.login(form("admin@nord.com", SENHA), IP).getToken());
    }

    @Test
    void limiteDeTentativasPorIpEPorEmailBarraAntesDoBanco() {
        doThrow(new LimiteRequisicoesException("Muitas tentativas")).when(limite).registrar(eq("email:admin@nord.com"), anyInt());

        assertThrows(LimiteRequisicoesException.class, () -> service.login(form("Admin@Nord.com", SENHA), IP));

        verify(limite).registrar("ip:" + IP, AuthServiceImpl.MAX_TENTATIVAS_IP);
        verify(limite).registrar("email:admin@nord.com", AuthServiceImpl.MAX_TENTATIVAS_EMAIL);
        verifyNoInteractions(repository);
    }

    @Test
    void senhaAcimaDe72BytesEhRecusadaSemConsultarOUsuario() {
        String longa = "ç".repeat(37);
        assertThrows(NaoAutenticadoException.class, () -> service.login(form("admin@nord.com", longa), IP));
        verify(repository, never()).buscarPorEmail(anyString());
    }

    // ---------- refresh / me ----------

    @Test
    void refreshRenovaMantendoOInicioDaSessao() {
        Instant inicio = AGORA.minus(Duration.ofHours(2));
        when(autorizacao.exigirAutenticado()).thenReturn(sessao(2, inicio));
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(usuario(true, null, 2)));

        LoginResponseDto resposta = service.refresh();

        assertEquals(inicio, jwtService.validar(resposta.getToken()).orElseThrow(AssertionError::new).getInicioSessao());
        assertEquals(inicio.plus(Duration.ofHours(12)).toString(), resposta.getSessaoExpiraEm());
    }

    @Test
    void refreshRecusaVersaoAntigaLimiteAbsolutoEUsuarioInativo() {
        when(autorizacao.exigirAutenticado()).thenReturn(sessao(1, AGORA.minusSeconds(60)));
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(usuario(true, null, 2)));
        assertThrows(NaoAutenticadoException.class, () -> service.refresh());

        when(autorizacao.exigirAutenticado()).thenReturn(sessao(2, AGORA.minus(Duration.ofHours(12)).minusSeconds(1)));
        assertThrows(NaoAutenticadoException.class, () -> service.refresh());

        when(autorizacao.exigirAutenticado()).thenReturn(sessao(2, AGORA.minusSeconds(60)));
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(usuario(false, null, 2)));
        assertThrows(NaoAutenticadoException.class, () -> service.refresh());
    }

    @Test
    void semUsuarioAutenticadoRefreshEMeSaoRecusados() {
        when(autorizacao.exigirAutenticado()).thenThrow(new NaoAutenticadoException("Autenticação necessária"));
        assertThrows(NaoAutenticadoException.class, () -> service.refresh());
        assertThrows(NaoAutenticadoException.class, () -> service.me());
        verifyNoInteractions(repository);
    }

    @Test
    void meDevolveAsPermissoesEfetivas() {
        when(autorizacao.exigirAutenticado()).thenReturn(sessao(0, AGORA));
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(usuario(true, null, 0)));
        when(repository.listarPermissoesUsuario(1L)).thenReturn(List.of(new PerfilPermissao("FINANCEIRO", "NENHUM")));

        Map<String, String> efetivas = permissoes(service.me());

        assertFalse(efetivas.containsKey("FINANCEIRO"));
        assertEquals("ADMIN", efetivas.get("CASAMENTO"));
    }

    // ---------- alterar senha ----------

    private static AlterarSenhaForm troca(String atual, String nova) {
        AlterarSenhaForm f = new AlterarSenhaForm();
        f.setSenhaAtual(atual);
        f.setNovaSenha(nova);
        return f;
    }

    @Test
    void alterarSenhaGravaHashEncerraOutrasSessoesEDevolveTokenDaNovaVersao() {
        when(autorizacao.exigirAutenticado()).thenReturn(sessao(2, AGORA.minusSeconds(600)));
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(usuario(true, null, 2)), Optional.of(usuario(true, null, 3)));

        LoginResponseDto resposta = service.alterarSenha(troca(SENHA, "nova-senha-segura-1"));

        verify(repository).alterarSenha(eq(1L), argThat(hash -> encoder.matches("nova-senha-segura-1", hash)));
        verify(sessaoService).invalidar(1L);
        assertEquals(3, jwtService.validar(resposta.getToken()).orElseThrow(AssertionError::new).getVersaoSessao());
    }

    @Test
    void alterarSenhaRecusaSenhaAtualErradaRepetidaOuAcimaDe72Bytes() {
        when(autorizacao.exigirAutenticado()).thenReturn(sessao(0, AGORA));
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(usuario(true, null, 0)));

        assertThrows(EntradaInvalidaException.class, () -> service.alterarSenha(troca("errada-123456", "nova-senha-segura-1")));
        assertThrows(EntradaInvalidaException.class, () -> service.alterarSenha(troca(SENHA, SENHA)));
        assertThrows(EntradaInvalidaException.class, () -> service.alterarSenha(troca(SENHA, "ç".repeat(37))));
        verify(repository, never()).alterarSenha(anyLong(), anyString());
        verify(sessaoService, never()).invalidar(anyLong());
    }

    @Test
    void sessaoDeUsuarioNaoEncontradoEhEncerrada() {
        when(autorizacao.exigirAutenticado()).thenReturn(sessao(0, AGORA));
        when(repository.buscarPorId(1L)).thenReturn(Optional.empty());
        assertThrows(NaoAutenticadoException.class, () -> service.me());
        assertEquals(AuthServiceImpl.MSG_SESSAO_ENCERRADA,
                assertThrows(NaoAutenticadoException.class, () -> service.alterarSenha(troca(SENHA, "nova-senha-segura-1"))).getMessage());
    }
}
