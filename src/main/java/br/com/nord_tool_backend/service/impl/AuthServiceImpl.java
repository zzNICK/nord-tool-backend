package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.PerfilPermissao;
import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.dto.UsuarioDto;
import br.com.nord_tool_backend.exception.EntradaInvalidaException;
import br.com.nord_tool_backend.exception.NaoAutenticadoException;
import br.com.nord_tool_backend.exception.NordException;
import br.com.nord_tool_backend.form.AlterarSenhaForm;
import br.com.nord_tool_backend.form.LoginForm;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.security.Acao;
import br.com.nord_tool_backend.security.JwtService;
import br.com.nord_tool_backend.security.Modulo;
import br.com.nord_tool_backend.security.PermissaoCalculator;
import br.com.nord_tool_backend.security.TokenEmitido;
import br.com.nord_tool_backend.security.UsuarioAutenticado;
import br.com.nord_tool_backend.service.AuthService;
import br.com.nord_tool_backend.service.AutorizacaoService;
import br.com.nord_tool_backend.service.LimiteTentativasService;
import br.com.nord_tool_backend.service.SessaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    static final int MAX_FALHAS = 5;
    static final int MAX_MINUTOS_BLOQUEIO = 15;
    static final int MAX_TENTATIVAS_IP = 10;
    static final int MAX_TENTATIVAS_EMAIL = 5;
    static final String MSG_CREDENCIAIS = "E-mail ou senha inválidos";
    static final String MSG_SESSAO_ENCERRADA = "Sessão encerrada. Faça login novamente.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessaoService sessaoService;
    private final LimiteTentativasService limiteTentativas;
    private final AutorizacaoService autorizacao;
    private final Clock clock;

    // Hash descartável para igualar o tempo de resposta quando a senha não é comparada com um hash real.
    private String hashFalso;

    /**
     * Toda recusa responde a mesma mensagem e executa o BCrypt uma vez (e-mail inexistente, conta bloqueada,
     * conta inativa ou senha errada), para não revelar qual foi o motivo pela resposta nem pelo tempo.
     */
    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NordException.class)
    public LoginResponseDto login(LoginForm form, String ip) {
        String email = form.getEmail().trim();
        limiteTentativas.registrar("ip:" + ip, MAX_TENTATIVAS_IP);
        limiteTentativas.registrar("email:" + email.toLowerCase(Locale.ROOT), MAX_TENTATIVAS_EMAIL);

        Usuario usuario = PoliticaSenha.excedeLimiteBcrypt(form.getSenha())
                ? null : usuarioRepository.buscarPorEmail(email).orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(form.getSenha(), hashFalso());
            throw credenciaisInvalidas();
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        boolean senhaOk = passwordEncoder.matches(form.getSenha(), usuario.getNmSenhaHash());
        if (usuario.getDhBloqueadoAte() != null && usuario.getDhBloqueadoAte().isAfter(agora)) {
            log.info("Login recusado: conta temporariamente bloqueada ({})", usuario.getId());
            throw credenciaisInvalidas();
        }
        if (!senhaOk) {
            registrarFalha(usuario.getId(), agora);
            throw credenciaisInvalidas();
        }
        if (!Boolean.TRUE.equals(usuario.getInAtivo())) {
            log.info("Login recusado: conta inativa ({})", usuario.getId());
            throw credenciaisInvalidas();
        }

        usuarioRepository.registrarLoginSucesso(usuario.getId());
        Instant instante = clock.instant();
        return responder(usuario, instante, instante);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto refresh() {
        UsuarioAutenticado atual = autorizacao.exigirAutenticado();
        Usuario usuario = buscarAtivo(atual.getId());
        Instant agora = clock.instant();
        if (SessaoServiceImpl.versao(usuario) != atual.getVersaoSessao()
                || agora.isAfter(atual.getInicioSessao().plus(jwtService.getDuracaoMaximaSessao()))) {
            throw new NaoAutenticadoException(MSG_SESSAO_ENCERRADA);
        }
        return responder(usuario, agora, atual.getInicioSessao());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDto me() {
        Usuario usuario = buscarAtivo(autorizacao.exigirAutenticado().getId());
        return toDto(usuario, permissoes(usuario));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseDto alterarSenha(AlterarSenhaForm form) {
        UsuarioAutenticado atual = autorizacao.exigirAutenticado();
        Usuario usuario = buscarAtivo(atual.getId());
        if (!passwordEncoder.matches(form.getSenhaAtual(), usuario.getNmSenhaHash())) {
            throw new EntradaInvalidaException("Senha atual incorreta");
        }
        if (form.getNovaSenha().equals(form.getSenhaAtual())) {
            throw new EntradaInvalidaException("A nova senha deve ser diferente da atual");
        }
        PoliticaSenha.validar(form.getNovaSenha()).ifPresent(motivo -> {
            throw new EntradaInvalidaException("Senha inválida: " + motivo);
        });
        usuarioRepository.alterarSenha(usuario.getId(), passwordEncoder.encode(form.getNovaSenha()));
        sessaoService.invalidar(usuario.getId());
        // A troca incrementa a versão da sessão: os outros tokens param de valer e esta sessão recebe um novo.
        return responder(buscarAtivo(usuario.getId()), clock.instant(), atual.getInicioSessao());
    }

    /** Bloqueio progressivo a partir da 5ª falha: 1, 2, 4, 8 e no máximo 15 minutos. */
    private void registrarFalha(Long idUsuario, LocalDateTime agora) {
        int falhas = usuarioRepository.incrementarFalhas(idUsuario);
        if (falhas >= MAX_FALHAS) {
            long minutos = Math.min(MAX_MINUTOS_BLOQUEIO, 1L << Math.min(falhas - MAX_FALHAS, 4));
            usuarioRepository.bloquearAte(idUsuario, agora.plusMinutes(minutos));
        }
    }

    private Usuario buscarAtivo(Long idUsuario) {
        return usuarioRepository.buscarPorId(idUsuario)
                .filter(u -> Boolean.TRUE.equals(u.getInAtivo()))
                .orElseThrow(() -> new NaoAutenticadoException(MSG_SESSAO_ENCERRADA));
    }

    private Map<Modulo, Acao> permissoes(Usuario usuario) {
        return PermissaoCalculator.resolver(usuarioRepository.listarPermissoes(usuario.getIdPerfil()),
                usuarioRepository.listarPermissoesUsuario(usuario.getId()));
    }

    private LoginResponseDto responder(Usuario usuario, Instant agora, Instant inicioSessao) {
        Map<Modulo, Acao> permissoes = permissoes(usuario);
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getId(), usuario.getNmEmail(), usuario.getCdPerfil(),
                permissoes, SessaoServiceImpl.versao(usuario), inicioSessao);
        TokenEmitido emitido = jwtService.emitir(principal, agora);
        return new LoginResponseDto(emitido.getToken(), emitido.getExpiraEm().toString(),
                jwtService.getDuracaoToken().toMinutes(),
                inicioSessao.plus(jwtService.getDuracaoMaximaSessao()).toString(),
                toDto(usuario, permissoes));
    }

    private static UsuarioDto toDto(Usuario usuario, Map<Modulo, Acao> permissoes) {
        List<PerfilPermissao> lista = permissoes.entrySet().stream()
                .map(e -> new PerfilPermissao(e.getKey().name(), e.getValue().name()))
                .collect(Collectors.toList());
        return new UsuarioDto(usuario.getId(), usuario.getNmNome(), usuario.getNmEmail(), usuario.getCdPerfil(), lista);
    }

    private static NaoAutenticadoException credenciaisInvalidas() {
        return new NaoAutenticadoException("CREDENCIAIS_INVALIDAS", MSG_CREDENCIAIS);
    }

    private synchronized String hashFalso() {
        if (hashFalso == null) hashFalso = passwordEncoder.encode("senha-descartavel-" + System.nanoTime());
        return hashFalso;
    }
}
