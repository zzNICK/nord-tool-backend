package br.com.nord_tool_backend.guard;

import br.com.nord_tool_backend.handler.GlobalExceptionHandler;
import br.com.nord_tool_backend.security.JwtAuthenticationFilter;
import br.com.nord_tool_backend.security.JwtServiceImpl;
import br.com.nord_tool_backend.security.SecurityConfig;
import br.com.nord_tool_backend.security.SecurityProperties;
import br.com.nord_tool_backend.service.SessaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Varredura de segurança (E24): lê por reflexão TODAS as rotas dos controllers do projeto (inclusive
 * arquivos, fotos e PDFs) e exige 401 sem token. Só login e health podem ser públicos.
 * Se alguém criar uma rota nova, ela entra na varredura automaticamente.
 */
@WebMvcTest(controllers = RotasProtegidasGuardTest.Rota.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtServiceImpl.class, SecurityProperties.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "nord-tool.security.jwt-secret=segredo-de-teste-com-mais-de-32-bytes!!")
class RotasProtegidasGuardTest {

    @RestController
    static class Rota {
        @GetMapping("/api/v1/nord-tool/qualquer")
        String ok() { return "ok"; }
    }

    private static final Set<String> PUBLICAS = new TreeSet<>(Arrays.asList(
            "POST /api/v1/nord-tool/auth/login", "GET /nord-tool/health"));

    @Autowired MockMvc mvc;
    @MockBean SessaoService sessaoService;

    @Test
    void todasAsRotasDosControllersExigemToken() throws Exception {
        List<String> rotas = rotasDosControllers();
        assertTrue(rotas.size() > 80, "a varredura deveria achar dezenas de rotas, achou " + rotas.size());

        List<String> vazadas = new ArrayList<>();
        for (String rota : rotas) {
            String[] partes = rota.split(" ", 2);
            int status = mvc.perform(MockMvcRequestBuilders.request(HttpMethod.valueOf(partes[0]), partes[1]))
                    .andReturn().getResponse().getStatus();
            boolean publica = PUBLICAS.contains(rota);
            if (!publica && status != 401) vazadas.add(rota + " -> " + status);
        }
        assertEquals(new ArrayList<String>(), vazadas, "rotas que responderam sem token");
    }

    @Test
    void asRotasPublicasSaoSoLoginEHealth() {
        List<String> rotas = rotasDosControllers();
        for (String publica : PUBLICAS) {
            assertTrue(rotas.contains(publica), "rota pública inexistente nos controllers: " + publica);
        }
        assertFalse(rotas.isEmpty());
    }

    /** "METODO /caminho" para cada método mapeado de cada @RestController (variáveis {x} viram 1). */
    private static List<String> rotasDosControllers() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> rotas = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("br.com.nord_tool_backend.controller")) {
            Class<?> tipo;
            try {
                tipo = Class.forName(bd.getBeanClassName());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
            RequestMapping base = AnnotatedElementUtils.findMergedAnnotation(tipo, RequestMapping.class);
            String prefixo = base != null && base.path().length > 0 ? base.path()[0] : "";
            for (Method metodo : tipo.getDeclaredMethods()) {
                RequestMapping mapa = AnnotatedElementUtils.findMergedAnnotation(metodo, RequestMapping.class);
                if (mapa == null) continue;
                String caminho = mapa.path().length > 0 ? mapa.path()[0] : "";
                RequestMethod[] verbos = mapa.method().length > 0 ? mapa.method() : new RequestMethod[]{RequestMethod.GET};
                for (RequestMethod verbo : verbos) {
                    rotas.add(verbo.name() + " " + (prefixo + caminho).replaceAll("\\{[^}/]+}", "1"));
                }
            }
        }
        return rotas;
    }
}
