package com.mini.credit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mini.credit.controller.audit.AuditController;
import com.mini.credit.dto.audit.CaisseHistoryDiagnosticReportDTO;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.repository.audit.AuditLogRepository;
import com.mini.credit.service.audit.AuditAccessService;
import com.mini.credit.service.audit.CaisseHistoryDiagnosticService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(AuditControllerCaisseHistoryLocalDbSmokeTest.LocalDbDiagnosticConfig.class)
class AuditControllerCaisseHistoryLocalDbSmokeTest {

    @Autowired
    private CaisseHistoryDiagnosticService caisseHistoryDiagnosticService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Test
    void exportDiagnosticReportFromLocalDatabase() throws Exception {
        Utilisateur admin = utilisateurRepository.findByUsername("admin")
                .orElseThrow(() -> new IllegalStateException("Utilisateur admin introuvable dans la base locale"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );

        try {
            AuditController controller = new AuditController(
                    mock(AuditLogRepository.class),
                    mock(AuditAccessService.class),
                    caisseHistoryDiagnosticService
            );

            var response = controller.getCaisseHistoryDiagnostic();
            assertThat(response.getStatusCode().value()).isEqualTo(200);

            CaisseHistoryDiagnosticReportDTO report = response.getBody();
            assertThat(report).isNotNull();
            assertThat(report.getAnomalies()).isNotNull();
            assertThat(report.getAnomalyCount()).isEqualTo(report.getAnomalies().size());
            if (report.getAnomaliesByType() != null) {
                long totalByType = report.getAnomaliesByType().values().stream().mapToLong(Long::longValue).sum();
                assertThat(totalByType).isEqualTo(report.getAnomalyCount());
            }

            Path output = Path.of("target", "p35-caisse-history-diagnostic-localdb.json");
            Files.createDirectories(output.getParent());
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            Files.writeString(output, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackages = "com.mini.credit.repository")
    static class LocalDbDiagnosticConfig {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl("jdbc:mysql://localhost:3306/microcredit_db?useSSL=false&serverTimezone=UTC");
            dataSource.setUsername("root");
            dataSource.setPassword("");
            return dataSource;
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setGenerateDdl(false);
            vendorAdapter.setShowSql(false);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.mini.credit.entity");
            factory.setJpaVendorAdapter(vendorAdapter);

            Properties properties = new Properties();
            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
            properties.setProperty("hibernate.hbm2ddl.auto", "none");
            properties.setProperty("hibernate.show_sql", "false");
            properties.setProperty("hibernate.format_sql", "false");
                properties.setProperty(
                    "hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
                );
            factory.setJpaProperties(properties);
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }

        @Bean
        PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
            return new PersistenceExceptionTranslationPostProcessor();
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        CaisseHistoryDiagnosticService caisseHistoryDiagnosticService(
                com.mini.credit.repository.caisse.SessionCaisseRepository sessionCaisseRepository,
                com.mini.credit.repository.caisse.CaisseRepository caisseRepository,
                com.mini.credit.repository.caisse.OperationCaisseRepository operationCaisseRepository,
                JdbcTemplate jdbcTemplate
        ) {
            return new CaisseHistoryDiagnosticService(
                    sessionCaisseRepository,
                    caisseRepository,
                    operationCaisseRepository,
                    jdbcTemplate
            );
        }
    }
}