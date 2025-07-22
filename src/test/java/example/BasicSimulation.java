package example;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class BasicSimulation extends Simulation {

    // Feeder para cargar datos dinámicos desde un archivo CSV
    private static final FeederBuilder<String> csvFeeder = csv("data.csv").circular();

    // Configuración del protocolo HTTP y headers para el servicio SOAP
    HttpProtocolBuilder httpProtocol = http.baseUrl("http://www.dneonline.com")
            .header("Content-Type", "text/xml; charset=utf-8")
            .header("SOAPAction", "http://tempuri.org/Add");

    // Escenario principal
    ScenarioBuilder scenario = scenario("SOAP Add Dynamic")
            .feed(csvFeeder)
            .exec(
                    http("Add Operation")
                            .post("/calculator.asmx")
                            .header("Content-Type", "text/xml;charset=UTF-8")
                            .header("SOAPAction", "http://tempuri.org/Add")
                            .body(StringBody(session -> {
                                String intA = session.getString("intA");
                                String intB = session.getString("intB");
                                String body = """
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:tem="http://tempuri.org/">
                                            <soapenv:Header/>
                                            <soapenv:Body>
                                                <tem:Add>
                                                    <tem:intA>%s</tem:intA>
                                                    <tem:intB>%s</tem:intB>
                                                </tem:Add>
                                            </soapenv:Body>
                                        </soapenv:Envelope>
                                        """.formatted(intA, intB);
                                System.out.println("SOAP Body enviado:\n" + body);
                                return body;
                            }))
                            .check(status().is(200))
                            .check(xpath("//*:AddResult").saveAs("result"))
            )
            .exec(session -> {
                String result = session.getString("result");
                try {
                    java.nio.file.Files.write(
                            java.nio.file.Paths.get("target/results.txt"),
                            (result + System.lineSeparator()).getBytes(),
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.APPEND
                    );
                    System.out.println("Resultado guardado: " + result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return session;
            });

    {
        setUp(
                scenario.injectOpen(
                        rampUsers(2).during(10),
                         rampUsers(4).during(10),
                         rampUsers(6).during(10),
                         rampUsers(8).during(10),
                         rampUsers(10).during(10)
                )
        ).protocols(httpProtocol);
    }
}