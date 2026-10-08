module jpms.k8s.app {
    // http server
    requires jdk.httpserver;
    // http client
    requires java.net.http;
    // logging
    requires java.xml;
    requires org.slf4j.jdk.platform.logging;
    requires ch.qos.logback.classic;
    // JSON
    requires tools.jackson.databind;
    // records are (de)serialized reflectively
    opens com.sigursoft.jpms.k8s.model to tools.jackson.databind;
}