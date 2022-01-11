package az.kapitalbank.ms.template.config;

import com.fasterxml.classmate.TypeResolver;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@Import(BeanValidatorPluginsConfiguration.class)
public class SwaggerConfig {

    @Bean
    public Docket docket(TypeResolver typeResolver) {

        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .paths(PathSelectors.regex(".*"))
                .apis(RequestHandlerSelectors.basePackage("az.kapitalbank.ms.template.controller"))
                .build()
                .produces(Collections.singleton(MediaType.APPLICATION_JSON_VALUE))
                .consumes(Collections.singleton(MediaType.APPLICATION_JSON_VALUE))
                .globalOperationParameters(Collections.singletonList(getAcceptLanguageHeader()));
    }

    private Parameter getAcceptLanguageHeader() {
        return new ParameterBuilder()
                .name("Accept-Language")
                .description("Allowed accept languages are following [az, en, ru]")
                .parameterType("header")
                .modelRef(new ModelRef("string"))
                .defaultValue("en")
                .required(false)
                .build();
    }
}
