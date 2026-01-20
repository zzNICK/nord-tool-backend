package br.com.nord_tool_backend.utils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class StringUtils {

    public static String getMensagem(final String chaveMensagem, final Object... params) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.getDefault());
        return recuperarTexto(bundle, chaveMensagem, params);
    }

    private static String recuperarTexto(final ResourceBundle bundle, final String chaveMensagem, final Object params) {
        String mensagem = "";

        try {
            mensagem = bundle.getString(chaveMensagem);
        } catch (MissingResourceException var5) {
            return chaveMensagem;
        }

        return (new MessageFormat(mensagem)).format(params);
    }
}
