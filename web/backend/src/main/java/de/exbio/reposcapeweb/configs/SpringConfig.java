package de.exbio.reposcapeweb.configs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.sql.DataSource;


/**
 * A configuration class for the backend server.
 * Asynchronous and scheduled execution is activated by @EnableAsync and @EnableScheduling
 *
 * @author Andreas Maier
 */
@Configuration
@EnableAsync
@EnableScheduling
public class SpringConfig {


//    /**
//     * A Bean for the ObjectMapper instance, but currently no need for use.
//     *
//     * @return Altered {@link ObjectMapper}
//     */
//    @Bean
//    public ObjectMapper getObjectMapper() {
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
//        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
//        return objectMapper;
//    }
//


//    /**
//     * Bean for a explicit use of UTF-8 encoding but currently not in use.
//     *
//     * @return A {@link CharacterEncodingFilter} with forced UTF-8 encoding filtering properties.
//     */
//    @Bean
//    CharacterEncodingFilter characterEncodingFilter() {
//        CharacterEncodingFilter filter = new CharacterEncodingFilter();
//        filter.setEncoding("UTF-8");
//        filter.setForceEncoding(true);
//        return filter;
//    }


//    /**
//     * A Bean for manual initialization of the datasource. The idea was to explicitly execute startup scripts for view generation.
//     *
//     * @param dataSource The {@link DataSource} of the Spring environment.
//     * @return {@link DataSourceInitializer} with additional initialization steps.
//     */
//    @Bean
//    public DataSourceInitializer dataSourceInitializer(@Qualifier("dataSource") final DataSource dataSource) {
//        ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
//        resourceDatabasePopulator.addScript(new ClassPathResource("/schema.sql"));
//        resourceDatabasePopulator.addScript(new ClassPathResource("/import.sql"));
//        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
//        dataSourceInitializer.setDataSource(dataSource);
//        dataSourceInitializer.setDatabasePopulator(resourceDatabasePopulator);
//        return dataSourceInitializer;
//    }

}