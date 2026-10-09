package br.com.nord_tool_backend.service.impl;

import br.com.nord_tool_backend.domain.Usuario;
import br.com.nord_tool_backend.repository.UsuarioRepository;
import br.com.nord_tool_backend.service.AdminBootstrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBootstrapServiceImpl implements AdminBootstrapService {

    static final String PERFIL_ADMIN = "ADMIN";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executar(String email, String nome, String senha) {
        if (vazio(email) && vazio(nome) && vazio(senha)) {
            return false;
        }
        if (vazio(email) || vazio(nome) || vazio(senha)) {
            throw new IllegalStateException("Defina NORD_ADMIN_EMAIL, NORD_ADMIN_NAME e NORD_ADMIN_PASSWORD juntas (ou nenhuma)");
        }
        if (!EMAIL.matcher(email.trim()).matches() || email.trim().length() > 150) {
            throw new IllegalStateException("NORD_ADMIN_EMAIL inválido");
        }
        PoliticaSenha.validar(senha).ifPresent(motivo -> {
            throw new IllegalStateException("NORD_ADMIN_PASSWORD: " + motivo);
        });
        if (usuarioRepository.contar() > 0) {
            log.info("Bootstrap do ADMIN ignorado: já existem usuários. Remova as variáveis NORD_ADMIN_*.");
            return false;
        }
        Long idPerfil = usuarioRepository.buscarIdPerfil(PERFIL_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Perfil ADMIN não existe: aplique os scripts de nord-tool-scripts-sql antes do bootstrap"));
        Usuario admin = new Usuario();
        admin.setNmEmail(email.trim());
        admin.setNmNome(nome.trim());
        admin.setNmSenhaHash(passwordEncoder.encode(senha));
        admin.setIdPerfil(idPerfil);
        usuarioRepository.inserir(admin);
        log.info("Usuário ADMIN inicial criado. Remova as variáveis NORD_ADMIN_*.");
        return true;
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
