package br.com.nord_tool_backend.service;

import br.com.nord_tool_backend.dto.LoginResponseDto;
import br.com.nord_tool_backend.dto.UsuarioDto;
import br.com.nord_tool_backend.form.AlterarSenhaForm;
import br.com.nord_tool_backend.form.LoginForm;

public interface AuthService {

    /** Autentica por e-mail e senha; {@code ip} alimenta o limite de tentativas por origem. */
    LoginResponseDto login(LoginForm form, String ip);

    /** Renova o token do usuário atual, respeitando versão da sessão e limite absoluto desde o login. */
    LoginResponseDto refresh();

    UsuarioDto me();

    /** Troca a senha, encerra as demais sessões e devolve um token novo para a sessão atual. */
    LoginResponseDto alterarSenha(AlterarSenhaForm form);
}
