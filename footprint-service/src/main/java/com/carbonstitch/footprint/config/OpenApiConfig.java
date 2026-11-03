package com.carbonstitch.footprint.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Carbonstitch Garment Carbon Footprint API")
                .version("1.0.0")
                .description(
                    "Production microservices backend estimating carbon footprint (kg CO2e) of garments "
                        + "from materials, weight, manufacturing country, and transport mode to identify high-emission supply chain hotspots.")
                .contact(
                    new Contact()
                        .name("Carbonstitch Engineering")
                        .url("https://github.com/meowski007/carbonstitch"))
                .license(
                    new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0")))
        .servers(List.of(new Server().url("/").description("Default Server")));
  }
}
