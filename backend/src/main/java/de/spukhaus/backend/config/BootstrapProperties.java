package de.spukhaus.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spukhaus.bootstrap")
public class BootstrapProperties {

    private String techAdminUsername = "techadmin";
    private String techAdminPassword;
    private String techAdminDisplayName = "Tech Admin";

    public String getTechAdminUsername() {
        return techAdminUsername;
    }

    public void setTechAdminUsername(String techAdminUsername) {
        this.techAdminUsername = techAdminUsername;
    }

    public String getTechAdminPassword() {
        return techAdminPassword;
    }

    public void setTechAdminPassword(String techAdminPassword) {
        this.techAdminPassword = techAdminPassword;
    }

    public String getTechAdminDisplayName() {
        return techAdminDisplayName;
    }

    public void setTechAdminDisplayName(String techAdminDisplayName) {
        this.techAdminDisplayName = techAdminDisplayName;
    }
}
