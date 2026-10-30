package sn.sunuagri.partage.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class ConfigurationWeb implements WebMvcConfigurer {

    private final ResolveurUtilisateur resolveurUtilisateur;

    public ConfigurationWeb(ResolveurUtilisateur resolveurUtilisateur) {
        this.resolveurUtilisateur = resolveurUtilisateur;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolveurs) {
        resolveurs.add(resolveurUtilisateur);
    }
}
