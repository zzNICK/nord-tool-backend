package br.com.nord_tool_backend.security;

import br.com.nord_tool_backend.service.AdminBootstrapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Dispara o bootstrap do primeiro ADMIN no startup (regras em {@link AdminBootstrapService}). */
@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private final AdminBootstrapService adminBootstrapService;
    private final String email;
    private final String nome;
    private final String senha;

    public AdminBootstrapRunner(AdminBootstrapService adminBootstrapService,
                                @Value("${nord-tool.admin.email:}") String email,
                                @Value("${nord-tool.admin.name:}") String nome,
                                @Value("${nord-tool.admin.password:}") String senha) {
        this.adminBootstrapService = adminBootstrapService;
        this.email = email;
        this.nome = nome;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        adminBootstrapService.executar(email, nome, senha);
    }
}
